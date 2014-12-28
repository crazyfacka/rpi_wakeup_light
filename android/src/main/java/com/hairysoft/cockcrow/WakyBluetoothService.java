package com.hairysoft.cockcrow;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import com.hairysoft.util.Log;

import com.hairysoft.bt.ConnectThread;

/**
 * This class is bound to perform the Waky device discovery.
 * Upon successful discover, instantiates a thread to connect with it
 */
public class WakyBluetoothService {

    private final static String TAG = "WakyBluetoothService";

    private final String WAKY_NAME = "Waky Waky";

    private final Context c;
    private final Handler mHandler;
    private final BluetoothAdapter mAdapter;
    private final IntentFilter mFilter;

    private BluetoothDevice waky;

    public WakyBluetoothService(Context c, Handler handler) {
        this.c = c;
        mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    }

    public void discoverWaky(boolean force) {
        if(waky == null || force) {
            c.registerReceiver(mReceiver, mFilter);
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
            }
            Log.d(TAG, "Starting discovery...");
            mAdapter.startDiscovery();
        } else {
            connect();
        }
    }

    public void discoverWaky() {
        discoverWaky(false);
    }

    public void holdWaky() {
        mAdapter.cancelDiscovery();
        try {
            c.unregisterReceiver(mReceiver);
        } catch(Exception ex) { }
        Log.d(TAG, "Bubye waky...");
    }

    private void connect() {
        try {
            c.unregisterReceiver(mReceiver);
        } catch(Exception ex) { }
        new ConnectThread(c, mHandler, waky).start();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName() != null && device.getName().equals(WAKY_NAME)) {
                    Log.d(TAG, "Found what appears to be waky!");
                    waky = device;
                    mAdapter.cancelDiscovery();
                    connect();
                } else {
                    Log.d(TAG, "Found device named: " + device.getName());
                }
            }
        }
    };

}
