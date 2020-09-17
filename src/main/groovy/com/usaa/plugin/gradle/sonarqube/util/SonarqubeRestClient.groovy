package com.usaa.plugin.gradle.sonarqube.util

import com.usaa.plugin.gradle.sonarqube.exceptions.QualityGateApplyFailedException
import com.usaa.plugin.gradle.sonarqube.exceptions.SonarCreateProjectFailedException
import groovy.time.TimeCategory

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.Method
import org.apache.http.HttpStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SonarqubeRestClient {

    enum ScanStatus {
        QUEUED,
        IN_PROGRESS,
        COMPLETE
    }

    enum QualityGateStatus {
        OK,
        ERROR,
        NONE
    }

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

    private static final Logger logger = LoggerFactory.getLogger(SonarqubeRestClient.class)
    
    protected static final String AUTH_HEADER_KEY = "Authorization"

    protected final HTTPBuilder http
    protected final String authHeader

    SonarqubeRestClient(String serverUrl) {
        this.http = new HTTPBuilder(serverUrl, ContentType.JSON)
    }

    SonarqubeRestClient(String serverUrl, String username, String password) {
        this(serverUrl)
        this.authHeader = "Basic " + (username + ":" + password).bytes.encodeBase64().toString()
    }

    SonarqubeRestClient(String serverUrl, String apiKey) {
        this(serverUrl, apiKey, "")
    }

    String getComponentId(String projectKey) {
        logger.debug('Getting Component id for projectKey<{}>', projectKey)
        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = '/api/components/show'
            uri.query = [key: projectKey]
            response.success = { resp, json ->
                return json.component.id
            }
            response.'404' = { resp ->
                // Added additional output here because standard output of 'Not Found' isn't very valuable.
                // Other methods don't need because if one is available, the rest will exist.
                logger.error('Unable to find SonarQube component with projectKey <{}>', projectKey)
                throw new HttpResponseException(resp)
            }
        }
    }

    ScanStatus getScanStatus(String projectKey) {
        logger.debug('Getting scan status for component <{}>', projectKey)
        http.get(path: '/api/ce/component', query: [component: projectKey]) { resp, json ->
            if (json.queue.size() > 0) {
                return ScanStatus.QUEUED
            } else if (json.current.status == 'SUCCESS') {
                return ScanStatus.COMPLETE
            } else if (json.current.status == 'IN_PROGRESS') {
                return ScanStatus.IN_PROGRESS
            } else {
                return ScanStatus.FAILED
            }
        }
    }

    QualityGateStatus getQualityGateStatus(String projectKey) {
        logger.debug('Getting quality gate status for projectKey <{}>', projectKey)

        http.get(path: '/api/qualitygates/project_status',
                query: [projectKey: projectKey],
        ) { resp, json ->
            if(json.errors && json.errors.size() > 0) {
                logger.debug("Error checking quality gate: {}", resp.errors.toString())
            }
            return QualityGateStatus.valueOf(json.projectStatus.status)
        }
    }

    boolean projectExists(String projectKey) {
        logger.debug('Search for projectKey <{}>', projectKey)
        http.get(path: '/api/projects/search',
                headers: [
                        (this.AUTH_HEADER_KEY): authHeader
                ],
                query: [
                        projects: projectKey
                ],
        ) { resp, json ->
            if(json.components.size() > 0) {
                return true
            }
            return false
        }
    }

    String getQualityGateId(String qualityGateName) {
        logger.debug('Getting quality gate id for name <{}>', qualityGateName)
        http.get(path: '/api/qualitygates/list') { resp, json ->
            for (gate in json.qualitygates) {
                if (gate.name == qualityGateName) {
                    logger.debug("Gate {} found with id {}", qualityGateName, gate.id)
                    return gate.id
                }
            }
            logger.debug("Gate {} not found", qualityGateName)
            return null;
        }
    }

    boolean applyQualityGate(String projectKey, String gateId) {
        logger.debug('Applying quality gate <{}> to project <{}>', gateId, projectKey)

        http.post(path: '/api/qualitygates/select',
                headers: [
                        (this.AUTH_HEADER_KEY): authHeader
                ],
                contentType: 'application/json',
                body: [
                        "projectKey": projectKey,
                        "gateId": gateId,
                ],
        ) { resp, json ->
            logger.debug('Apply gate response code: {}', resp.status)
            if (resp.status != HttpStatus.SC_NO_CONTENT) {
                throw new QualityGateApplyFailedException("Unable to apply gate: " + gateId + " to project " + projectKey +  \
                     ". error: " + resp.status + "-" + resp.errors[0].msg)
            }
            return true
        }
    }

    boolean applyQualityProfile(String projectKey, String profileName, String language) {
        logger.debug('Applying quality profile <{}> to project <{}> for language <{}>', profileName, projectKey, language)

        http.post(path: '/api/qualityprofiles/add_project',
                headers: [
                        (this.AUTH_HEADER_KEY): authHeader
                ],
                contentType: ContentType.JSON,
                body: [
                        "projectKey": projectKey,
                        "profileName": profileName,
                        "language": language
                ],
        ) { resp, json ->
            logger.debug('Apply profile response code: {}', resp.status)
            if (resp.status != HttpStatus.SC_NO_CONTENT) {
                throw new QualityGateApplyFailedException(sprintf("Unable to apply profile: %s to project %s. error: %s - %s",
                        profileName, projectKey, resp.status, resp.errors[0].msg))
            }
            return true
        }
    }

    @SuppressWarnings('FactoryMethodName')
    String createProject(String projectKey, String projectName) {
        logger.debug('Creating project with key <{}>', projectKey)

        http.post(path: '/api/projects/create',
                headers: [
                        (this.AUTH_HEADER_KEY): authHeader
                ],
                contentType: ContentType.JSON,
                body: [
                        "project": projectKey,
                        "name": projectName
                ],
        ) { resp, json ->
            logger.debug('Create project response code: {}', resp.status)
            if (json && json.project.key) {
                logger.info('Project with key {} created.', projectKey)
                return json.project.key
            }
            throw new SonarCreateProjectFailedException(projectKey)
        }
    }

}
