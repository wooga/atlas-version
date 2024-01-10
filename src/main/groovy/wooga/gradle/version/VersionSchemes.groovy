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

import wooga.gradle.version.internal.release.base.VersionStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategy
import wooga.gradle.version.strategies.StaticMarkerStrategies
import wooga.gradle.version.strategies.opinion.LegacyNuGetStrategies
import wooga.gradle.version.strategies.opinion.SemverV2WithDefaultStrategies
import wooga.gradle.version.strategies.opinion.UpmStrategies
import wooga.gradle.version.strategies.opinion.WdkNuGetStrategies

/**
 * Version schemes available for use with this plugin.
 * Each scheme consists of at least 4 strategies (development, snapshot, pre-release and final)
 * plus additional strategies if so desired. Check IVersionScheme for more details.
 */
enum VersionSchemes implements VersionScheme {
    semver(LegacyNuGetStrategies.DEVELOPMENT,
            LegacyNuGetStrategies.SNAPSHOT,
            LegacyNuGetStrategies.PRE_RELEASE,
            LegacyNuGetStrategies.FINAL),
    semver2(SemverV2WithDefaultStrategies.DEVELOPMENT,
            SemverV2WithDefaultStrategies.SNAPSHOT,
            SemverV2WithDefaultStrategies.PRE_RELEASE,
            SemverV2WithDefaultStrategies.FINAL),
    /**
     * Strategies for versions generated based on a static marker. Based on Semver v2.
     */
    staticMarker(StaticMarkerStrategies.DEVELOPMENT,
            StaticMarkerStrategies.SNAPSHOT,
            StaticMarkerStrategies.PRE_RELEASE,
            StaticMarkerStrategies.FINAL, StaticMarkerStrategies.PREFLIGHT),
    /**
     * Schema for Paket/Nuget WDK projects. See WdkNuGetStrategies for more details.
     */
    wdk(WdkNuGetStrategies.DEVELOPMENT,
            WdkNuGetStrategies.SNAPSHOT,
            WdkNuGetStrategies.PRE_RELEASE,
            WdkNuGetStrategies.FINAL, WdkNuGetStrategies.PREFLIGHT),

    /**
     * Schema for UPM-based unity3d projects. See UpmStrategies for more details.
     * IMPORTANT: please note that the pre-release strategy for this schema does not enforces order between versions.
     */
    upm(UpmStrategies.DEVELOPMENT,
            UpmStrategies.SNAPSHOT,
            UpmStrategies.PRE_RELEASE.copyWith(enforcePrecedence: false),
            UpmStrategies.FINAL, UpmStrategies.PREFLIGHT)

    final VersionStrategy development
    final VersionStrategy snapshot
    final VersionStrategy preRelease
    final VersionStrategy finalStrategy

    final List<VersionStrategy> strategies
    final VersionStrategy defaultStrategy

    VersionSchemes(VersionStrategy development, VersionStrategy snapshot, VersionStrategy preRelease, VersionStrategy finalStrategy, SemVerStrategy... others) {
        this.development = development
        this.snapshot = snapshot
        this.preRelease = preRelease
        this.finalStrategy = finalStrategy
        this.defaultStrategy = development
        this.strategies = [development, snapshot, preRelease, finalStrategy, *others]
    }
}


