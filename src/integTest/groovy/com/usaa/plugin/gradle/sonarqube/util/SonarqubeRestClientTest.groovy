package com.usaa.plugin.gradle.sonarqube.util

import groovyx.net.http.HttpResponseException
import spock.lang.Specification
import spock.lang.Subject

class SonarqubeRestClientTest extends Specification {

    @Subject client = new SonarqubeRestClient('http://localhost:9000')

    private SUCCESS_TEST_PROJECT_KEY = 'com.usaa.sonar-quality-gates.test:test-project'
    private ERROR_TEST_PROJECT_KEY = 'com.usaa.sonar-quality-gates.test:error-project'

    def "can get project status -- success"() {
        when:
        def status = client.getScanStatus(SUCCESS_TEST_PROJECT_KEY)

        then:
        status == SonarqubeRestClient.ScanStatus.COMPLETE
    }
    
    def "can validate quality gates -- success"() {
        when:
        def status = client.getQualityGateStatus(SUCCESS_TEST_PROJECT_KEY)

        then:
        status == SonarqubeRestClient.QualityGateStatus.OK
    }

    def "can validate quality gates -- error"() {
        when:
        def status = client.getQualityGateStatus(ERROR_TEST_PROJECT_KEY)

        then:
        status == SonarqubeRestClient.QualityGateStatus.ERROR
    }

    def "can get quality gate id"() {
        when:
        def id = client.getQualityGateId('Sonar way')

        then:
        id == '1'
    }

    def "can get scan status"() {
        when:
        def status = client.getScanStatus(SUCCESS_TEST_PROJECT_KEY)

        then:
        status == SonarqubeRestClient.ScanStatus.COMPLETE
    }
}