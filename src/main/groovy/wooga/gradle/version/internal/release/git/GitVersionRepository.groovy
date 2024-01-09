package wooga.gradle.version.internal.release.git

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import wooga.gradle.version.internal.release.base.PrefixVersionParser
import wooga.gradle.version.internal.release.base.VersionParser
import wooga.gradle.version.internal.release.semver.NearestVersion

/**
 * Version storage in a git backend.
 */
class GitVersionRepository implements Closeable {

    /**
     * Creates a new GitVersionRespository based on a git repository (GrGit instance) and a parser that translates tags into versions.
     * @param grgit - desired base git repository
     * @param parser - translates, in this case, git tag names into proper versions
     * @return GitVersionRepository
     */
    static GitVersionRepository fromTagStrategy(Grgit grgit, VersionParser parser) {
        def walker = GrGitWalk.fromGrGit(grgit)
        return new GitVersionRepository(grgit, walker, parser)
    }

    final Grgit grgit
    final GrGitWalk walker
    final VersionParser parser

    GitVersionRepository(Grgit grgit, GrGitWalk walker, VersionParser parser) {
        this.grgit = grgit
        this.walker = walker
        this.parser = parser
    }

    /**
     * Finds the nearest version to the repository current HEAD.
     * @param parser - Custom tag name to version parser. Defaults to the one informed in the object creation.
     * @return NearestVersion with data regarding the nearest version.
     */
    NearestVersion nearestVersion(VersionParser parser = this.parser) {
        def head = grgit.head()

        def allVersions = versionsMatching(parser)
        def normalVersions = allVersions.findAll { !it.version.preReleaseVersion }

        def nearestNormal = nearestVersionFrom(head, normalVersions)
        def nearestAny = nearestVersionFrom(head, allVersions)
        return new NearestVersion([any: nearestAny.version, normal: nearestNormal.version,
                                   distanceFromAny: nearestAny.distance, distanceFromNormal: nearestNormal.distance])
    }

    Integer countReachableVersions(Boolean countPrerelease = true) {
        return countReachableVersions(this.parser, countPrerelease)
    }

    int countReachableVersions(VersionParser parser, Boolean countPrerelease = true) {
        def allVersions = versionsMatching(parser)
        def normalVersions = allVersions.findAll {!it.version.preReleaseVersion }
        def tagsToCalculate = (countPrerelease) ? allVersions : normalVersions

        return countReachableVersions(grgit.head(), tagsToCalculate)
    }

    protected int countReachableVersions(Commit head, List<GitVersion> versions) {
        def reachableVersionTags = reachableVersionsFrom(head, versions)
        return reachableVersionTags? reachableVersionTags.size() : 0
    }

    protected Collection<GitVersion> reachableVersionsFrom(Commit head, List<GitVersion> versionTags, boolean unmarkParents = false) {
        walker.walkFrom(head) {
            def versionTagsByRev = versionTags.groupBy {
                walker.toRev(it.commit)
            }
            return walker.collectMany { rev ->
                def revVersions = versionTagsByRev[rev]
                if (revVersions && unmarkParents) {
                    // Parents can't be "nearer". Exclude them to avoid extra walking.
                    rev.parents.each { walker.markUninteresting(it) }
                }
                return revVersions?: []
            }
        }
    }

    protected GitVersion nearestVersionFrom(Commit head, List<GitVersion> versionTags) {
        def reachableVersionTags = reachableVersionsFrom(head, versionTags,true).each {
            GitVersion it -> it.distance = walker.distanceBetween(head, it.commit)
        }

        if (!reachableVersionTags.empty) {
            return reachableVersionTags.min { a, b ->
                def distanceCompare = a.distance <=> b.distance
                def versionCompare = (a.version <=> b.version) * -1
                distanceCompare == 0 ? versionCompare : distanceCompare
            }
        } else {
            return GitVersion.empty(walker.distanceBetween(head, null))
        }
    }


    protected GitVersion createVersion(VersionParser parser, Tag tag) {
        def version = parser.parse(tag.name)
        def commit = grgit.resolve.toCommit(tag)
        return version? new GitVersion(version, tag, commit, -1) : null
    }

    protected List<GitVersion> versionsMatching(VersionParser parser) {
        return grgit.tag.list().collect { tag ->
            createVersion(parser, tag)
        }.findAll {it != null }
    }

    @Override
    void close() throws IOException {
        walker.dispose()
    }
}


