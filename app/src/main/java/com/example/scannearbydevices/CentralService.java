package com.example.scannearbydevices;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import static com.example.scannearbydevices.Constants.BODY_LOCATION_CHARACTERISTIC_UUID;

public class CentralService extends Service {
    private static  final String TAG = "CentralService";


    private   final static int STATE_DISCONNECTED = 0;
    private   final static int STATE_CONNECTING = 1;
    private   final static int STATE_CONNECTED = 2;


    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_SERVICE_DISCOVERED = "ACTION_SERVICE_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "EXTRA_DATA";


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;

    private  final IBinder binder = new LocalBinder();
//    Initially Device Disconnected
    private int connectionState = STATE_DISCONNECTED;



    @Override
    public IBinder onBind(Intent intent) {
        return  binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }


        return super.onUnbind(intent);
    }


    public class LocalBinder extends Binder {
        CentralService getService(){
            return CentralService.this;
        }
    }


    private  final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String intentAction;

            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);

                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + bluetoothGatt.discoverServices());
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;

                broadcastUpdate(intentAction);
                Log.i(TAG, "Disconnected from the GATT server.");

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_SERVICE_DISCOVERED);
            }
            else{
                Log.d(TAG, "onServicesDiscovered: GATT FAILURE ");
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                Log.d(TAG, "onCharacteristicRead: DATA AVAILABLE");
            }
            else{
                Log.d(TAG, " onCharacteristicRead: GATT FAILURE ");
            }
        }
    };


    public void broadcastUpdate(String action){
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);
        if(BODY_LOCATION_CHARACTERISTIC_UUID.equals(characteristic.getUuid())){
//            TODO check the docs for format and other things.

            int flag = characteristic.getProperties();
            int format = -1;

            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "data format UINT16."+ format);
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "data format UINT18.");
            }

            byte[] msg = characteristic.getValue();
            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data) {
                    Log.d(TAG, "broadcastUpdate: DATA "+byteChar);
                    stringBuilder.append(byteChar);
                }
                intent.putExtra(EXTRA_DATA, stringBuilder.toString());
            Log.d(TAG, "broadcastUpdate: RECCCC"+stringBuilder);
            }
//            String msg = characteristic.getStringValue(format);
//            Log.d(TAG, String.format("message:  RECIEVED %d"+msg));

//            intent.putExtra(EXTRA_DATA, msg);
        }
        else{
            intent.putExtra(EXTRA_DATA, "Sending Data From CS for other than BodyLocationCharacteristic");

        }
        sendBroadcast(intent);
    }


    public boolean initialise(){
        if(bluetoothManager == null){
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if(bluetoothManager == null){
                Log.d(TAG, "initialise: CAN'T INITIALIZE THE BLUETOOTH BECAUSE BLUETOOTHMANAGER IS NULL");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null){
            Log.d(TAG, "initialise: CANT INITIALIZE THE BLUETOOTH BECAUSE BLUETOOTHADAPTER IS NULL");
        }


        return  true;
    }

    public boolean connect(final String address){
        if(bluetoothAdapter == null){
            Log.d(TAG, "connect: BLUETOOTH ADAPTER IS NULL, CAN'T CONNECT");
            return false;

        }


        if(bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress) && bluetoothGatt!= null){
            if(bluetoothGatt.connect()){
                connectionState = STATE_CONNECTED;
                Log.d(TAG, "connect: CONNECTED");
                return true;
            }
            else
                return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if(device == null){
            Log.d(TAG, "connect: DEVICE NOT FOUND!");
                return false;
        }

        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTED;

        return true;

    }

    public void disconnected(){
        if(bluetoothAdapter == null || bluetoothGatt == null){
            Log.d(TAG, "disconnected: BLUETOOTH NOT INITIALISED");
            return;
        }

        bluetoothGatt.disconnect();
    }


    public void readCharacteristic(BluetoothGattCharacteristic characteristic){
        if(bluetoothAdapter == null || bluetoothGatt == null){
            Log.d(TAG, "readCharacteristic: BLUETOOTH NOT INITIALISED");
            return;
        }

        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {

        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) {
            return null;
        }

        return bluetoothGatt.getServices();
    }

}