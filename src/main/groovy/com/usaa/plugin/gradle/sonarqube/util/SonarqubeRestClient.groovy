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
        WARN,
        ERROR,
        NONE
    }

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

    private static final Logger logger = LoggerFactory.getLogger(SonarqubeRestClient.class)
    private static final String AUTH_HEADER_KEY = "Authorization"

    private final HTTPBuilder http
    private final String authHeader

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

    Date getExecutionDate(String projectKey, String version) {
        logger.debug('Getting execution date for projectKey<{}> and version<{}>', projectKey, version)
        http.request(Method.GET, ContentType.JSON) { req ->
            uri.path = '/api/events'
            uri.query = [resource: projectKey, categories: 'Version']
            response.success = { resp, json ->
                for (obj in json) {
                    if (version == obj.n) {
                        return Date.parse(DATE_FORMAT, obj.dt)
                    }
                }
                return null
            }
            response.'404' = { resp ->
                // Added additional output here because standard output of 'Not Found' isn't very valuable.
                // Other methods don't need because if one is available, the rest will exist.
                logger.error('Unable to find execution date with projectKey <{}> and version <{}>', projectKey, version)
                throw new HttpResponseException(resp as Object)
            }
        }
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

    ScanStatus getScanStatus(String componentId) {
        logger.debug('Getting scan status for componentId <{}>', componentId)
        http.get(path: '/api/ce/component', query: [componentId: componentId]) { resp, json ->
            if (json.queue.size() > 0) {
                return ScanStatus.QUEUED
            } else if (json.current.status == 'SUCCESS') {
                return ScanStatus.COMPLETE
            } else {
                return ScanStatus.IN_PROGRESS
            }
        }
    }

    QualityGateStatus getQualityGateStatus(String projectKey, Date executionDate) {
        logger.debug('Getting quality gate status for projectKey <{}> and executionDate <{}>', projectKey, executionDate.format(DATE_FORMAT))

        Date endDate = null
        use(TimeCategory) {
            endDate = executionDate + 1.second
            endDate
        }

        http.get(path: '/api/timemachine',
                query: [
                        resource    : projectKey,
                        metrics     : 'alert_status',
                        fromDateTime: executionDate.format(DATE_FORMAT),
                        toDateTime  : endDate.format(DATE_FORMAT)
                ],
        ) { resp, json ->
            for (obj in json) {
                if (obj.cells.size() == 0) {
                    return QualityGateStatus.NONE
                }
                return QualityGateStatus.valueOf(obj.cells[0].v[0])
            }
        }
    }

    String getProjectId(String projectKey) {
        logger.debug('Getting projectId for projectKey <{}>', projectKey)
        http.get(path: '/api/projects/index',
                query: [
                        key: projectKey
                ],
        ) { resp, json ->
            for (proj in json) {
                return proj.id
            }
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

    boolean applyQualityGate(String projectId, String gateId) {
        logger.debug('Applying quality gate <{}> to project <{}>', gateId, projectId)

        http.post(path: '/api/qualitygates/select',
                headers: [
                        (this.AUTH_HEADER_KEY): authHeader
                ],
                contentType: 'application/json',
                body: [
                        "projectId": projectId,
                        "gateId": gateId,
                ],
        ) { resp, json ->
            logger.debug('Apply gate response code: {}', resp.status)
            if (resp.status != HttpStatus.SC_NO_CONTENT) {
                throw new QualityGateApplyFailedException("Unable to apply gate: " + gateId + " to project " + projectId +  \
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
                        "key": projectKey,
                        "name": projectName
                ],
        ) { resp, json ->
            logger.debug('Create project response code: {}', resp.status)
            if (json && json.id) {
                return json.id
            }
            throw new SonarCreateProjectFailedException(projectKey)
        }
    }

}
