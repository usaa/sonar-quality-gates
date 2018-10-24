package com.usaa.plugin.gradle.sonarqube.exceptions

import org.gradle.api.GradleException

class QualityGateNotFoundException extends GradleException {
    private static final String ERROR = "Unable to find quality gate named ";

    QualityGateNotFoundException(String name) {
        super(ERROR + name)
    }
}
