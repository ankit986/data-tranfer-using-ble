package com.example.scannearbydevices;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class PeripheralAdvertisingService extends Service {
    private  static  final String TAG = "PeripheralAdvertService";

    public static boolean running = false;

    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private AdvertiseCallback advertiseCallback;
    private Handler handler;
    private Runnable timeoutRunnable;

    private long TIMEOUT = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    @Override
    public void onCreate() {
        super.onCreate();
        running= true;
        init();
        startAdvertising();

//        STOP THIS SERVICE AFTER 10 MINUTES OF SCANNING
        handler = new Handler();
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        };
        handler.postDelayed(timeoutRunnable, TIMEOUT);

    }

//    GETTING REFERENCE TO SYSTEM BLUETOOTH OBJECT
    private void init(){
        if(bluetoothLeAdvertiser == null){
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

            if(bluetoothManager != null){
                BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
                if(bluetoothAdapter != null){
                    bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
                }
            }
        }
    }

    private void startAdvertising(){
        if(advertiseCallback == null){
            AdvertiseData data = buildAdvertiseData();
            AdvertiseSettings settings = buildAdvertiseSettings();
            advertiseCallback = new SampleAdvertiseCallback();

            if(bluetoothLeAdvertiser != null){
                bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
            }
        }
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }


    private void stopAdvertising(){
        if(bluetoothLeAdvertiser != null){
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            advertiseCallback = null;
        }
    }

    private AdvertiseData buildAdvertiseData(){
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        dataBuilder.addServiceUuid(ParcelUuid.fromString(Constants.INSULIN_PUMP_SERVICE_UUID.toString()));
        String addingData = "Temporary Data";
//        dataBuilder.addServiceData(Constants.SERVICE_UUID, addingData.getBytes());
        dataBuilder.setIncludeDeviceName(true);

        return dataBuilder.build();
    }




    @Override
    public IBinder onBind(Intent intent) {
        return  null;// no need for binding because this will be a started service
    }

    private class SampleAdvertiseCallback extends AdvertiseCallback {
        private  static  final String TAG = "SampleAdvertiseCallback";

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(TAG, "Advertising failed");
            stopSelf();
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }
    }

}