package com.usaa.plugin.gradle.sonarqube.exceptions

class SonarExecutionNotFound extends Exception {
    SonarExecutionNotFound(String message) {
        super(message)
    }
}
