package com.usaa.plugin.gradle.sonarqube.util

import groovyx.net.http.HttpResponseException
import spock.lang.Specification
import spock.lang.Subject

class SonarqubeRestClientTest extends Specification {

    @Subject SonarqubeRestClient client = new SonarqubeRestClient('http://prodsonar1l:9002')

    private final TEST_PROJECT_KEY = 'git:com.usaa.plugin.gradle:sonarqube-quality-gates-plugin'

    def "can get component id"() {
        when:
        def compId = client.getComponentId(TEST_PROJECT_KEY)

        then:
        compId != null
        compId == 'AWU_thINVlFUu60bv18n'
    }

    def "can not find component"() {
        when:
        client.getComponentId('does-not-exist')

        then:
        thrown(HttpResponseException)
    }

    def "can get project status -- success"() {
        when:
        def compId = client.getComponentId(TEST_PROJECT_KEY)
        def status = client.getScanStatus(compId)

        then:
        status == SonarqubeRestClient.ScanStatus.COMPLETE
    }

    def "can get execution date"() {
        when:
        def date = client.getExecutionDate(TEST_PROJECT_KEY, '2.0.0-osf-prepare-SNAPSHOT')

        then:
        date.equals(Date.parse(SonarqubeRestClient.DATE_FORMAT, '2018-08-22T15:02:55-0500'))
    }

    def "can validate quality gates -- none blocking"() {
        when:
        def date = Date.parse(SonarqubeRestClient.DATE_FORMAT, '2018-08-22T15:02:55-0500')
        def status = client.getQualityGateStatus(TEST_PROJECT_KEY, date)

        then:
        status == SonarqubeRestClient.QualityGateStatus.NONE
    }
/*
    def "can validate quality gates -- warning"() {
        when:
        def date = Date.parse(SonarqubeRestClient.DATE_FORMAT, '')
        def status = client.getQualityGateStatus(TEST_PROJECT_KEY, date)

        then:
        status == SonarqubeRestClient.QualityGateStatus.WARN
    }
*/
    def "can get quality gate id"() {
        when:
        def id = client.getQualityGateId('SonarQube way')

        then:
        id == '10000'
    }

    def "can get scan status"() {
        when:
        def compId = client.getComponentId(TEST_PROJECT_KEY)
        def status = client.getScanStatus(compId)

        then:
        status == SonarqubeRestClient.ScanStatus.COMPLETE
    }
}