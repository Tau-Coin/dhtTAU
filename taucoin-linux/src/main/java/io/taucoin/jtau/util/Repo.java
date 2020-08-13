package io.taucoin.jtau.util;

import io.taucoin.jtau.config.Config;

import com.frostwire.jlibtorrent.Ed25519;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Repo is the root directory of storing data for jtau.
 * Repo is responsible for creating data directory, generating seed and so on.
 */
public class Repo {

    private static final Logger logger = LoggerFactory.getLogger("repo");

    // environment variable to specify the data root directory.
    public static final String TAU_PATH = "TAU_PATH";

    public static final String DIR_NAME = ".jtau";

    public static final String KEY_FILE_NAME = "key";

    private static volatile Repo INSTANCE;

    private Config config;

    /**
     * Get Repo instance.
     *
     * @return Repo instance.
     */
    public static Repo getInstance() {
        if (INSTANCE == null) {
            synchronized (Repo.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Repo();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * Repo constructor.
     */
    private Repo() {
    }

    public void init(Config config) throws RepoException {

        if (config == null) {
            throw new RepoException("null config object");
        }

        this.config = config;

        // init root directory.
        String dir = config.getDataDir();
        File rootDir = new File(dir);
        if (!rootDir.exists()) {
            rootDir.mkdir();
            logger.info("make directory:" + dir);
        }

        // generate or load key seed.
        if (config.getKeySeed() == null) {
            File keyFile = new File(dir + File.separator + KEY_FILE_NAME);

            if (keyFile.exists()) {
                try {
                    byte[] seed = Files.readAllBytes(Paths.get(keyFile.getAbsolutePath()));
                    String hexStr = new String(seed);

                    logger.info("loading key seed:" + hexStr);
                    config.setKeySeed(Hex.decode(hexStr));
                } catch (IOException e) {
                    throw new RepoException(e.getMessage());
                }
            } else {
                byte[] seed = Ed25519.createSeed();
                config.setKeySeed(seed);
                String hexStr = Hex.toHexString(seed);
                // TODO: confuse key seed
                logger.info("generating key seed:" + hexStr);

                try {
                    Files.write(Paths.get(keyFile.getAbsolutePath()), hexStr.getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RepoException(e.getMessage());
                }
            }
        }
    }

    public void updateKeySeed(byte[] seed) throws RepoException {

        if (this.config == null) {
            throw new RepoException("null config object");
        }

        File keyFile = new File(config.getDataDir() + File.separator + KEY_FILE_NAME);
        String hexStr = Hex.toHexString(seed);

        try {
            Files.write(Paths.get(keyFile.getAbsolutePath()), hexStr.getBytes(),
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RepoException(e.getMessage());
        }

        logger.info("update key seed successfully:" + hexStr);
        this.config.setKeySeed(seed);
    }

    public static String getDefaultDataDir() {

        String dir = System.getenv(TAU_PATH);
        if (dir != null && !"".equals(dir)) {
            return dir;
        }

        return System.getenv("HOME") + File.separator + DIR_NAME;
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
