package com.usaa.plugin.gradle.sonarqube.verify

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import com.usaa.plugin.gradle.sonarqube.SonarqubeBaseTask
import com.usaa.plugin.gradle.sonarqube.util.Helpers
import com.usaa.plugin.gradle.sonarqube.util.SonarqubeRestClient.ScanStatus
import com.usaa.plugin.gradle.sonarqube.util.SonarqubeRestClient.QualityGateStatus
import com.usaa.plugin.gradle.sonarqube.exceptions.SonarExecutionNotFound
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGateFailedException
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGatesNotConfiguredException

class SonarqubeQualityGateTask extends SonarqubeBaseTask {

    private static final int MILLIS_TO_SECS = 1000

    private boolean failOnWarn
    private int sleepAmount
    private int maxWait

    void setFailOnWarn(boolean failOnWarn) {
        this.failOnWarn = failOnWarn
    }

    void setSleepAmount(int millis) {
        this.sleepAmount = millis
    }

    void setMaxWait(int millis) {
        this.maxWait = millis
    }

    @TaskAction
    void action() {
        Project project = this.getProject();
        logger.debug('failOnWarn: {}', this.failOnWarn)
        logger.debug('sleep: {}', this.sleepAmount)
        logger.debug('maxWait: {}', this.maxWait)

        if(project.version.toLowerCase().startsWith('unspecified')) {
            logger.error('Specified version number <{}> is not valid.', project.version)
            throw new InvalidVersionSpecificationException(sprintf('<%s> is not a valid version number. Please specify a version number in your gradle.properties file.', project.version))
        }

        if (Helpers.sonarPluginExists(project)) {
            populateRequiredVariables()

            String componentId = client.getComponentId(this.projectKey)
            int currentWaitTime = 0;
            while (ScanStatus.IN_PROGRESS == client.getScanStatus(componentId)) {
                logger.warn('Scan still in progress. Retrying in {} second(s)', (this.sleepAmount / MILLIS_TO_SECS))
                sleep(this, this.sleepAmount)
                currentWaitTime += this.sleepAmount
                if(currentWaitTime > this.maxWait) {
                    throw new QualityGateFailedException(sprintf('Max wait time of %d seconds was exceeded.', (this.maxWait / MILLIS_TO_SECS)))
                }
            }

            Date executionDate = client.getExecutionDate(this.projectKey, project.version as String)
            if(executionDate == null) {
                throw new SonarExecutionNotFound(sprintf('Unable to find sonarqube execution for this project/version <%s/%s>.', this.projectKey, project.version))
            }

            QualityGateStatus gateStatus = client.getQualityGateStatus(this.projectKey, executionDate)
            if (QualityGateStatus.ERROR == gateStatus) {
                logger.error('Quality gate failed. Please see {}?id={}', this.serverUrl, this.projectKey)
                throw new QualityGateFailedException(sprintf('Quality gate failed. Please see %s?id=%s', this.serverUrl, this.projectKey))
            } else if (QualityGateStatus.NONE == gateStatus) {
                logger.warn('No quality gates have been defined for this project. See {}?id={}', this.serverUrl, this.projectKey)
                throw new QualityGatesNotConfiguredException()
            } else if (QualityGateStatus.WARN == gateStatus) {
                logger.warn('One or more quality gates are in a \'WARN\' state. See {}?id={}', this.serverUrl, this.projectKey)
                if(this.failOnWarn.asBoolean() == true) {
                    throw new QualityGateFailedException(sprintf('Quality gate failed with WARNING. Please see %s?id=%s', this.serverUrl, this.projectKey))
                }
            }
        } else {
            throw new QualityGatesNotConfiguredException('The sonarqube plugin is not applied to this project')
        }
    }
}
