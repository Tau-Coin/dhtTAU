package io.taucoin.jtau.util;

import io.taucoin.jtau.config.Config;

import com.frostwire.jlibtorrent.Ed25519;
import org.spongycastle.util.encoders.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Repo is the root directory of storing data for jtau.
 * Repo is responsible for creating data directory, generating seed and so on.
 */
public class Repo {

    // environment variable to specify the data root directory.
    public static final String TAU_PATH = "TAU_PATH";

    public static final String DIR_NAME = ".jtau";

    public static final String KEY_FILE_NAME = "key";

    /**
     * Repo constructor.
     */
    public Repo() {
    }

    public void init(Config config) throws RepoException {

        if (config == null) {
            throw new RepoException("null config object");
        }

        // init root directory.
        String dir = config.getDataDir();
        File rootDir = new File(dir);
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }

        // generate or load key seed.
        if (config.getKeySeed() == null) {
            File keyFile = new File(dir + "/" + KEY_FILE_NAME);

            if (keyFile.exists()) {
                FileInputStream fis;

                try {
                    fis = new FileInputStream(keyFile);
                } catch (FileNotFoundException e) {
                    throw new RepoException(e.getMessage());
                }

                FileChannel ch = fis.getChannel();
                ByteBuffer buf = ByteBuffer.allocate(200);
                try {
                    ch.read(buf);
                    fis.close();
                } catch (IOException e) {
                    throw new RepoException(e.getMessage());
                }

                byte[] seed = buf.array();
                config.setKeySeed(seed);
            } else {
                byte[] seed = Ed25519.createSeed();
                config.setKeySeed(seed);

                try {
                    keyFile.createNewFile();
                } catch (IOException e) {
                    throw new RepoException(e.getMessage());
                }

                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(keyFile);
                } catch (FileNotFoundException e) {
                    throw new RepoException(e.getMessage());
                }

                try {
                    fos.write(seed);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    throw new RepoException(e.getMessage());
                }
            }
        }
    }

    public static String getDefaultDataDir() {

        String dir = System.getenv(TAU_PATH);
        if (dir != null && !"".equals(dir)) {
            return dir;
        }

        return System.getenv("HOME") + "/" + DIR_NAME;
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
