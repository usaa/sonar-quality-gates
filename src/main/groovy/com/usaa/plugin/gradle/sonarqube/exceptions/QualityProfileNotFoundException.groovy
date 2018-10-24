package com.usaa.plugin.gradle.sonarqube.exceptions

import org.gradle.api.GradleException

class QualityProfileNotFoundException extends GradleException {
    private static final String ERROR = "Unable to find quality profile named ";

    QualityProfileNotFoundException(String name) {
        super(ERROR + name)
    }
}
