/*
 * Copyright 2018-2020 Wooga GmbH
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
 *
 *
 */

package wooga.gradle.version

import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.internal.release.base.TagStrategy
import wooga.gradle.version.internal.release.semver.ChangeScope
import org.ajoberstar.grgit.Grgit
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

interface VersionPluginExtension {

    Property<VersionScheme> getVersionScheme()
    void versionScheme(VersionScheme value)
    void versionScheme(Provider<VersionScheme> value)
    void versionScheme(String value)
    void setVersionScheme(VersionScheme value)
    void setVersionScheme(Provider<VersionScheme> value)
    void setVersionScheme(String value)

    Property<VersionCodeScheme> getVersionCodeScheme()
    void versionCodeScheme(VersionCodeScheme value)
    void versionCodeScheme(Provider<VersionCodeScheme> value)
    void versionCodeScheme(String value)
    void setVersionCodeScheme(VersionCodeScheme value)
    void setVersionCodeScheme(Provider<VersionCodeScheme> value)
    void setVersionCodeScheme(String value)

    Property<Integer> getVersionCodeOffset()
    void versionCodeOffset(Integer value)
    void versionCodeOffset(Provider<Integer> value)
    void setVersionCodeOffset(Integer value)
    void setVersionCodeOffset(Provider<Integer> value)

    Property<String> getReleaseBranchPattern()
    void releaseBranchPattern(String value)
    void releaseBranchPattern(Provider<String> value)
    void setReleaseBranchPattern(String value)
    void setReleaseBranchPattern(Provider<String> value)

    Property<String> getMainBranchPattern()
    void mainBranchPattern(String value)
    void mainBranchPattern(Provider<String> value)
    void setMainBranchPattern(String value)
    void setMainBranchPattern(Provider<String> value)

    Property<Grgit> getGit()

    Provider<ReleaseVersion> getVersion()
    Provider<Integer> getVersionCode()

    Provider<String> getStage()
    Provider<ChangeScope> getScope()

    TagStrategy getTagStrategy()
}
