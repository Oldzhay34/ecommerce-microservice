package com.payment.support;

import org.testcontainers.DockerClientFactory;

/**
 * Testcontainers gerektiren payment testleri için ortak ön koşul.
 * <p>
 * Docker olmayan geliştirici makinelerinde bu testler HATA vermek yerine SKIP edilir;
 * CI'da (ubuntu-latest, Docker kurulu) normal şekilde çalışırlar.
 * <p>
 * DİKKAT: {@code @EnabledIf} {@code @Inherited} DEĞİLDİR. Bu koşulu abstract base
 * sınıfa koymak somut alt sınıflar için ÇALIŞMAZ; her somut test sınıfına ayrı ayrı
 * yazılmalıdır (bu servis genelinde böyle yapılmıştır).
 */
public final class DockerAvailability {

    private DockerAvailability() {
    }

    private static Boolean cached;

    @SuppressWarnings("unused") // JUnit @EnabledIf tarafından refleksiyonla çağrılır
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
