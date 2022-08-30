package wooga.gradle.version.strategies.operations

import wooga.gradle.version.internal.release.opinion.Strategies
import wooga.gradle.version.internal.release.semver.PartialSemVerStrategy
import wooga.gradle.version.internal.release.semver.SemVerStrategyState
import static wooga.gradle.version.internal.release.semver.StrategyUtil.closure
import static wooga.gradle.version.internal.release.semver.StrategyUtil.all

class PreReleasePartials {

    /**
     *
     */
    public static PartialSemVerStrategy STAGE_WITH_PADDED_COUNT = all (
        Strategies.PreRelease.STAGE_FIXED,
        Strategies.PreRelease.countIncremented("", 5),
    )

    /**
     *
     */
    public static PartialSemVerStrategy V2_STAGE_BRANCH_NAME = closure { SemVerStrategyState state ->
        String branchName = state.currentBranch.name
        String prefix = "branch"

        if (branchName == "HEAD" && System.getenv("BRANCH_NAME")) {
            branchName = System.getenv("BRANCH_NAME")
        }
        if (!branchName.matches(state.mainBranchPattern)) {
            branchName = "$prefix.${branchName.toLowerCase()}"
        }

        branchName = branchNameAsSemverV2Prerelease(branchName)

        def inferred = state.inferredPreRelease ? "${state.inferredPreRelease}.${branchName}" : "${branchName}"
        state.copyWith(inferredPreRelease: inferred)
    }

    /**
     *
     */
    public static final PartialSemVerStrategy PAKET_BRANCH_NAME = closure({ state ->

        String branchName = state.currentBranch.name

        if( branchName == "HEAD" && System.getenv("BRANCH_NAME") ) {
            branchName = System.getenv("BRANCH_NAME")
        }

        if( branchName != "master") {
            String prefix = "branch"
            branchName = "$prefix${branchName.capitalize()}"
        }

        branchName = branchName.replaceAll(/(\/|-|_)([\w])/) {all, delimiter, firstAfter -> "${firstAfter.capitalize()}" }
        branchName = branchName.replaceAll(/\./, "Dot")
        branchName = numbersAsLiteralNumbers(branchName)

        def buildSinceAny = state.nearestVersion.distanceFromNormal
        def integration = "$buildSinceAny".padLeft(5, '0')
        state.copyWith(inferredPreRelease: "$branchName$integration")
    })

    protected static String numbersAsLiteralNumbers(String branchName) {
        return branchName.replaceAll(/0/, "Zero")
                .replaceAll(/1/, "One")
                .replaceAll(/2/, "Two")
                .replaceAll(/3/, "Three")
                .replaceAll(/4/, "Four")
                .replaceAll(/5/, "Five")
                .replaceAll(/6/, "Six")
                .replaceAll(/7/, "Seven")
                .replaceAll(/8/, "Eight")
                .replaceAll(/9/, "Nine")
    }

    protected static String branchNameAsSemverV2Prerelease(String branchName) {
        //Split at branch delimiter /-_+ and replace with .
        branchName = branchName.replaceAll(/((\/|-|_|\.)+)([\w])/) { all, delimiterAll, delimiter, firstAfter -> ".${firstAfter}" }
        //Remove all hanging /-_+
        branchName = branchName.replaceAll(/[-\/_\+]+$/) { "" }
        //parse all digits and replace with unpadded value e.g. 001 -> 1
        branchName = branchName.replaceAll(/([\w\.])([0-9]+)/) { all, s, delimiter ->
            if (s == ".") {
                s = ""
            }
            "${s}.${Integer.parseInt(delimiter).toString()}"
        }
        return branchName
    }
}
