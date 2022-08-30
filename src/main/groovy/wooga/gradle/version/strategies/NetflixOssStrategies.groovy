/*
 * Copyright 2014-2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wooga.gradle.version.strategies

import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.ChangeScope
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.internal.release.semver.StrategyUtil
import org.gradle.api.Project
import wooga.gradle.version.strategies.operations.BuildMetadataPartials
import wooga.gradle.version.strategies.operations.NormalPartials

import java.util.regex.Pattern

class NetflixOssStrategies {
    static final PartialSemVerStrategy TRAVIS_BRANCH_MAJOR_X = NormalPartials.TRAVIS_BRANCH_MAJOR_X
    static final PartialSemVerStrategy TRAVIS_BRANCH_MAJOR_MINOR_X = NormalPartials.TRAVIS_BRANCH_MAJOR_MINOR_X
    static final PartialSemVerStrategy NEAREST_HIGHER_ANY = NormalPartials.NEAREST_HIGHER_ANY
    private static final scopes = StrategyUtil.one(
            Strategies.Normal.USE_SCOPE_PROP,
            NormalPartials.TRAVIS_BRANCH_MAJOR_X,
            NormalPartials.TRAVIS_BRANCH_MAJOR_MINOR_X,
            NormalPartials.SCOPE_EMBED_IN_BRANCH,
            NormalPartials.NEAREST_HIGHER_ANY,
            Strategies.Normal.useScope(ChangeScope.MINOR)
    )

    static final SemVerStrategy SNAPSHOT = Strategies.SNAPSHOT.copyWith(normalStrategy: scopes)
    static final SemVerStrategy DEVELOPMENT = Strategies.DEVELOPMENT.copyWith(
            normalStrategy: scopes,
            buildMetadataStrategy: BuildMetadataPartials.DEVELOPMENT_METADATA_STRATEGY)
    static final SemVerStrategy PRE_RELEASE = Strategies.PRE_RELEASE.copyWith(normalStrategy: scopes)
    static final SemVerStrategy FINAL = Strategies.FINAL.copyWith(normalStrategy: scopes)

    @Deprecated
    static final class BuildMetadata {
        static final PartialSemVerStrategy DEVELOPMENT_METADATA_STRATEGY = BuildMetadataPartials.DEVELOPMENT_METADATA_STRATEGY
    }

    static Project project

    @Deprecated
    static final String TRAVIS_BRANCH_PROP = 'release.travisBranch'

    @Deprecated
    static PartialSemVerStrategy fromTravisPropertyPattern(Pattern pattern) {
        return NormalPartials.fromBranchProperty(project, TRAVIS_BRANCH_PROP, pattern)
    }

    /**
     * If the nearest any is higher from the nearest normal, sets the
     * normal component to the nearest any's normal component. Otherwise
     * do nothing.
     *
     * <p>
     * For example, if the nearest any is {@code 1.2.3-alpha.1} and the
     * nearest normal is {@code 1.2.2}, this will infer the normal
     * component as {@code 1.2.3}.
     * </p>
     */
    @Deprecated
    static PartialSemVerStrategy nearestHigherAny() {
        return StrategyUtil.closure { state -> NormalPartials.NEAREST_HIGHER_ANY.infer(state) }
    }
}
