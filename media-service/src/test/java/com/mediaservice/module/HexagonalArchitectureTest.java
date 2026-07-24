package com.mediaservice.module;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Katman: MODULE - hexagonal mimari + SOLID sinirlarini MEKANIK olarak dogrular.
 * Sadece test SIRASINDA gozlemlenen bir kural degil; mimari kaydigi anda BUILD'i kirar.
 * <p>
 * Yalnizca ana kod taranir ({@code DO_NOT_INCLUDE_TESTS}) - aksi halde test siniflarindaki
 * idiomatik {@code @Autowired MockMvc} gibi alanlar R5 kuralinda yanlis pozitif uretirdi.
 */
@AnalyzeClasses(packages = "com.mediaservice", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION_USECASE = "..application.usecase..";
    private static final String APPLICATION_PORT = "..application.port..";
    private static final String INFRASTRUCTURE = "..infrastructure..";
    private static final String[] INFRASTRUCTURE_ADAPTERS = {
            "..infrastructure.persistence..",
            "..infrastructure.storage..",
            "..infrastructure.cache..",
            "..infrastructure.converter..",
            "..infrastructure.messaging..",
    };
    private static final String API_CONTROLLER = "..api.controller..";

    /**
     * R1: domain modeli saf POJO'dur - hicbir ust katmana (application/infrastructure/api)
     * bagimli OLAMAZ. Framework'ten tamamen bagimsiz kalmasi gerektigi kod yorumlarinda da
     * acikca belirtilmis (bkz. MediaAsset, ImageBinary).
     */
    @ArchTest
    static final ArchRule domain_ShouldNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", INFRASTRUCTURE, "..api..");

    /**
     * R2: use case'ler infrastructure'a DOGRUDAN bagli olamaz - yalnizca port'lar (arayuzler)
     * uzerinden konusurlar. Saglayici degisimi (MinIO -> S3, Redis -> baska cache) tek bir
     * adapter degisikligi olmali, use case'e DOKUNMAMALI.
     */
    @ArchTest
    static final ArchRule usecases_ShouldOnlyDependOnPortsNotInfrastructure = noClasses()
            .that().resideInAPackage(APPLICATION_USECASE)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);

    /**
     * R3: port.in / port.out yalnizca ARAYUZ olabilir - somut siniflar/implementasyon
     * detaylari port paketine sizamaz (Dependency Inversion Principle).
     */
    @ArchTest
    static final ArchRule ports_ShouldBeInterfaces = classes()
            .that().resideInAPackage(APPLICATION_PORT)
            .should().beInterfaces();

    /**
     * R4: Controller, persistence/storage/cache/converter/messaging adapter'larina DOGRUDAN
     * erisemez - her zaman application.port.in use case'leri uzerinden gecmelidir. Aksi halde
     * HTTP katmani ile depolama detaylari sikica bagli olur ve saglayici degisimi controller'i
     * da etkiler.
     */
    @ArchTest
    static final ArchRule controllers_ShouldNotAccessAdaptersDirectly = noClasses()
            .that().resideInAPackage(API_CONTROLLER)
            .should().dependOnClassesThat().resideInAnyPackage(INFRASTRUCTURE_ADAPTERS);

    /**
     * R5: Constructor injection ZORUNLU (SOLID - bagimliliklar acik ve immutable olmali).
     * Ana kodda hicbir yerde alan seviyesinde {@code @Autowired} kullanilamaz.
     */
    @ArchTest
    static final ArchRule noFieldInjection = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("bagimliliklar constructor injection ile acik ve test edilebilir olmali (SOLID - DIP)");
}
