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

import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.strategies.StaticMarkerStrategies
import wooga.gradle.version.strategies.opinion.LegacyNuGetStrategies
import wooga.gradle.version.strategies.opinion.SemverV2WithDefaultStrategies
import wooga.gradle.version.strategies.opinion.UpmStrategies
import wooga.gradle.version.strategies.opinion.WdkNuGetStrategies

//TODO: Rename to VersionSchemes when breaking change
enum VersionScheme implements IVersionScheme {
    semver(LegacyNuGetStrategies.DEVELOPMENT,
            LegacyNuGetStrategies.SNAPSHOT,
            LegacyNuGetStrategies.PRE_RELEASE,
            LegacyNuGetStrategies.FINAL),
    semver2(SemverV2WithDefaultStrategies.DEVELOPMENT,
            SemverV2WithDefaultStrategies.SNAPSHOT,
            SemverV2WithDefaultStrategies.PRE_RELEASE,
            SemverV2WithDefaultStrategies.FINAL),
    staticMarker(StaticMarkerStrategies.DEVELOPMENT,
            StaticMarkerStrategies.SNAPSHOT,
            StaticMarkerStrategies.PRE_RELEASE,
            StaticMarkerStrategies.FINAL, StaticMarkerStrategies.PREFLIGHT),
    wdk(WdkNuGetStrategies.DEVELOPMENT,
            WdkNuGetStrategies.SNAPSHOT,
            WdkNuGetStrategies.PRE_RELEASE,
            WdkNuGetStrategies.FINAL, WdkNuGetStrategies.PREFLIGHT),
    upm(UpmStrategies.DEVELOPMENT,
            UpmStrategies.SNAPSHOT,
            UpmStrategies.PRE_RELEASE.copyWith(enforcePrecedence: false),
            UpmStrategies.FINAL, UpmStrategies.PREFLIGHT)

    final SemVerStrategy development
    final SemVerStrategy snapshot
    final SemVerStrategy preRelease
    final SemVerStrategy finalStrategy

    final List<SemVerStrategy> strategies
    final SemVerStrategy defaultStrategy

    VersionScheme(SemVerStrategy development, SemVerStrategy snapshot, SemVerStrategy preRelease, SemVerStrategy finalStrategy, SemVerStrategy... others) {
        this.development = development
        this.snapshot = snapshot
        this.preRelease = preRelease
        this.finalStrategy = finalStrategy
        this.defaultStrategy = development
        this.strategies = [development, snapshot, preRelease, finalStrategy, *others]
    }
}


