 package com.example.scannearbydevices;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

 public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "ScanNearByBluetooth";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 2;
    private static final int PERMISSION_REQUEST_ACCESS_LOCATION = 3;

    private Button centralButton ;
    private Button peripheralButton ;


    private BluetoothAdapter bluetoothAdapter;
    private SparseArray<BluetoothDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();

        centralButton = (Button) findViewById(R.id.centralBtn);
        peripheralButton = (Button) findViewById(R.id.peripheralBtn);

        centralButton.setOnClickListener(this);
        peripheralButton.setOnClickListener(this);

        devices = new SparseArray<BluetoothDevice>();


    }

     @Override
     protected void onResume() {
         super.onResume();

         if(bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
             Intent enableBtIntent  = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
             startActivity(enableBtIntent);
             finish();
             return;
         }

         // CHECK FOR ACCESS_FINE_LOCATION
         if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION )!= PackageManager.PERMISSION_GRANTED) {

           if(  ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){

           }
           else{
               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_LOCATION);
           }
         }

         // CHECK FOR ACCESS_COARSE_LOCATION
         if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION )!= PackageManager.PERMISSION_GRANTED) {

           if(  ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){

           }
           else{
               ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
           }
         }

         if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
             Toast.makeText(this, "No LE Support in your device", Toast.LENGTH_SHORT).show();
             finish();
             return;
         }


     }



     @Override
     public void onClick(View v) {
         Intent intent = null;

         switch (v.getId()){
             case R.id.peripheralBtn:
                 intent = new Intent(this, PeripheralActivity.class);
                 break;
             case R.id.centralBtn:
                 intent = new Intent(this, CentralActivity.class);
                 break;

         }
         if(intent != null){
             startActivity(intent);
         }

     }

//


//     ================FOR OPTION MENU CONTAINING SCAN FUNCTIONALITY==============
//
//     private BluetoothGatt bluetoothGatt;
//
//     @Override
//     public boolean onCreateOptionsMenu(Menu menu) {
//         getMenuInflater().inflate(R.menu.menu, menu);
//         for(int i =0; i<devices.size(); i++){
//             BluetoothDevice device = devices.valueAt(i);
//             Log.d(TAG, "onPrepareOptionsMenu: "+device.getName());
//             menu.add(Menu.NONE, devices.keyAt(i), 2, device.getName());
//         }
//
//         return true;
//     }
//
//     @Override
//     public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        switch (item.getItemId()){
//            case R.id.scan:
//                devices.clear();
//                Toast.makeText(this, "Start Scanning", Toast.LENGTH_SHORT).show();
//                startScan();
//                return true;
//            default:
//                BluetoothDevice device = devices.get(item.getItemId());
//                Log.d(TAG, "onOptionsItemSelected: Connecting to "+device.getName());
//
//
//                bluetoothGatt = device.connectGatt(this, true, mGattCallback);
//                Log.d(TAG, "onOptionsItemSelected: Connecting to "+device.getName());
//                return super.onOptionsItemSelected(item);
//
//        }
//     }
//
//     private Runnable mStopRunnable = () ->{stopScan();};
//     private Runnable mStartRunnable = () ->{startScan();};
//
//     private void startScan(){
//         Toast.makeText(this, "Scanning Started", Toast.LENGTH_SHORT).show();;
//         bluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().startScan(leScanCallback);
//         setProgressBarIndeterminateVisibility(true);
//         Handler handler = new Handler();
//         handler.postDelayed(mStopRunnable, 3000);
//     }
//
//     private void stopScan(){
//         bluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner().stopScan(leScanCallback);
//
//     }
//
//     ScanCallback leScanCallback =
//             new ScanCallback() {
//                 @Override
//                 public void onScanResult(int callbackType, ScanResult result) {
//                     super.onScanResult(callbackType, result);
//                     Log.d(TAG, "onScanResult: ");
//                     BluetoothDevice device = result.getDevice();
//                     devices.put(device.hashCode(), device);
//
//                     Toast.makeText(getApplicationContext(), device.toString(), Toast.LENGTH_SHORT).show();
//                     invalidateOptionsMenu();
//                     Log.d(TAG, device.toString());
//                 }
//
//                 @Override
//                 public void onScanFailed(int errorCode) {
//                     super.onScanFailed(errorCode);
//                     Log.d(TAG, "onScanFailed: ");
//                 }
//
//             };
//
//     private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
//         @Override
//         public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//             super.onConnectionStateChange(gatt, status, newState);
//             Log.d(TAG, "onConnectionStateChange: "+status);
//         }
//     };



 }