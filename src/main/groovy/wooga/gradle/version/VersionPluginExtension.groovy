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

import com.wooga.gradle.BaseSpec
import org.ajoberstar.grgit.Grgit
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import wooga.gradle.version.internal.GitStrategyPicker
import wooga.gradle.version.internal.release.base.DefaultVersionStrategy
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.internal.release.base.TagStrategy
import wooga.gradle.version.internal.release.base.VersionStrategy
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.VersionInferenceParameters

trait VersionPluginExtension implements BaseSpec {

    /**
     * @return The version scheme being used by the plugin (such as SemVer2)
     */
    Property<IVersionScheme> getVersionScheme() {
        versionScheme
    }

    private final Property<IVersionScheme> versionScheme = objects.property(IVersionScheme)

    void versionScheme(IVersionScheme value) {
        setVersionScheme(value)
    }

    void versionScheme(Provider<IVersionScheme> value) {
        setVersionScheme(value)
    }

    void setVersionScheme(IVersionScheme value) {
        versionScheme.set(value)
    }

    void setVersionScheme(Provider<IVersionScheme> value) {
        versionScheme.set(value)
    }

    void versionScheme(String value) {
        setVersionScheme(value)
    }

    void setVersionScheme(String value) {
        versionScheme.set(VersionScheme.valueOf(value.trim()))
    }

    /**
     * @return The version code scheme being used by the plugin (such as SemVer2, releaseCount)
     */
    Property<VersionCodeScheme> getVersionCodeScheme() {
        versionCodeScheme
    }

    private final Property<VersionCodeScheme> versionCodeScheme = objects.property(VersionCodeScheme)

    void versionCodeScheme(VersionCodeScheme value) {
        setVersionCodeScheme(value)
    }

    void versionCodeScheme(Provider<VersionCodeScheme> value) {
        setVersionCodeScheme(value)
    }

    void setVersionCodeScheme(VersionCodeScheme value) {
        versionCodeScheme.set(value)
    }

    void setVersionCodeScheme(Provider<VersionCodeScheme> value) {
        versionCodeScheme.set(value)
    }

    void versionCodeScheme(String value) {
        setVersionCodeScheme(value)
    }

    void setVersionCodeScheme(String value) {
        versionCodeScheme.set(VersionCodeScheme.valueOf(value.trim()))
    }

    /**
     * @return If set, used during the evaluation of {@code versionCode}
     */
    Property<Integer> getVersionCodeOffset() {
        versionCodeOffset
    }

    private final Property<Integer> versionCodeOffset = objects.property(Integer)

    void versionCodeOffset(Integer value) {
        setVersionCodeOffset(value)
    }

    void versionCodeOffset(Provider<Integer> value) {
        setVersionCodeOffset(value)
    }

    void setVersionCodeOffset(Integer value) {
        versionCodeOffset.set(value)
    }

    void setVersionCodeOffset(Provider<Integer> value) {
        versionCodeOffset.set(value)
    }

    /**
     * @return If set, ....
     */
    Property<String> getReleaseBranchPattern() {
        releaseBranchPattern
    }

    private final Property<String> releaseBranchPattern = objects.property(String)

    void releaseBranchPattern(String value) {
        setReleaseBranchPattern(value)
    }

    void releaseBranchPattern(Provider<String> value) {
        setReleaseBranchPattern(value)
    }

    void setReleaseBranchPattern(String value) {
        releaseBranchPattern.set(value)
    }

    void setReleaseBranchPattern(Provider<String> value) {
        releaseBranchPattern.set(value)
    }

    /**
     * @return If set, ...
     */
    Property<String> getMainBranchPattern() {
        mainBranchPattern
    }

    private final Property<String> mainBranchPattern = objects.property(String)

    void mainBranchPattern(String value) {
        setMainBranchPattern(value)
    }

    void mainBranchPattern(Provider<String> value) {
        setMainBranchPattern(value)
    }

    void setMainBranchPattern(String value) {
        mainBranchPattern.set(value)
    }

    void setMainBranchPattern(Provider<String> value) {
        mainBranchPattern.set(value)
    }

    /**
     * @return The instance of the git client to be used
     */
    Property<Grgit> getGit() {
        git
    }

    private final Property<Grgit> git = objects.property(Grgit)

    /**
     * @return The evaluated version of the project
     */
    Provider<ReleaseVersion> getVersion() {
        version
    }

    private Provider<ReleaseVersion> version

