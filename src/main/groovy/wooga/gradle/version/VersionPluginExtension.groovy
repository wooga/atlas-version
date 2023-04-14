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
import com.wooga.gradle.extensions.ProviderExtensions
import org.ajoberstar.grgit.Grgit
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import wooga.gradle.version.internal.GitStrategyPicker
import wooga.gradle.version.internal.release.base.ReleaseVersion
import wooga.gradle.version.internal.release.base.VersionStrategy
import wooga.gradle.version.internal.release.git.GitVersionRepository
import wooga.gradle.version.internal.release.base.PrefixVersionParser
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.VersionInferenceParameters

//TODO 3.x: review what should be exposed and what shouldnt. Maybe split this into internal/external and/or interface.
trait VersionPluginExtension implements BaseSpec {

    /**
     * The version scheme being used by the plugin (such as SemVer2)
     */
    private final Property<VersionScheme> versionScheme = objects.property(VersionScheme)

    Property<VersionScheme> getVersionScheme() {
        versionScheme
    }

    void setVersionScheme(VersionScheme value) {
        versionScheme.set(value)
    }

    void setVersionScheme(Provider<VersionScheme> value) {
        versionScheme.set(value)
    }

    void setVersionScheme(String value) {
        versionScheme.set(VersionSchemes.valueOf(value.trim()))
    }

    /**
     * @return The version code scheme being used by the plugin (such as SemVer2, releaseCount)
     */
    private final Property<VersionCodeSchemes> versionCodeScheme = objects.property(VersionCodeSchemes)

    Property<VersionCodeSchemes> getVersionCodeScheme() {
        versionCodeScheme
    }


    void versionCodeScheme(VersionCodeSchemes value) {
        setVersionCodeScheme(value)
    }

    void versionCodeScheme(Provider<VersionCodeSchemes> value) {
        setVersionCodeScheme(value)
    }

    void setVersionCodeScheme(VersionCodeSchemes value) {
        versionCodeScheme.set(value)
    }

    void setVersionCodeScheme(Provider<VersionCodeSchemes> value) {
        versionCodeScheme.set(value)
    }

    void versionCodeScheme(String value) {
        setVersionCodeScheme(value)
    }

    void setVersionCodeScheme(String value) {
        versionCodeScheme.set(VersionCodeSchemes.valueOf(value.trim()))
    }

    /**
     * @return If set, applies an offset to {@code project.versionCode}.
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
     * @return If set, mark matching branches as release branches
     */
    Property<String> getReleaseBranchPattern() {
        releaseBranchPattern
    }
    //TODO 3.x: Defaults for this property are embbed in the strategy. Find a way to get them to extension-level, as well as other defaults.
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
     * @return If set, mark matching branches as main branches
     */
    Property<String> getMainBranchPattern() {
        mainBranchPattern
    }
    //TODO 3.x: Defaults for this property are embbed in the strategy. Find a way to get them to extension-level, as well as other defaults.
    //TODO: 3x. This is true for other properties as well, investigate.
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
     * @return The git client used to fetch version tags and local repository information like branch names.
     */
    Property<Grgit> getGit() {
        git
    }

    final Property<Grgit> git = objects.property(Grgit)

    void setGit(Grgit grgit) {
        git.set(grgit)
    }

    void setGit(Provider<Grgit> grgit) {
        git.set(grgit)
    }

    /**
     * @return The evaluated version of the project.
     * A new version will not be calculated if this property is set externally.
     */
    Property<ReleaseVersion> getVersion() {
        version
    }

    private final Property<ReleaseVersion> version = objects.property(ReleaseVersion)

    void setVersion(Provider<ReleaseVersion> value) {
        version.set(value)
    }

    void setVersion(ReleaseVersion version) {
        version.set(version)
    }

    void setVersion(String versionStr) {
        setVersion(new ReleaseVersion(version: versionStr))
    }

    /**
     * @return The evaluated version code of the project. Won't be calculated if this property is set externally.
     */
    Property<Integer> getVersionCode() {
        versionCode
    }

    private final Property<Integer> versionCode = objects.property(Integer)

    void setVersionCode(Provider<Integer> value) {
        versionCode.set(value)
    }

    void setVersionCode(Integer value) {
        versionCode.set(value)
    }

