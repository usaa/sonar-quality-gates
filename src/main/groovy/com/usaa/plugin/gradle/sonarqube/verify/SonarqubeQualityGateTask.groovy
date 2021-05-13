package com.usaa.plugin.gradle.sonarqube.verify

import java.net.URLEncoder

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

import com.usaa.plugin.gradle.sonarqube.SonarqubeBaseTask
import com.usaa.plugin.gradle.sonarqube.util.Helpers
import com.usaa.plugin.gradle.sonarqube.util.SonarqubeRestClient.ScanStatus
import com.usaa.plugin.gradle.sonarqube.util.SonarqubeRestClient.QualityGateStatus
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGateFailedException
import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGatesNotConfiguredException

class SonarqubeQualityGateTask extends SonarqubeBaseTask {

    private static final int MILLIS_TO_SECS = 1000

    private int sleepAmount
    private int maxWait

    void setSleepAmount(int millis) {
        this.sleepAmount = millis
    }

    void setMaxWait(int millis) {
        this.maxWait = millis
    }

    @TaskAction
    void action() {
        Project project = this.getProject();
        logger.debug('sleep: {}', this.sleepAmount)
        logger.debug('maxWait: {}', this.maxWait)

        if(project.version.toLowerCase().startsWith('unspecified')) {
            logger.error('Specified version number <{}> is not valid.', project.version)
            throw new InvalidVersionSpecificationException(sprintf('<%s> is not a valid version number. Please specify a version number in your gradle.properties file.', project.version))
        }

        if (Helpers.sonarPluginExists(project)) {
            populateRequiredVariables()

            int currentWaitTime = 0;
            def status = client.getScanStatus(this.projectKey)
            while (status == ScanStatus.IN_PROGRESS || status == ScanStatus.QUEUED) {
                logger.warn('Scan still in progress. Retrying in {} second(s)', (this.sleepAmount / MILLIS_TO_SECS))
                sleep(this.sleepAmount)
                currentWaitTime += this.sleepAmount
                if(currentWaitTime > this.maxWait) {
                    throw new QualityGateFailedException(sprintf('Max wait time of %d seconds was exceeded.', (this.maxWait / MILLIS_TO_SECS)))
                }
                status = client.getScanStatus(this.projectKey)
            }

            QualityGateStatus gateStatus = client.getQualityGateStatus(this.projectKey)
            if (QualityGateStatus.ERROR == gateStatus) {
                def encodedKey = URLEncoder.encode(this.projectKey, "UTF-8")
                logger.error('Quality gate failed. Please see {}/dashboard?id={}', this.serverUrl, encodedKey)
                throw new QualityGateFailedException(sprintf('Quality gate failed. Please see %s/dashboard?id=%s', this.serverUrl, encodedKey))
            } else if (QualityGateStatus.NONE == gateStatus) {
                def encodedKey = URLEncoder.encode(this.projectKey, "UTF-8")
                logger.warn('No quality gates have been defined for this project. See {}/dashboard?id={}', this.serverUrl, encodedKey)
                throw new QualityGatesNotConfiguredException()
            }
        } else {
            throw new QualityGatesNotConfiguredException('The sonarqube plugin is not applied to this project')
        }
    }
}
