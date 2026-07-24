package com.mediaservice.support;

import org.testcontainers.DockerClientFactory;

/**
 * Testcontainers gerektiren media-service testleri icin ortak on kosul.
 * <p>
 * Docker olmayan gelistirici makinelerinde bu testler HATA vermek yerine SKIP edilir;
 * CI'da (ubuntu-latest, Docker kurulu) normal sekilde calisirlar.
 * <p>
 * DIKKAT: {@code @EnabledIf} {@code @Inherited} DEGILDIR. Bu kosulu abstract base
 * sinifa koymak somut alt siniflar icin CALISMAZ; her somut test sinifina ayri ayri
 * yazilmalidir (payment servisindeki desenle birebir aynidir).
 */
public final class DockerAvailability {

    private DockerAvailability() {
    }

    private static Boolean cached;

    @SuppressWarnings("unused") // JUnit @EnabledIf tarafindan refleksiyonla cagrilir
    public static boolean isDockerAvailable() {
        if (cached == null) {
            try {
                cached = DockerClientFactory.instance().isDockerAvailable();
            } catch (Throwable t) {
                cached = Boolean.FALSE;
            }
        }
        return cached;
    }
}
