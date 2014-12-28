package com.hairysoft.bt;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import com.hairysoft.util.Log;
import android.widget.Toast;

import com.hairysoft.cockcrow.R;
import com.hairysoft.message.ClockMessage;
import com.hairysoft.util.Constants;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.UUID;

/**
 * Thread class to handle the bluetooth communication.
 * This thread is only instantiated after successfully discovering the device.
 */
public class ConnectThread extends Thread {

    private final static String TAG = "ConnectThread";

    private final static UUID MY_UUID_SECURE = UUID.fromString("62d1e4b0-848f-11e4-b4a9-0800200c9a66");

    // Providing a static reference for the connection, as it is supposed to exist only one connection at a given time
    private static ConnectThread instance;

    private final Context c;
    private final Handler mHandler;
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;

    private boolean connected;
    private InputStream mmInStream;
    private OutputStream mmOutStream;

    /**
     * Returns the current connection instance
     *
     * @return ConnectThread Static reference for the current connection instance
     */

    public static ConnectThread getInstance() {
        return ConnectThread.instance;
    }

    public static boolean isConnected() {
        return ConnectThread.instance != null && ConnectThread.instance.connected;
    }

    public ConnectThread(Context c, Handler handler, BluetoothDevice device) {
        this.c = c;
        mHandler = handler;
        mmDevice = device;
        BluetoothSocket tmpSocket = null;

        connected = false;

        try {
            // Starts a secure bluetooth connection with the device
            tmpSocket = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
        } catch(IOException ex) {
            Log.e(TAG, "Failed to open socket to device", ex);
        }

        mmSocket = tmpSocket;
        ConnectThread.instance = this;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, "Connecting to Waky");
            mmSocket.connect();
            connected = true;
            // Notifies the UI thread of the connection state change
            mHandler.obtainMessage(Constants.BT_CONNECT_SUCCESS).sendToTarget();
            connectedRoutine();
        } catch(IOException ex) {
            Log.e(TAG, "Problem during connection", ex);
            try {
                mmSocket.close();
            } catch(IOException ex2) {
                Log.e(TAG, "Problem closing socket during connect failure", ex2);
            }
            try {
                // To avoid eating up all the device's CPU when trying to perform multiple reconnects
                Thread.sleep(1000);
            } catch(InterruptedException ex2) { }
            // Notifies the UI thread of the connection state change
            mHandler.obtainMessage(Constants.BT_CONNECT_FAILED).sendToTarget();
        } finally {
            connected = false;
            ConnectThread.instance = null;
        }
    }

    public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);
        } catch(IOException ex) {
            Log.e(TAG, "Error while talking to waky", ex);
            connected = false;
            ConnectThread.instance = null;
        }
    }

    private void connectedRoutine() {
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        try {
            tmpIn = mmSocket.getInputStream();
            tmpOut = mmSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error creating sockets", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;

        try {
            Dispatcher.queueMessage(new ClockMessage().getJSON());
        } catch(JSONException ex) { }

        mHandler.obtainMessage(Constants.UPDATE_ALARM).sendToTarget();

        byte[] buffer = new byte[1024];
        while(true) {
            try {
                // Currently not handling these messages as they're not needed/implemented
                mmInStream.read(buffer);
                Log.d(TAG, "Received [" + new String(buffer) + "]");
                // TODO Send read message to main thread
            } catch(IOException ex) {
                Log.e(TAG, "Connection to waky was lost", ex);
                connected = false;
                ConnectThread.instance = null;
                // Notifies the UI thread of the connection state change
                mHandler.obtainMessage(Constants.BT_DISCONNECT).sendToTarget();
                break;
            }
        }
    }

}
