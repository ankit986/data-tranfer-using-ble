package com.example.scannearbydevices;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CentralActivity extends AppCompatActivity implements View.OnClickListener, DeviceAdapter.DevicesAdapterListener{

   private static final long SCAN_PERIOD = 30000;
   private static final String TAG = "CentralActivity";

   private RecyclerView deviceRecycler;
   private DeviceAdapter deviceAdapter;
    private ScanCallback scanCallback;
    private Button scanButton;

   private Handler handler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        scanButton = (Button ) findViewById(R.id.scanAgain);
        scanButton.setOnClickListener(this);

        deviceRecycler = (RecyclerView) findViewById(R.id.recyclerView);
        deviceRecycler.setHasFixedSize(true);
        deviceRecycler.setLayoutManager(new LinearLayoutManager(this));

        deviceAdapter = new DeviceAdapter(this);
        deviceRecycler.setAdapter(deviceAdapter);

        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.scanAgain:
                startBLEScan();
                break;
        }
    }

    @Override
    public void onDeviceItemClick(String deviceName, String deviceAddress) {
            Intent intent = new Intent(this, DeviceConnectActivity.class);
            Log.d(TAG, "onDeviceItemClick: Clicked "+deviceName);
            intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_NAME, deviceName);
            intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_ADDRESS, deviceAddress);

            startActivity(intent);
    }

    private BluetoothAdapter getBluetoothAdapter() {

        BluetoothAdapter bluetoothAdapter;
        BluetoothManager bluetoothService = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));

        if (bluetoothService != null) {

            bluetoothAdapter = bluetoothService.getAdapter();

            if (bluetoothAdapter != null) {

                if (bluetoothAdapter.isEnabled()) {
                   
                    return bluetoothAdapter;
                }
            }
        }

        return null;
    }

    private void startBLEScan() {

        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();

        /*
        better to request each time as BluetoothAdapter state might change (connection lost, etc...)
         */
        if (bluetoothAdapter != null) {

            BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

            if (bluetoothLeScanner != null) {

                if (scanCallback == null) {
                    Log.d(TAG, "Starting Scanning");

                    // Will stop the scanning after a set time.
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bluetoothLeScanner.stopScan( scanCallback);
                            scanCallback = null;
                        }
                    }, SCAN_PERIOD);

                    // Kick off a new scan.
                    scanCallback = new scanCallBackClass();
                    bluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), scanCallback);


                } else {
                    Log.d(TAG, "startBLEScan: UNKNOWN ERROR WHILE STARTING BLE");                }

                return;
            }
        }

    }

    private class scanCallBackClass extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult result : results)
                 deviceAdapter.add(result, true);
            logResults(results);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            deviceAdapter.add(result, true);
            logResults(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed: SCANNING FAILED");
        }


        private void logResults(List<ScanResult> results) {
            if (results != null) {
                for (ScanResult result : results) {
                    logResults(result);
                }
            }
        }

        private void logResults(ScanResult result) {
            if (result != null) {
                BluetoothDevice device = result.getDevice();
                if (device != null) {
                    Log.v(TAG, device.getName() + " " + device.getAddress());
                    return;
                }
            }
            Log.e(TAG, "error SampleScanCallback");
        }
    }


//    private ScanCallback scanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            super.onScanResult(callbackType, result);
//            deviceAdapter.add(result, true);
//            Log.d(TAG, "onScanResult: DEVICEFOUND "+result.getDevice().getName());
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            super.onBatchScanResults(results);
//           for(ScanResult result : results) {
//               deviceAdapter.add(result, true);
//
//               Log.d(TAG, "onBatchScanResult: DEVICEFOUND " + result.getDevice().getName());
//           }
//
//        }
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            super.onScanFailed(errorCode);
//            Log.d(TAG, "onScanFailed: ERRORFOUND "+errorCode);
//
//        }
//    };




//    JUST FOR TRYING

    private List<ScanFilter> buildScanFilters() {

        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        // Comment out the below line to see all BLE devices around you
        //builder.setServiceUuid(Constants.SERVICE_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }


    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }
//
//    private void startBLEScan(){
//        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
//
//        if(bluetoothAdapter != null){
//            BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
//
//            if(bluetoothLeScanner != null){
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            bluetoothLeScanner.stopScan(scanCallback);
//                            deviceAdapter.notifyDataSetChanged();
//                        }
//                    }, SCAN_PERIOD);
//
//
//                    scanCallback = new scanCallBackClass();
//
//                    bluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), scanCallback);
//
//                Toast.makeText(this, "Scanning for 5 minutes", Toast.LENGTH_LONG).show();
//            }
//            else{
//                Toast.makeText(this, "Error with scanning", Toast.LENGTH_LONG).show();
//            }
//        }
//
//    }

}