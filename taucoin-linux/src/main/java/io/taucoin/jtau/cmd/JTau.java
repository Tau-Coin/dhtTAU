package io.taucoin.jtau.cmd;

import io.taucoin.jtau.config.Config;
import io.taucoin.jtau.util.Repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.taucoin.jtau.cmd.CLIInterface.ArgumentException;
import static io.taucoin.jtau.cmd.ExitCode.*;
import static io.taucoin.jtau.util.Repo.RepoException;

public class JTau {

    private static final Logger logger = LoggerFactory.getLogger("jtau");

    public static void main(String[] args) {

        Config config = new Config();
        Repo repo = new Repo();
        boolean isNormalExit = false;

        // parse commandline arguments.
        try {
            isNormalExit = CLIInterface.parse(config, args);
        } catch (ArgumentException e) {
            logger.error(e.getMessage());
            System.exit(CLI_ARGUMENTS_PARSE_ERROR);
        }
        if (isNormalExit) {
            System.exit(NORMAL);
        }

        // init repo
        try {
            repo.init(config);
        } catch (RepoException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            System.exit(INIT_REPO_ERROR);
        }

        // Create tau daemon and start it.
        final TauDaemon tauDaemon = new TauDaemon(config);
        final Thread thread = new Thread(tauDaemon);
        thread.setDaemon(true);
        thread.start();

        // Register shutdown hook.
        // Note: for foreground application, this hook
        // can capture INT(ctrl + c or kill -2) and TERM(kill -15) signal.
        // But for background application, this hook can only
        // capture TERM(kill -15) signal.
        // And for both types application, it can't capture KILL(kill -9) signal.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("handling shutdown signal");
                tauDaemon.stop();
                thread.interrupt();
            }
        });

        try {
            thread.join();
        } catch (InterruptedException e) {
            // Ignore this exception
        }

        logger.info("JTau is exiting...");
    }
}
