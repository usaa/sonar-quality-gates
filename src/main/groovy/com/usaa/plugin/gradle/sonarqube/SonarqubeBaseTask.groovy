package com.usaa.plugin.gradle.sonarqube

import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGateFailedException
import com.usaa.plugin.gradle.sonarqube.util.Helpers
import com.usaa.plugin.gradle.sonarqube.util.ISonarQubeTask
import com.usaa.plugin.gradle.sonarqube.util.SonarqubeRestClient
import org.gradle.api.DefaultTask
import org.gradle.api.Project

class SonarqubeBaseTask extends DefaultTask {

    protected String serverUrl
    protected String projectKey
    protected String projectName
    protected String username
    protected String password
    protected String apiKey

    protected SonarqubeRestClient client

    SonarqubeBaseTask() {
        super()
        populateRequiredVariables()
    }

    void setUsername(String username) {
        this.username = username
    }

    void setPassword(String password) {
        this.password = password
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey
    }

    SonarqubeRestClient getClient() {
        if (!client) {
            if (apiKey) {
                client = new SonarqubeRestClient(serverUrl, apiKey)
            } else if (username && password) {
                client = new SonarqubeRestClient(serverUrl, username, password)
            } else {
                client = new SonarqubeRestClient(serverUrl)
            }
        }
        return client
    }

    protected void populateRequiredVariables() {
        Project project = this.getProject()
        ISonarQubeTask task = Helpers.getSonarQubeTask(project)
        this.serverUrl = Helpers.getServerUrl(task)
        logger.debug('Sonarqube host url set to {}', this.serverUrl)
        this.projectKey = Helpers.getProjectKey(task)
        logger.debug('Sonarqube projectKey set to {}', this.projectKey)
        if(!this.projectKey) {
            throw new QualityGateFailedException('Unable to find sonar.projectKey. Please make sure configuration is correct.')
        }
        String branch = Helpers.getBranch(task)
        if(branch) {
            this.projectKey += ':' + branch
        }
        this.projectName = Helpers.getProjectName(task)
        logger.debug('Sonarqube projectName set to {}', this.projectName)
    }
}