package io.taucoin.jtau.cmd;

import io.taucoin.controller.TauController;
import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.listener.TauListener;
import io.taucoin.jtau.config.Config;
import io.taucoin.jtau.db.RocksDatabaseFactory;
import io.taucoin.jtau.event.TauListenerImpl;
import io.taucoin.jtau.rpc.JsonRpcServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class TauDaemon implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger("tau-daemon");

    private final Config config;

    // Tau Controller
    private TauController tauController;

    // key-value database factory
    private KeyValueDataBaseFactory dbFactory;

    // Tau listener that is the event handler for jtau app
    private TauListener listener;

    // rpc server
    private JsonRpcServer jsonRpcServer;
    private Thread rpcServerThread;

    // blocks this daemon thread
    private final Object signal = new Object();
    private boolean stopped = false;

    // for test
    private AtomicBoolean testRunning = new AtomicBoolean(true);

    public TauDaemon(Config config) {

        this.config = config;

        // new database factory
        this.dbFactory = new RocksDatabaseFactory();

        // new TauController through which blockchain components can be accessed.
        this.tauController = new TauController(config.getDataDir(),
                config.getKeySeed(), this.dbFactory);

        // new TauListner and register it into tau controller.
        this.listener = new TauListenerImpl(this.tauController);
        this.tauController.registerListener(this.listener);

        // new rpc server
        this.jsonRpcServer = new JsonRpcServer(this.tauController);
        this.rpcServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TauDaemon.this.jsonRpcServer.start(TauDaemon.this.config.getRPCPort());
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("start rpc server fatal:" + e);
                    System.exit(-3);
                }
            }
        });
    }

    @Override
    public void run() {
        start();
    }

    public void start() {

        logger.info("starting tau daemon...");
        logger.info("Config:" + "\n"  + this.config);

        // start tau blockchain and torrent dht engine
        startTau();

        // start rpc server
        startRpcServer();

        // wait for stop signal
        logger.info("waiting for stopping signal");
        synchronized (signal) {
            while (!stopped) {
                try {
                    signal.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }
        }

        logger.info("tau daemon is exiting...");

        //testLoop();
    }

    public void stop() {

        logger.info("stopping tau daemon...");

        // stop rpc server
        stopRpcServer();

        // stop tau blockchain and torrent dht engine
        stopTau();

        // notify this daemon exiting
        synchronized (signal) {
            stopped = true;
            signal.notify();
        }

        //stopTestLoop();
    }

    private void startTau() {
        this.tauController.start(this.config.getSessionsQuota(),
               this.config.getInterfacesQuota());
    }

    private void startRpcServer() {
        this.rpcServerThread.start();
    }

    private void stopTau() {
        if (this.tauController != null) {
            this.tauController.stop();
            this.tauController = null;
        }
    }

    private void stopRpcServer() {
        if (this.jsonRpcServer != null) {
            this.jsonRpcServer.stop();
            this.jsonRpcServer = null;
            if (this.rpcServerThread != null) {
                this.rpcServerThread.interrupt();
                try {
                    this.rpcServerThread.join();
                } catch (InterruptedException e) {}
                this.rpcServerThread = null;
            }
        }
    }

    private void testLoop() {

        while (testRunning.get()) {
            System.out.println("Hi, JTau...");

            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
                return;
            }
        }
    }

    private void stopTestLoop() {
        testRunning.set(false);
    }
}
