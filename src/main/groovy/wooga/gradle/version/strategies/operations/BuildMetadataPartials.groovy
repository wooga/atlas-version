package wooga.gradle.version.strategies.operations

import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy

class BuildMetadataPartials {

    static final PartialSemVerStrategy DEVELOPMENT_METADATA_STRATEGY = { state ->
        boolean needsBranchMetadata = false
        String shortenedBranch = (state.currentBranch.name =~ /(?:(?:bugfix|feature|hotfix|release)(?:-|\/))?(.+)/)[0][1]
        shortenedBranch = shortenedBranch.replaceAll(/[_\/-]/, '.')
        def metadata = needsBranchMetadata ? "${shortenedBranch}.${state.currentHead.abbreviatedId}" : state.currentHead.abbreviatedId
        state.copyWith(inferredBuildMetadata: metadata)
    }
}
