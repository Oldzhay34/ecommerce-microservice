package com.notificationservice.support;

import org.testcontainers.DockerClientFactory;

/**
 * Testcontainers gerektiren testler için ortak koşul.
 * <p>
 * Docker olmayan geliştirici makinelerinde testler HATA vermek yerine SKIP edilir;
 * CI (ubuntu-latest, Docker kurulu) üzerinde normal şekilde çalışırlar. Böylece
 * "yerelde kırmızı build" ile "CI'da gerçekten koşan test" arasında seçim yapmak
 * zorunda kalmıyoruz.
 */
public final class DockerAvailability {

    private DockerAvailability() {}

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
