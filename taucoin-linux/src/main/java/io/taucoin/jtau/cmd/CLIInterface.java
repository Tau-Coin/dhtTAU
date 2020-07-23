package io.taucoin.jtau.cmd;

import io.taucoin.jtau.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class CLIInterface {

    private static final Logger logger = LoggerFactory.getLogger("cli-option");

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

        for (int i = 0; i < args.length; ++i) {

            String arg = args[i];

            // process help
            if (processHelp(arg)) {
                return true;
            }

            // process simple option
            // TODO:

            // possible additional parameter
            if (i + 1 >= args.length) {
                continue;
            }

            // process options with additional parameter

            if (processDataDir(config, arg, args[i + 1])) {
                continue;
            }

            if (processRpcPort(config, arg, args[i + 1])) {
                continue;
            }

            if (processKeySeed(config, arg, args[i + 1])) {
                continue;
            }
        }

        return false;
    }

    private static boolean processDataDir(Config config, String arg1, String arg2) {
        if (!"-dataDir".equals(arg1)) {
            return false;
        }

        config.setDataDir(arg2);
        logger.info("Data directory set to [{}]", arg2);

        return true;
    }

    private static boolean processRpcPort(Config config, String arg1, String arg2) {
        if (!"-rpcPort".equals(arg1)) {
            return false;
        }

        config.setRPCPort(Integer.parseInt(arg2));
        logger.info("Rpc port set to [{}]", arg2);

        return true;
    }

    private static boolean processKeySeed(Config config, String arg1, String arg2) {
        if (!"-keySeed".equals(arg1)) {
            return false;
        }

        byte[] seed = Hex.decode(arg2);
        config.setKeySeed(seed);
        logger.info("Key seed [{}]", arg2);

        return true;
    }

    private static boolean processHelp(String arg) {
        if (!"--help".equals(arg) || "-h".equals(arg)) {
            return false;
        }

        printHelp();
        return true;
    }

    private static void printHelp() {

        System.out.println("--help              -- this help message ");
        System.out.println("-rpcPort  <port>    -- port to listen on json rpc server ");
        System.out.println("-dataDir            -- data directory ");
        System.out.println("-keySeed            -- data key seed ");
        System.out.println();
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
