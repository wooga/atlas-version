package wooga.gradle.version

enum ReleaseStage {
    /**
     * Used to denote a release that was not able to be deduced
     */
    Unknown,
    /**
     * The default release
     */
    Development,
    /**
     * A release that is conventionally produced by a CI pipeline.
     * (It generally uses a counter from the branch and the number of commits since the last tag)
     */
    Snapshot,
    /**
     * A release that is mainly used for internal integration testing
     */
    Preflight,
    /**
     * A release for early adopters
     */
    Prerelease,
    /**
     * A published release with a high degree of confidence
     */
    Final,
}
