package com.usaa.plugin.gradle.sonarqube

import com.usaa.plugin.gradle.sonarqube.manage.SonarqubeAddQualityGateTask
import com.usaa.plugin.gradle.sonarqube.manage.SonarqubeAddQualityProfileTask
import com.usaa.plugin.gradle.sonarqube.verify.SonarqubeQualityGateTask
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class SonarqubeQualityGatePlugin implements Plugin<Project> {

    private static final Logger logger = LoggerFactory.getLogger(SonarqubeQualityGatePlugin.class)

    private static final String PLUGIN_GROUP = "SonarQube Utility"

    @Override
    void apply(Project project) {
        SonarqubeQualityGateExtension extension = project.getExtensions().create("sonarqubeQualityGate", SonarqubeQualityGateExtension.class);

        project.afterEvaluate { // do this so that the extension is properly set prior to defining the tasks
            Task verifyTask = project.getTasks().create("verifySonarqubeQualityGates", SonarqubeQualityGateTask.class, new Action<SonarqubeQualityGateTask>() {
                void execute(SonarqubeQualityGateTask task) {
                    logger.info('Setting sleep to {}', extension.getSleep())
                    task.setFailOnWarn(extension.getFailOnWarn())
                    task.setSleepAmount(extension.getSleep().toInteger())
                    task.setMaxWait(extension.getMaxWait().toInteger())
                }
            })
            verifyTask.setDescription('Verifies no quality gate errors exist for project')
            verifyTask.setGroup(PLUGIN_GROUP)

            Task gateTask = project.getTasks().create("applyQualityGate", SonarqubeAddQualityGateTask.class, new Action<SonarqubeAddQualityGateTask>() {
                void execute(SonarqubeAddQualityGateTask task) {
                    task.setGate(extension.getGate())
                    applyCredentials(task, extension)
                }
            })
            gateTask.setDescription('Applies SonarQube quality gate to project')
            gateTask.setGroup(PLUGIN_GROUP)

            Task profileTask = project.getTasks().create("applyQualityProfile", SonarqubeAddQualityProfileTask.class, new Action<SonarqubeAddQualityProfileTask>() {
                void execute(SonarqubeAddQualityProfileTask task) {
                    task.setProfile(extension.getProfile())
                    task.setLanguage(extension.getLanguage())
                    applyCredentials(task, extension)
                }
            })
            profileTask.setDescription('Applies SonarQube quality profile to project')
            profileTask.setGroup(PLUGIN_GROUP)
        }
    }
    private void applyCredentials(DefaultTask task, SonarqubeQualityGateExtension extension) {
        String username = findNotNullValue(extension.getUsername(), System.getProperty("sonar.quality.username"))
        String password = findNotNullValue(extension.getPassword(), System.getProperty("sonar.quality.password"))
        if (username?.trim() && password?.trim()) {
            task.setUsername(username)
            task.setPassword(password)
        } else {
            task.setApiKey(findNotNullValue(extension.getApiKey(), System.getProperty("sonar.quality.login"), System.getProperty("sonar.login")))
        }
    }
    private String findNotNullValue(String ...values) {
        for (String s : values) {
            if (s) {
                return s
            }
        }
        return null
    }
}
