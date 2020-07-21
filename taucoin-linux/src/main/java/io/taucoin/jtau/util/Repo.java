package io.taucoin.jtau.util;

import io.taucoin.jtau.config.Config;

/**
 * Repo is the root directory of storing data for jtau.
 * Repo is responsible for creating data directory, generating seed and so on.
 */
public class Repo {

    /**
     * Repo constructor.
     */
    public Repo() {
    }

    public void init(Config config) throws RepoException {

        if (config == null) {
            throw new RepoException("null config object");
        }
    }

    public static class RepoException extends Exception {

        public RepoException() {
            super();
        } 

        public RepoException(String message) {
            super(message);
        }
    }
}
