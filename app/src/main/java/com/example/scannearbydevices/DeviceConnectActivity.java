package com.example.scannearbydevices;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.example.scannearbydevices.Constants.BODY_LOCATION_CHARACTERISTIC_UUID;
import static com.example.scannearbydevices.Constants.INSULIN_PUMP_SERVICE_UUID;
import static com.example.scannearbydevices.Constants.SERVER_MSG_FIRST_STATE;
import static com.example.scannearbydevices.Constants.SERVER_MSG_SECOND_STATE;

public class DeviceConnectActivity extends AppCompatActivity implements View.OnClickListener {


    public static final String TAG = "DeviceConnectActivity";


    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";


    private CentralService bluetoothCentralLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> deviceServices;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private String deviceName;
    private String deviceAddress;

    private TextView connectionStatus;
    private TextView connectedDeviceName;
    private TextView serverInfo;
    private Button requestCharacteristicBtn;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_connect);

        deviceServices = new ArrayList<>();
        bluetoothGattCharacteristic = null;

        deviceName = "No Device";

        Intent intent = getIntent();
        if(intent != null){
            deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        }

        connectedDeviceName = (TextView) findViewById(R.id.connected_device_name);
        connectionStatus = (TextView) findViewById(R.id.connection_status);
        serverInfo = (TextView) findViewById(R.id.serverInfo);
        requestCharacteristicBtn = (Button) findViewById(R.id.request_characteristic_btn);

        requestCharacteristicBtn.setOnClickListener(this);

        connectedDeviceName.setText(deviceName);

        Intent gattServiceIntent = new Intent(this, CentralService.class);

        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);


    }



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bluetoothCentralLeService = null;
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.request_characteristic_btn:
                requestReadCharacteristic();
                break;
        }
    }

    private void requestReadCharacteristic(){
        if(bluetoothCentralLeService != null && bluetoothGattCharacteristic != null){
            bluetoothCentralLeService.readCharacteristic(bluetoothGattCharacteristic);
        }
        else {
            Log.d(TAG, "requestReadCharacteristic: UNKNOWN ERROR WHILE READING CHARACTERISTIC");
        }
    }


    private  final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothCentralLeService = ((CentralService.LocalBinder) service).getService();

            if(!bluetoothCentralLeService.initialise()){
                Log.d(TAG, "onServiceConnected: BLUETOOTH NOT INITIALISED");
                finish();
            }

            bluetoothCentralLeService.connect(deviceAddress);

            Toast.makeText(getApplicationContext(), "Device Service Connected", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onServiceConnected: " + bluetoothCentralLeService);
            Log.d(TAG, "onServiceConnected: SERVICE CONNECTED");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: SERVICE DISCONNECTED");

            bluetoothCentralLeService = null;
        }
    };

    private  final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action == null){
                return;
            }

            switch (intent.getAction()){
                case CentralService.ACTION_GATT_CONNECTED:
                    Log.d(TAG, "onReceive: GATT CONNECTED");

                    updateConnectionState("Connected"); // Connected = 1
                    requestCharacteristicBtn.setEnabled(true);
                    break;
                case CentralService.ACTION_GATT_DISCONNECTED:
                    Log.d(TAG, "onReceive: GATT DISCONNECTED");
                    updateConnectionState("Disconnected");  // DisConnected = 2
                    requestCharacteristicBtn.setEnabled(false);
                    break;
                case CentralService.ACTION_SERVICE_DISCOVERED:
                    Log.d(TAG, "onReceive: SERVICE DISCOVERED");
                    setGattServices(bluetoothCentralLeService.getSupportedGattServices());
                    registerCharacteristic();
                    break;
                case CentralService.ACTION_DATA_AVAILABLE:
                    Byte b = 1;
//                    byte[] msg = intent.getByteArrayExtra(CentralService.EXTRA_DATA);
                    String msg1 = intent.getStringExtra(CentralService.EXTRA_DATA);

                    Log.d(TAG, "onReceive: DATA AVAILABLE "+msg1);
                    updateInputFromServer(msg1);
                    break;

            }

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(CentralService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(CentralService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CentralService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(CentralService.ACTION_SERVICE_DISCOVERED);

        return intentFilter;
    }

    private void updateConnectionState(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStatus.setText(String.valueOf(status));
            }
        });
    }

    private  void setGattServices(List<BluetoothGattService> gattServices){
        if(gattServices == null){
            return;
        }

        deviceServices = new ArrayList<>();

        for(BluetoothGattService gattService: gattServices){
            ArrayList<BluetoothGattCharacteristic> characteristic = new ArrayList<>();
            characteristic.addAll(gattService.getCharacteristics());
            deviceServices.add(characteristic);

        }
    }

    private void registerCharacteristic() {

        BluetoothGattCharacteristic characteristic = null;

        if (deviceServices != null) {


            for (ArrayList<BluetoothGattCharacteristic> service : deviceServices) {

                for (BluetoothGattCharacteristic serviceCharacteristic : service) {

                    /* check this characteristic belongs to the Service defined in
                    PeripheralAdvertiseService.buildAdvertiseData() method
                     */
                    if (serviceCharacteristic.getService().getUuid().equals(INSULIN_PUMP_SERVICE_UUID)) {

                        if (serviceCharacteristic.getUuid().equals(BODY_LOCATION_CHARACTERISTIC_UUID)) {
                            characteristic = serviceCharacteristic;
                            bluetoothGattCharacteristic = characteristic;
                        }
                    }
                }
            }



            if (characteristic != null) {
                bluetoothCentralLeService.readCharacteristic(characteristic);
                bluetoothCentralLeService.setCharacteristicNotification(characteristic, true);
            }
        }
    }


    private void updateInputFromServer(String msg){
        String serverData;
//
//        switch (msg){
//            case SERVER_MSG_FIRST_STATE:
//                serverData = "FIRST SERVER";
//                break;
//            case SERVER_MSG_SECOND_STATE:
//                serverData = "SECOND SERVER";
//                break;
//            default:
//                serverData= "No Info Available";
//                break;
//        }

        serverInfo.setText(msg);
        Log.d(TAG, "updateInputFromServer: SERVER INFO FOUND "+msg);

    }


}