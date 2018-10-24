package com.usaa.plugin.gradle.sonarqube.exceptions

import org.gradle.api.GradleException

class QualityGatesNotConfiguredException extends GradleException {
    QualityGatesNotConfiguredException() {
        this('No quality gates have been configured for this project.')
    }
    QualityGatesNotConfiguredException(String msg) {
        super(msg)
    }
}
