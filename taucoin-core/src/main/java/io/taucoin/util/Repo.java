package io.taucoin.util;

public class Repo {

    private static String repoPath;

    /**
     * Set repo path where blockchain data is stored.
     *
     * @param path the data directory.
     */
    public static void setRepoPath(String path) {
        repoPath = path;
    }

    /*
     * Get repo path.
     *
     * @return repo path where blockchain data is stored.
     */
    public static String getRepoPath() {
        return repoPath;
    }
}
