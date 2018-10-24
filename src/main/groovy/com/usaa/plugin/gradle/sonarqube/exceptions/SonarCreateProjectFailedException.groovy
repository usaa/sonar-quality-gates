package com.usaa.plugin.gradle.sonarqube.exceptions

class SonarCreateProjectFailedException extends Exception {
    SonarCreateProjectFailedException(String key) {
        super('Unable to create SonarQube project with key ' + key)
    }
}
