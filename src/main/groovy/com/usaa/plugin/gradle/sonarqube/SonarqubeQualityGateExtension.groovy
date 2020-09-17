package com.usaa.plugin.gradle.sonarqube

class SonarqubeQualityGateExtension {
    private String maxWait = "1800000"
    private String sleep = "1000"
    private String gate = "SonarQube way"
    private String profile = "Sonar way"
    private String language = "java"
    private String username
    private String password
    private String apiKey

    String getMaxWait() {
        return maxWait
    }

    void setMaxWait(String maxWait) {
        this.maxWait = maxWait
    }

    String getSleep() {
        return sleep
    }

    void setSleep(String sleep) {
        this.sleep = sleep
    }

    String getGate() {
        return gate
    }

    void setGate(String gate) {
        this.gate = gate;
    }

    String getProfile() {
        return profile
    }

    void setProfile(String profile) {
        this.profile = profile;
    }

    String getLanguage() {
        return language
    }

    void setLanguage(String language) {
        this.language = language;
    }

    String getUsername() {
        return username
    }

    void setUsername(String username) {
        this.username = username
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    String getApiKey() {
        return apiKey
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey
    }
}
