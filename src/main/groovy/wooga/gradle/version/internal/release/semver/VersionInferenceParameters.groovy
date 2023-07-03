/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wooga.gradle.version.internal.release.semver

import com.wooga.gradle.extensions.ProviderExtensions
import groovy.transform.MapConstructor
import groovy.transform.ToString
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.internal.DefaultVersionPluginExtension
import wooga.gradle.version.internal.release.base.PrefixVersionParser
import wooga.gradle.version.internal.release.git.GitVersionRepository

/**
 * Working state used by {@link PartialSemVerStrategy}.
 */
@MapConstructor
@ToString(includeNames = true)
final class VersionInferenceParameters {

    static VersionInferenceParameters fromExtension(VersionPluginExtension extension) {
        return new VersionInferenceParameters([:]).withExtension(extension)
    }

    ChangeScope scope
    String stage = null
    String releaseBranchPattern
    String mainBranchPattern

    //git-releated params
    NearestVersion nearestVersion
    NearestVersion nearestCiMarker
    NearestVersion nearestReleaseMarker
    Commit currentHead
    Branch currentBranch
    boolean repoDirty

    VersionInferenceParameters withExtension(VersionPluginExtension ext) {
        this.scope = ext.scope.orNull
        this.stage = ext.stage.orNull
        this.releaseBranchPattern = ext.releaseBranchPattern.get()
        this.mainBranchPattern = ext.mainBranchPattern.get()
        def git = ext.git.get()
        def versionRepo = ext instanceof DefaultVersionPluginExtension?
                ((DefaultVersionPluginExtension)ext).versionRepo.get():
                GitVersionRepository.fromTagStrategy(git, new PrefixVersionParser(ext.prefix.get(), true))

        return withGit(git, versionRepo)
    }

    VersionInferenceParameters withGit(Grgit git,
                                       GitVersionRepository baseVersionRepo,
                                       GitVersionRepository ciVersionRepo = GitVersionRepository.fromTagPrefix(git, "ci-"), //TODO: do something about this
                                       GitVersionRepository releaseVersionRepo = GitVersionRepository.fromTagPrefix(git, "release-")) {
        this.nearestVersion = baseVersionRepo.nearestVersion()
        this.nearestCiMarker = ciVersionRepo.nearestVersion()
        this.nearestReleaseMarker = releaseVersionRepo.nearestVersion()
        this.currentHead = git.head()
        this.currentBranch = git.branch.current()
        this.repoDirty = !git.status().clean
        return this
    }
}
