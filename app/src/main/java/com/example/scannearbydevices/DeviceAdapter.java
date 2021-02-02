package com.example.scannearbydevices;


import android.bluetooth.le.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private ArrayList<ScanResult> arrayList;
    private DevicesAdapterListener  devicesAdapterListener;

    public DeviceAdapter(DevicesAdapterListener listener){
        arrayList = new ArrayList<>();
        devicesAdapterListener = listener;
    }

    public interface DevicesAdapterListener {
        void onDeviceItemClick(String deviceName, String deviceAddress);
    }

    @NonNull
    @Override
    public DeviceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult scanResult = arrayList.get(position);
        final String deviceNameReceived = scanResult.getDevice().getName();
        final String deviceAddressReceived = scanResult.getDevice().getAddress();

        holder.deviceName.setText(deviceNameReceived);
        holder.deviceAddress.setText(deviceAddressReceived);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(devicesAdapterListener!=null){
                    devicesAdapterListener.onDeviceItemClick(deviceNameReceived, deviceAddressReceived);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();

    }


    public void add(ScanResult scanResult, boolean notify){


        if(scanResult != null){
            int position =-1;

            String currentAddress = scanResult.getDevice().getAddress();
            for(int i=0; i<arrayList.size(); i++){
                if(arrayList.get(i).getDevice().getAddress().equals(currentAddress)){
                    position = i;
                    break;
                }
            }

            if(position>=0){
                arrayList.set(position, scanResult);
            }
            else{
                arrayList.add(scanResult);

            }

            if(notify)
                notifyDataSetChanged();
        }
    }

    public static  class ViewHolder extends  RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.deviceName);
            deviceAddress = (TextView) itemView.findViewById(R.id.deviceAddress);
        }
    }
}
