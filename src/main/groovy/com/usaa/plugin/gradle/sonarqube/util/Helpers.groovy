package com.usaa.plugin.gradle.sonarqube.util

import org.gradle.api.Project

class Helpers {
    static ISonarQubeTask getSonarQubeTask(Project project) {
        Iterator it = project.getTasksByName('sonarqube', true).iterator()
        while (it.hasNext()) {
            return it.next() as ISonarQubeTask
        }
        return null
    }

    static String getServerUrl(ISonarQubeTask sqt) {
        return sqt.properties.get('sonar.host.url')
    }

    static String getProjectKey(ISonarQubeTask sqt) {
        return sqt.properties.get('sonar.projectKey')
    }

    static String getBranch(ISonarQubeTask sqt) {
        return sqt.properties.get('sonar.branch')
    }

    static String getProjectName(ISonarQubeTask sqt) {
        return sqt.properties.get('sonar.projectName')
    }

    static boolean sonarPluginExists(Project project) {
        return project.plugins.hasPlugin("org.sonarqube")
    }
}
