package com.usaa.plugin.gradle.sonarqube.manage

import com.usaa.plugin.gradle.sonarqube.SonarqubeBaseTask
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGatesNotConfiguredException
import com.usaa.plugin.gradle.sonarqube.util.Helpers
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class SonarqubeAddQualityProfileTask extends SonarqubeBaseTask {

    private String profile
    private String language

    void setProfile(String profile) {
        this.profile = profile
    }

    void setLanguage(String language) {
        this.language = language;
    }
    
    @TaskAction
    void action() {
        Project project = this.getProject();

        if (Helpers.sonarPluginExists(project)) {
            populateRequiredVariables()

            if (!client.getProjectId(this.projectKey)) {
                client.createProject(this.projectKey, this.projectName)
            }

            if (client.applyQualityProfile(this.projectKey, this.profile, this.language)) {
                logger.info('Profile {} applied', this.profile)
            }
        } else {
            throw new QualityGatesNotConfiguredException('The sonarqube plugin is not applied to this project')
        }
    }
}
