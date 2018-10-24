package com.usaa.plugin.gradle.sonarqube.exceptions

import org.gradle.api.GradleException

class QualityGateFailedException extends GradleException {
    QualityGateFailedException() {
        this('One or more quality gates has failed for this project.')
    }
    QualityGateFailedException(String msg) {
        super(msg)
    }
}
