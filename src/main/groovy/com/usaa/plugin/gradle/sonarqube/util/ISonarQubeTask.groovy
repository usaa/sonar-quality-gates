package com.usaa.plugin.gradle.sonarqube.util

/*
 * This interface was created to mimic the SonarQubeTask class as defined within the SonarQube Gradle Plugin
 * (https://github.com/SonarSource/sonar-scanner-gradle). It eliminates the need to include the plugin just to
 * strongly type the object.
 */
interface ISonarQubeTask {
    public Map<String, Object> properties;
}