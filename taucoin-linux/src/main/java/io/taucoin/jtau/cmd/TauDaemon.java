package io.taucoin.jtau.cmd;

import io.taucoin.jtau.config.Config;

public class TauDaemon implements Runnable {

    public TauDaemon(Config config) {
    }

    @Override
    public void run() {
        start();
    }

    public void start() {
        testLoop();
    }

    public void stop() {}

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
