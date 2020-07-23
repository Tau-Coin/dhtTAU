package io.taucoin.jtau.cmd;

import io.taucoin.controller.TauController;
import io.taucoin.db.KeyValueDataBaseFactory;
import io.taucoin.listener.TauListener;
import io.taucoin.torrent.SessionSettings;
import io.taucoin.jtau.config.Config;
import io.taucoin.jtau.db.RocksDatabaseFactory;
import io.taucoin.jtau.event.TauListenerImpl;
import io.taucoin.jtau.rpc.JsonRpcServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // torrent session settings
    private SessionSettings sessionSettings;

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

        // new torrent session settings
        this.sessionSettings = new SessionSettings.Builder()
                .setDHTMaxItems(SessionSettings.TauDHTMaxItems)
                .build();
    }

    @Override
    public void run() {
        start();
    }

    public synchronized void start() {

        // start tau blockchain and torrent dht engine
        //startTau();

        // start rpc server
        //startRpcServer();

        testLoop();
    }

    public synchronized void stop() {
        // stop rpc server
        //stopRpcServer()

        // stop tau blockchain and torrent dht engine
        //stopTau()
    }

    private void startTau() {
        this.tauController.start(this.sessionSettings);
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

        while (true) {
            System.out.println("Hi, JTau...");

            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
                return;
            }
        }
    }
}