    /**
     * @return The stage to be used for evaluating the project version (such as rc, snapshot, final)
     */
    Property<String> getStage() {
        stage
    }
    //TODO 3.x: This guy defaults are kinda messy, figure this out
    private final Property<String> stage = objects.property(String)

    void setStage(Provider<String> value) {
        stage.set(value)
    }

    void setStage(String value) {
        stage.set(value)
    }

    /**
     * @return The scope to be used for evaluating the project version (such as major, minor, patch)
     */
    Property<ChangeScope> getScope() {
        scope
    }

    private final Property<ChangeScope> scope = objects.property(ChangeScope)

    void setScope(Provider<ChangeScope> value) {
        scope.set(value)
    }

    void setScope(ChangeScope scope) {
        this.scope.set(scope)
    }

    void setScope(String scope) {
        this.scope.set(ChangeScope.valueOf(scope.trim()))
    }

    private final Property<String> prefix = objects.property(String)

    /**
     * @return Prefix to be used when storing versions. Useful to distinguish projects in a same repository
     */
    Property<String> getPrefix() {
        return prefix
    }

    void setPrefix(String prefix) {
        this.prefix.set(prefix)
    }

    void setPrefix(Provider<String> prefix) {
        this.prefix.set(prefix)
    }

    /**
     * @return Whether this is a development build
     */
    Provider<Boolean> getIsDevelopment() {
        isDevelopment
    }

    void setIsDevelopment(Provider<Boolean> value) {
        isDevelopment.set(value)
    }

    private final Property<Boolean> isDevelopment = objects.property(Boolean)

    /**
     * @return Whether this is a production build
     */
    Property<Boolean> getIsFinal() {
        isFinal
    }

    private final Property<Boolean> isFinal = objects.property(Boolean)

    void setIsFinal(Provider<Boolean> value) {
        isFinal.set(value)
    }

    void setIsFinal(Boolean isFinal) {
        this.isFinal.set(isFinal)
    }

    /**
     * @return Whether this is a pre-release build
     */
    Property<Boolean> getIsPrerelease() {
        isPrerelease
    }

    private final Property<Boolean> isPrerelease = objects.property(Boolean)

    void setIsPrerelease(Provider<Boolean> value) {
        isPrerelease.set(value)
    }

    void setIsPrerelease(Boolean value) {
        isPrerelease.set(value)
    }

    /**
     * @return Whether this is a snapshot build (such as from a CI environment)
     */
    Provider<Boolean> getIsSnapshot() {
        isSnapshot
    }

    private final Property<Boolean> isSnapshot = objects.property(Boolean)

    void setIsSnapshot(Provider<Boolean> value) {
        isSnapshot.set(value)
    }

    void setIsSnapshot(Boolean value) {
        isSnapshot.set(value)
    }

    /**
     * @return The deduced release stage for this build
     */
    Provider<ReleaseStage> getReleaseStage() {
        releaseStage
    }
    //TODO 3.x: isDevelopment, isFinal, etc are only used to calculate this. Expose only releaseStage
    // and don't expose is<<stage>> properties.
    private final Provider<ReleaseStage> releaseStage = providers.provider { ->
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

    //TODO 3.x don't expose this.
    Provider<GitVersionRepository> versionRepo = ProviderExtensions.mapOnce(git) { Grgit it ->
        GitVersionRepository.fromTagStrategy(it, new PrefixVersionParser(prefix.get(), true))
    }

    /**
     * Infers the next version for this project based on extension information and underlying git repository tags.
     * @param scheme - version scheme that this project is following
     * @param stageProvider - provider with the stage desired for the next version
     * @param scopeProvider - provider with the ChangeScope desired for the next version
     * @return Provider<ReleaseVersion> containing the inferred version,
     * @throws org.gradle.api.internal.provider.MissingValueException if there is no configured git repository in this extension.
     * or empty if none of the scheme strategies can be applied to the arguments.
     */
    Provider<ReleaseVersion> inferVersion(VersionScheme scheme, Provider<String> stageProvider = this.stage,
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

    private Provider<VersionStrategy> pickStrategy(VersionScheme scheme, Provider<String> stageProvider) {
        return git.map {
            new GitStrategyPicker(it).pickStrategy(scheme, stageProvider.orNull)
        }.orElse( providers.provider{
            throw new IllegalStateException("A git repository must be available in order to a strategy to be selected")
        })
    }
}