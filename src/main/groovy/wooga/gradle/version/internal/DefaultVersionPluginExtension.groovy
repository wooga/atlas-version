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
import org.gradle.api.provider.Provider
import wooga.gradle.version.VersionPluginExtension
import wooga.gradle.version.internal.release.base.PrefixVersionParser
import wooga.gradle.version.internal.release.git.GitVersionRepository

class DefaultVersionPluginExtension implements VersionPluginExtension {
    final Provider<GitVersionRepository> versionRepo
    final Provider<GitVersionRepository> ciVersionRepo
    final Provider<GitVersionRepository> releaseVersionRepo

    DefaultVersionPluginExtension() {
        (versionRepo, ciVersionRepo, releaseVersionRepo)  = createVersionRepositories(git, prefix)
    }

    static Tuple3<Provider<GitVersionRepository>,Provider<GitVersionRepository>,Provider<GitVersionRepository>>
    createVersionRepositories(Provider<Grgit> git, Provider<String> prefix) {
        def versionRepo = ProviderExtensions.mapOnce(git, { Grgit it ->
            GitVersionRepository.fromTagStrategy(it, new PrefixVersionParser(prefix.get(), true))
        })
        def ciVersionRepo = ProviderExtensions.mapOnce(git, {Grgit it ->
            GitVersionRepository.fromTagStrategy(it, new PrefixVersionParser("${prefix.get()}ci-", false))
        })
        def releaseVersionRepo = ProviderExtensions.mapOnce(git, {Grgit it ->
            GitVersionRepository.fromTagStrategy(it, new PrefixVersionParser("${prefix.get()}release-", false))
        })
        return new Tuple3<Provider<GitVersionRepository>,Provider<GitVersionRepository>,Provider<GitVersionRepository>>(
                versionRepo, ciVersionRepo, releaseVersionRepo
        )
    }


}
