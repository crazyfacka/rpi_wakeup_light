package com.hairysoft.bt;

import com.hairysoft.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This thread handles the sending of messages from the application to the device.
 * If there is no current active connections, it stores them internally for sending them out later
 */
public class Dispatcher extends Thread {

    private final static String TAG = "Dispatcher";
    private final static int RETRY_COUNT = 3;

    // Providing a static reference for the dispatcher, as it is supposed to exist only one at a given time
    private static Dispatcher instance;

    // Using this type of queue as it ditches the need to use 'wait' and 'notify', while proving to be more efficient
    private final BlockingQueue<String> queue;

    public static void init() {
        instance = new Dispatcher();
        instance.start();
    }

    public static void queueMessage(String msg) {
        try {
            Log.d(TAG, "Queueing: " + msg);
            instance.queue.offer(msg, 100, TimeUnit.MILLISECONDS);
        } catch(InterruptedException ex) {
            Log.e(TAG, "Message queue timed out: " + msg);
        }
    }

    private Dispatcher() {
        queue = new LinkedBlockingQueue<>();
    }

    private void sendMessage(String msg) throws Exception {
        while(!ConnectThread.isConnected()) {
            try {
                Thread.sleep(500);
            } catch(Exception ex) { }
        }
        ConnectThread.getInstance().write(msg.getBytes());
    }

    @Override
    public void run() {
        int retries = 0;
        String msg = null, retry = null;

        while(true) {
            try {

                if(retry != null) {
                    sendMessage(retry);
                    retries = 0;
                    retry = null;
                }

                msg = queue.take();
                sendMessage(msg);

            } catch(Exception ex) {

                Log.e(TAG, "Sending exception", ex);

                if(retries++ < RETRY_COUNT) {
                    retry = retry == null ? msg : retry;
                } else {
                    retries = 0;
                    retry = null;
                }

                try {
                    Thread.sleep(500);
                } catch(InterruptedException ex2) { }

            }
        }
    }

}
