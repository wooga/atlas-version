package wooga.gradle.version.internal.release.git

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Tag
import wooga.gradle.version.internal.release.base.MarkerTagStrategy
import wooga.gradle.version.internal.release.base.TagStrategy
import wooga.gradle.version.internal.release.semver.NearestVersion

import java.util.stream.Stream
import java.util.stream.StreamSupport

class GitVersionRepository implements Closeable {

    static GitVersionRepository fromTagPrefix(Grgit grgit, String prefix) {
        return fromTagStrategy(grgit, new MarkerTagStrategy(prefix))
    }

    static GitVersionRepository fromTagStrategy(Grgit grgit, TagStrategy strategy) {
        def walker = GrGitWalk.fromGrGit(grgit)
        return new GitVersionRepository(grgit, walker, strategy)
    }

    final Grgit grgit
    final GrGitWalk walker
    final TagStrategy strategy

    GitVersionRepository(Grgit grgit, GrGitWalk walker, TagStrategy strategy) {
        this.grgit = grgit
        this.walker = walker
        this.strategy = strategy
    }

    NearestVersion nearestVersion(TagStrategy strategy = this.strategy) {
        def head = grgit.head()

        def allVersions = versionsMatching(strategy)
        def normalVersions = allVersions.findAll { !it.version.preReleaseVersion }

        def nearestNormal = nearestVersionFrom(head, normalVersions)
        def nearestAny = nearestVersionFrom(head, allVersions)
        return new NearestVersion([any: nearestAny.version, normal: nearestNormal.version,
                                   distanceFromAny: nearestAny.distance, distanceFromNormal: nearestNormal.distance])
    }

    Integer countReachableVersions(Boolean countPrerelease = true) {
        return countReachableVersions(this.strategy, countPrerelease)
    }

    Integer countReachableVersions(TagStrategy strategy, Boolean countPrerelease = true) {
        def allVersions = versionsMatching(strategy)
        def normalVersions = allVersions.findAll {!it.version.preReleaseVersion }
        def tagsToCalculate = (countPrerelease) ? allVersions : normalVersions

        return countReachableVersions(grgit.head(), tagsToCalculate)
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

    protected Integer countReachableVersions(Commit head, List<GitVersion> versions) {
        def reachableVersionTags = reachableVersionsFrom(head, versions)
        return reachableVersionTags? reachableVersionTags.size() : 0
    }

    protected GitVersion createVersion(TagStrategy strategy, Tag tag) {
        def version = strategy.parseTag(tag)
        def commit = grgit.resolve.toCommit(tag)
        return version? new GitVersion(version, tag, commit, -1) : null
    }

    protected List<GitVersion> versionsMatching(TagStrategy strategy) {
        return grgit.tag.list().collect { tag ->
            createVersion(strategy, tag)
        }.findAll {it != null }
    }

    @Override
    void close() throws IOException {
        walker.dispose()
    }
}


