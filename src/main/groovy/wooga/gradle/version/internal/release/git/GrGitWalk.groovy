package wooga.gradle.version.internal.release.git

import org.ajoberstar.grgit.Commit
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Repository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils

class GrGitWalk extends RevWalk {

    static GrGitWalk fromGrGit(Grgit grgit) {
        return new GrGitWalk(grgit.repository)
    }

    GrGitWalk(Repository grgitRepo) {
        super(grgitRepo.jgit.repository)
        this.retainBody = false
    }

    public <T> T walk(Closure<T> operation) {
        try {
            operation(this)
        } finally {
            this.reset()
        }
    }

    def <T> T walkFrom(Commit revLike, Closure<T> operation) {
        walk { GrGitWalk revWalk ->
            revWalk.reset()
            revWalk.markStart(toRev(revLike))
            return operation(revWalk)
        }
    }

    RevCommit toRev(Commit commit) {
        def id = ObjectId.fromString(commit.id)
        return parseCommit(id)
    }

    int distanceBetween(Commit src, Commit dest) {
        walk { GrGitWalk revWalk ->
            return RevWalkUtils.count(revWalk, toRev(src), dest? toRev(dest) : null)
        }
    }
}