/*
 * Copyright 2018-2022 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wooga.gradle.version

import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import org.ajoberstar.grgit.Grgit
import wooga.gradle.version.internal.release.semver.ChangeScope


class VersionPluginMultiProjectIntegrationSpec extends VersionIntegrationSpec {

    def ignoreEverythingGit(File baseDir) {
        new File(projectDir, '.gitignore') << """
        * 
        """.stripIndent() //ensures that repository is always clean

        def git = Grgit.init(dir: baseDir)
        git.add(patterns: ['.gitignore'])
        git.commit(message: 'gitignore file')
        return git;
    }

    def "can generate different versions for plugin applied in multiple subprojects and not in root project"() {
        given:
        def git  = ignoreEverythingGit(projectDir)

        and:
        subprojects.each {
            git.commit(message: "a commit")
            git.tag.add(name: "${it.prefix}${it.nearestNormal}")
        }

        and:
        subprojects.each {it ->
            it.dir = addSubproject(it.name)
            it.buildFile = new File(it.dir, "build.gradle").with { buildFile ->
                createNewFile()
                buildFile << """
                    ${applyPlugin(VersionPlugin)}
                    versionBuilder.prefix = ${wrapValueBasedOnType(it.prefix, String)}
                    versionBuilder.stage.set(${wrapValueBasedOnType(it.stage, String)})
                    versionBuilder.scope = provider{ ${wrapValueBasedOnType(it.scope, "ChangeScope")} }
                    versionBuilder.versionScheme = ${wrapValueBasedOnType(it.versionScheme, String)}
                    versionBuilder.versionCodeScheme = ${wrapValueBasedOnType(it.versionCodeScheme, String)}

                """
                return it
            }
        }

        when:
        subprojects.each {
            it.versionQuery = runPropertyQuery(
                    new PropertyGetterTaskWriter("project(':${it.name}').version", ".orNull",
                            "${it.name}Version")
            )
            it.versionCodeQuery = runPropertyQuery(
                    new PropertyGetterTaskWriter("project(':${it.name}').versionCode", ".orNull",
                            "${it.name}VersionCode")
            )
        }

        then:
        subprojects.forEach {
            assert it.versionQuery.matches(it.newVersion)
            assert it.versionCodeQuery.matches(it.newVersionCode)
        }

        where:
        subprojects = [
            [
                name: "subproject1",
                prefix: "proj1-",
                nearestNormal: "0.0.1",
                stage: "final",
                scope: ChangeScope.MINOR,
                versionScheme: "semver",
                versionCodeScheme: VersionCodeScheme.releaseCountBasic,
                newVersion: "0.1.0",
                newVersionCode: "2",

            ],
            [
                name: "subproject2",
                prefix: "proj2-",
                nearestNormal: "1.0.0",
                stage: "rc",
                scope: ChangeScope.MAJOR,
                versionScheme: "semver2",
                versionCodeScheme: VersionCodeScheme.semver,
                newVersion: "2.0.0-rc.1",
                newVersionCode: "200000",
            ]
        ]
    }

    List<Map<String, ?>> flattenSubprojects(List<Map<String, ?>> subprojects) {
        if(subprojects == null) {
            return null
        }
        return subprojects.collectMany {
            [it, flattenSubprojects(it.subprojects)]
        }.flatten().findAll { it != null } as List<Map<String, ?>>
    }

    def "version/versionCode in parent project project propagates to child projects that doesn't apply version plugin"() {
        given:
        def git = ignoreEverythingGit(projectDir)
        List<Map<String, ?>> allSubprojects = flattenSubprojects(subprojects)
        List<Map<String, ?>> allProjects = [rootProject, allSubprojects].flatten()

        and:
        allProjects.findAll{it.nearestNormal}.each {
            git.commit(message: "a commit")
            git.tag.add(name: it.prefix.toString() + it.nearestNormal.toString())
        }
        and:
        rootProject.buildFile = buildFile
        addSubprojects(projectDir, subprojects)


        and:
        allProjects.findAll {it.appliesPlugin }.each {
            fillExtension(it, it.buildFile)
        }
        when:
        rootProject.versionQuery = queryProperty("rootProject.version")
        rootProject.versionCodeQuery = queryProperty("rootProject.versionCode")
        allSubprojects.each {
            it.versionQuery = queryProperty(
                    "project(':${it.name}').version", ".orNull", "${it.name.replaceAll(":", "_")}Version"
            )
            it.versionCodeQuery = queryProperty(
                    "project(':${it.name}').versionCode", ".orNull", "${it.name.replaceAll(":", "_")}VersionCode"
            )
        }

        then:
        allProjects.forEach {
            assert it.versionQuery.matches(it.newVersion)
            assert it.versionCodeQuery.matches(it.newVersionCode)
        }

        where:
        rootProject = [
            name: "proj",
            appliesPlugin: true,
            prefix: "v_",
            nearestNormal: "1.0.0",
            stage: "rc",
            scope: ChangeScope.MAJOR,
            versionScheme: "semver2",
            versionCodeScheme: VersionCodeScheme.semver,
            newVersion: "2.0.0-rc.1",
            newVersionCode: "200000",
        ]
        subprojects = [
        [
            name: "subproj1",
            appliesPlugin: false,
            newVersion: rootProject.newVersion,
            newVersionCode: rootProject.newVersionCode,
            subprojects: [[
                name: "subproj1:sub1",
                appliesPlugin: false,
                newVersion: rootProject.newVersion,
                newVersionCode: rootProject.newVersionCode,
            ]]
        ],
        [
            name: "subproj2",
            appliesPlugin: true,
            prefix: "subproj2_",
            nearestNormal: "0.1.0",
            stage: "final",
            scope: ChangeScope.PATCH,
            versionScheme: "semver2",
            versionCodeScheme: VersionCodeScheme.releaseCount,
            newVersion: "0.1.1",
            newVersionCode: "2",
            subprojects: [[
                name: "subproj2:sub1",
                appliesPlugin: false,
                newVersion: "0.1.1", //parent.newVersion
                newVersionCode: "2", //parent.newVersionCode
            ], [
                name: "subproj2:sub2",
                appliesPlugin: true,
                prefix: "sub2sub2_",
                nearestNormal: "0.0.1",
                stage: "final",
                scope: ChangeScope.MAJOR,
                versionScheme: "semver2",
                versionCodeScheme: VersionCodeScheme.semver,
                newVersion: "1.0.0",
                newVersionCode: "100000",
            ]]
        ]]

    }

    def queryProperty(String path, String invocation = ".getOrNull()", String taskName = null) {
        runPropertyQuery(new PropertyGetterTaskWriter(path, invocation, taskName))
    }

    def addSubprojects(File parentProjectDir, List<Map<String, ?>> subprojects) {
        subprojects.each {
            def folderName = it.name.toString().tokenize(':').last()
            it.projectDir = addSubproject(parentProjectDir, folderName, it.name)
            it.buildFile = new File(it.projectDir as File, "build.gradle").with {
                it.createNewFile()
                return it
            }
            if(it.subprojects) {
                addSubprojects(it.projectDir as File, it.subprojects as List<Map<String, ?>>)
            }
        }
    }

    File addSubproject(File projectDir, String folderName, String fullName = folderName) {
        settingsFile << "include '${fullName}'\n"
        def dir = new File(projectDir, folderName)
        dir.mkdirs()
        return dir
    }

    def fillExtension(Map<String, ?> it, File buildFile) {
        buildFile << """
        ${applyPlugin(VersionPlugin)}
        ${fieldSetter("versionBuilder.prefix", it.prefix)}
        ${fieldSetter("versionBuilder.stage", it.stage, String, false)}
        ${fieldSetter("versionBuilder.scope", it.scope, "Provider<ChangeScope>")}
        ${fieldSetter("versionBuilder.versionScheme", it.versionScheme)}
        ${fieldSetter("versionBuilder.versionCodeScheme", it.versionCodeScheme)}
        """
    }

    def fieldSetter(String field, Object value, Object type = value.class, boolean assign = true) {
        if(value != null) {
            def typeStr = type instanceof Class? ((Class)type).simpleName : type.toString();
            if(assign) {
                return "${field} = ${wrapValueBasedOnType(value, typeStr)}"
            }
            return "${field}.set(${wrapValueBasedOnType(value, typeStr)})"
        }
    }

}
