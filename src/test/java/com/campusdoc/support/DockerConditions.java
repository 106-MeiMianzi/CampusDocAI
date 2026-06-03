package com.campusdoc.support;

import org.testcontainers.DockerClientFactory;

public final class DockerConditions {

    private DockerConditions() {
    }

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception ex) {
            return false;
        }
    }
}
