package com.example.scannearbydevices;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;

import static com.example.scannearbydevices.Constants.INSULIN_PUMP_SERVICE_UUID;
import static com.example.scannearbydevices.Constants.SERVER_MSG_FIRST_STATE;
import static com.example.scannearbydevices.Constants.SERVER_MSG_SECOND_STATE;
import static com.example.scannearbydevices.Constants.BODY_LOCATION_CHARACTERISTIC_UUID;


public class PeripheralActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PeripheralActivity";

    private Button notifyBtn;
    private Switch enableAdvertisementSwitch;
    private RadioGroup characteristicValueSwitch;
    private EditText editTextInsulinLevel;

    private BluetoothManager bluetoothManager;
    private BluetoothGattServer bluetoothGattServer;
    private HashSet<BluetoothDevice> bluetoothDevices;

    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral);

        notifyBtn = (Button )  findViewById(R.id.notifyBtn);
        enableAdvertisementSwitch = (Switch) findViewById(R.id.advertisementSwitch);
        characteristicValueSwitch = (RadioGroup) findViewById(R.id.dataRadioGroup);
        editTextInsulinLevel = (EditText) findViewById(R.id.editTextInsulinLevel);

        notifyBtn.setOnClickListener(this);
        enableAdvertisementSwitch.setOnClickListener(this);

//        characteristicValueSwitch.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                setCharacterstic(checkedId);
//            }
//        });

        setGattServer();
        setBluetoothService();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.advertisementSwitch:
                Switch switchToggle = (Switch) v;
                if(switchToggle.isChecked())
                    startAdvertising();
                else
                    stopAdvertising();
                break;
            case R.id.notifyBtn:
                notifyCharactersticChanged();
                setCharacterstic();
                break;
        }
    }


    private void setGattServer(){
        bluetoothDevices = new HashSet<>();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if(bluetoothManager != null) {
            bluetoothGattServer = bluetoothManager.openGattServer(this, bluetoothGattServerCallback);
            
            if(bluetoothGattServer!=null){
                Log.d(TAG, "setGattServer: GATT SERVER STARTED");
                Toast.makeText(this, "Gatt Server Started", Toast.LENGTH_LONG).show();
            }
        }
        else
            Log.d(TAG, "Error in Setting Gatt Server");
    }

    private void setBluetoothService(){
        bluetoothGattService = new BluetoothGattService(INSULIN_PUMP_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        bluetoothGattCharacteristic = new BluetoothGattCharacteristic(BODY_LOCATION_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY|BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ );
        setCharacterstic();

        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);

        // Add Service to the Server/Peripheral
        if(bluetoothGattServer!=null){
            bluetoothGattServer.addService(bluetoothGattService);

            Log.d(TAG, "setBluetoothService: INSULIN SERVICE STARTED");
            Toast.makeText(this, "Insulin Service Added to Gatt Server", Toast.LENGTH_LONG).show();
        }

    }

    public void setCharacterstic() {
        setCharacterstic(R.id.radio1);
    }

    public void setCharacterstic(int checkedId) {
        int value = checkedId == R.id.radio1 ? SERVER_MSG_FIRST_STATE : SERVER_MSG_SECOND_STATE;
//        bluetoothGattCharacteristic.setValue(  new byte[]{(byte) value});
        Log.d(TAG, editTextInsulinLevel.getText().toString());
        int dataToTransfer = Integer.parseInt(editTextInsulinLevel.getText().toString());
        bluetoothGattCharacteristic.setValue(new byte[]{(byte) dataToTransfer});
    }

    private void startAdvertising(){
        startService(new Intent(this, PeripheralAdvertisingService.class));
    }

    private void stopAdvertising(){
        stopService(new Intent(this, PeripheralAdvertisingService.class));
        enableAdvertisementSwitch.setChecked(false);
    }

    private void notifyCharactersticChanged(){
        for(BluetoothDevice device : bluetoothDevices){
            if(bluetoothGattServer !=null)
                    bluetoothGattServer.notifyCharacteristicChanged(device, bluetoothGattCharacteristic, true);
        }
    }

    private final BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);


            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    bluetoothDevices.add(device);
                    Toast.makeText(getApplicationContext(), "A new device connected "+device.getName(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onConnectionStateChange: DEVICE CONNECTED " + device.getName() + " " + device.getAddress());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    bluetoothDevices.remove(device);
                    Toast.makeText(getApplicationContext(), "A new device disconnected "+device.getName(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onConnectionStateChange: DEVICE DISCONNECTED " + device.getName() + " " + device.getAddress());
                }
            } else {
                bluetoothDevices.remove(device);
                Log.d(TAG, "onConnectionStateChange: ERROR WHILE CONNECTING TO DEVICE " + status);

            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if(bluetoothGattServer == null)
                    return;

            Log.d(TAG, "onCharacteristicReadRequest: DEVICE READ THE CHARACTERISTIC - VALUE "+ Arrays.toString(characteristic.getValue()));

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            Log.d(TAG, "onCharacteristicWriteRequest: CHARACTERISTIC WRITE REQUEST - "+ Arrays.toString(value));

            bluetoothGattCharacteristic.setValue(value);

            if(responseNeeded)
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);

            Log.v(TAG, "onNotificationSent : NOTIFICATION SENT. Status: " + status);
        }
    };

}