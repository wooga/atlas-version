package wooga.gradle.version.strategies.operations

import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy

class BuildMetadataPartials {

    static final PartialSemVerStrategy DEVELOPMENT_METADATA_STRATEGY = { state ->
        state.copyWith(inferredBuildMetadata: state.currentHead.abbreviatedId)
    }
}