    void setVersion(Provider<ReleaseVersion> value) {
        version = value
    }

    void setVersion(String value) {
        version = providers.provider({ new ReleaseVersion(version: value) })
    }

    /**
     * @return The evaluated version code of the project
     */
    Provider<Integer> getVersionCode() {
        versionCode
    }

    Provider<Integer> versionCode = objects.property(Integer)

    void setVersionCode(Provider<Integer> value) {
        versionCode = value
    }

    /**
     * @return The stage to be used for evaluating the project version (such as rc, snapshot, final)
     */
    Provider<String> getStage() {
        stage
    }

    Provider<String> stage = objects.property(String)

    void setStage(Provider<String> value) {
        stage.set(value)
    }

    void setStage(String value) {
        stage.set(value)
    }

    /**
     * @return The scope to be used for evaluating the project version (such as major, minor, patch)
     */
    Provider<ChangeScope> getScope() {
        scope
    }

    Provider<ChangeScope> scope = objects.property(ChangeScope)

    void setScope(Provider<ChangeScope> value) {
        scope = value
    }

    /**
     * @return Used during nearest version inference
     */
    TagStrategy getTagStrategy() {
        tagStrategy
    }

    TagStrategy tagStrategy = new TagStrategy()

    /**
     * @return Whether this is a development build
     */
    Provider<Boolean> getIsDevelopment() {
        isDevelopment
    }

    void setIsDevelopment(Provider<Boolean> value) {
        isDevelopment = value
    }

    Provider<Boolean> isDevelopment

    /**
     * @return Whether this is a production build
     */
    Provider<Boolean> getIsFinal() {
        isFinal
    }

    Provider<Boolean> isFinal

    void setIsFinal(Provider<Boolean> value) {
        isFinal = value
    }

    /**
     * @return Whether this is a pre-release build
     */
    Provider<Boolean> getIsPrerelease() {
        isPrerelease
    }

    Provider<Boolean> isPrerelease

    void setIsPrerelease(Provider<Boolean> value) {
        isPrerelease = value
    }

    /**
     * @return Whether this is a snapshot build (such as from a CI environment)
     */
    Provider<Boolean> getIsSnapshot() {
        isSnapshot
    }

    Provider<Boolean> isSnapshot

    void setIsSnapshot(Provider<Boolean> value) {
        isSnapshot = value
    }

    /**
     * @return The deduced release stage for this build
     */
    Provider<ReleaseStage> getReleaseStage() {
        releaseStage

    }

    final Provider<ReleaseStage> releaseStage = providers.provider { ->
        if (isDevelopment.getOrElse(false)) {
            return ReleaseStage.Development
        } else if (isSnapshot.getOrElse(false)) {
            return ReleaseStage.Snapshot
        } else if (isPrerelease.getOrElse(false)) {
            return ReleaseStage.Prerelease
        } else if (isFinal.getOrElse(false)) {
            return ReleaseStage.Final
        } else {
            return versionScheme.flatMap{scheme ->
                stage.map {stageName -> scheme.findStageForStageName(stageName) }
            }.orNull
        }
    }.orElse(ReleaseStage.Unknown)

    /**
     * Infers the next version for this project based on extension information and underlying git repository tags.
     * @param scheme - version scheme that this project is following
     * @param stageProvider - provider with the stage desired for the next version
     * @param scopeProvider - provider with the ChangeScope desired for the next version
     * @return Provider<ReleaseVersion> containing the inferred version,
     * @throws org.gradle.api.internal.provider.MissingValueException if there is no configured git repository in this extension.
     * or empty if none of the scheme strategies can be applied to the arguments.
     */
    Provider<ReleaseVersion> inferVersion(IVersionScheme scheme, Provider<String> stageProvider = this.stage,
                                          Provider<ChangeScope> scopeProvider = this.scope) {
        def strategyProvider = pickStrategy(scheme, stageProvider)
        strategyProvider.map { strategy ->
            def inferParams = VersionInferenceParameters.fromExtension(this).with {
                it.stage = stageProvider.orNull
                it.scope = scopeProvider.orNull
                return it
            }
            return strategy.infer(inferParams)
        }
    }

    private Provider<VersionStrategy> pickStrategy(IVersionScheme scheme, Provider<String> stageProvider) {
        return git.map {
            new GitStrategyPicker(it).pickStrategy(scheme, stageProvider.orNull)
        }.orElse( providers.provider{
            throw new IllegalStateException("A git repository must be available in order to a strategy to be selected")
        })
    }
}