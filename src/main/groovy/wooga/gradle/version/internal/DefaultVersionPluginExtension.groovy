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

package wooga.gradle.version.internal

import com.wooga.gradle.extensions.ProviderExtensions
import org.ajoberstar.grgit.Grgit
import wooga.gradle.version.ReleaseStage
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.internal.release.base.PrefixVersionParser
import wooga.gradle.version.internal.release.git.GitVersionRepository

class DefaultVersionPluginExtension implements VersionPluginExtension {

    private final Project project;

    DefaultVersionPluginExtension(Project project) {
        this.project = project
        this.versionRepo = ProviderExtensions.mapOnce(git) { Grgit it ->
            GitVersionRepository.fromTagStrategy(it, new PrefixVersionParser(prefix.get(), true))
        }

        // It's development if the development strategy contains the set `stage` OR
        // if the default strategy's release stage is development
        this.isDevelopment = canRunStageWithName(ReleaseStage.Development, stage)
        this.isSnapshot = canRunStageWithName(ReleaseStage.Snapshot, stage)
        this.isPrerelease = canRunStageWithName(ReleaseStage.Prerelease, stage)
        this.isFinal = canRunStageWithName(ReleaseStage.Final, stage)

    }

    private Provider<Boolean> canRunStageWithName(ReleaseStage releaseStage, Provider<String> stageProvider) {
        def strategy = versionScheme.map{
            it.strategyFor(releaseStage)
        }

        return strategy.flatMap { versionStrategy ->
            stageProvider.map{ stage -> versionStrategy.stages.contains(stage) }
        }
        .orElse(versionScheme.map({
            it.defaultStrategy.releaseStage == releaseStage
        }))
    }
}
