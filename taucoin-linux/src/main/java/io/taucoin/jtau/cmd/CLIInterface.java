package io.taucoin.jtau.cmd;

import io.taucoin.jtau.config.Config;

public class CLIInterface {

    /**
     * Parse command line arguments.
     *
     * @param config Config
     * @param args command line arguments
     * @return boolean is help or not.
     * @throws ArgumentException.
     */
    public static boolean parse(Config config, String[] args) throws ArgumentException {

        if (config == null) {
            throw new ArgumentException("null config object");
        }

        return false;
    }

    public static class ArgumentException extends Exception {

        public ArgumentException() {
            super();
        }

        public ArgumentException(String message) {
            super(message);
        }
    }
}
