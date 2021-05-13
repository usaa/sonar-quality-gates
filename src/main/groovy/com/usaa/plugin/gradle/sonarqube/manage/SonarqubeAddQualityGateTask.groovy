package com.usaa.plugin.gradle.sonarqube.manage

import com.usaa.plugin.gradle.sonarqube.SonarqubeBaseTask
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGateNotFoundException
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGatesNotConfiguredException
import com.usaa.plugin.gradle.sonarqube.util.Helpers
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class SonarqubeAddQualityGateTask extends SonarqubeBaseTask {

    private String gate

    void setGate(String gate) {
        this.gate = gate
    }

    @TaskAction
    void action() {
        Project project = this.getProject();

        if (Helpers.sonarPluginExists(project)) {
            populateRequiredVariables()

            client.projectExists(this.projectKey)?: client.createProject(this.projectKey, this.projectName)

            String gateId = client.getQualityGateId(this.gate)
            if (!gateId) {
                throw new QualityGateNotFoundException(this.gate)
            }
            client.applyQualityGate(this.projectKey, gateId)
            logger.info('Gate {} applied', this.gate)
        } else {
            throw new QualityGatesNotConfiguredException('The sonarqube plugin is not applied to this project')
        }
    }

}
