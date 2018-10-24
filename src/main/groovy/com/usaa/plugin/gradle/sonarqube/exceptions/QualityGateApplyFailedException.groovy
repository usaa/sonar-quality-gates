package com.usaa.plugin.gradle.sonarqube.exceptions

import org.gradle.api.GradleException

class QualityGateApplyFailedException extends GradleException {
    QualityGateApplyFailedException(String name) {
        super('Unable to apply quality gate ' + name)
    }
}
