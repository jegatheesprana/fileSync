package com.example.filesynchttp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.example.filesynchttp.ConnectedDevice;
import com.example.filesynchttp.ConnectionStatus;
import com.example.filesynchttp.R;

import java.util.List;

public class DeviceItemAdapter extends BaseAdapter {
    Context context;
    List<ConnectedDevice> connectedDevices;
    LayoutInflater inflater;

    public DeviceItemAdapter(Context context, List<ConnectedDevice> connectedDevices) {
        this.context = context;
        this.connectedDevices = connectedDevices;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return connectedDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return connectedDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.activity_device_list_item, null);
        ConnectedDevice connectedDevice = connectedDevices.get(i);

        CheckBox cb = convertView.findViewById(R.id.deviceListSelected);
        cb.setChecked(connectedDevice.selectedForPairing);
        cb.setText(connectedDevice.deviceName);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                connectedDevice.selectedForPairing = b;
            }
        });

        TextView tv = convertView.findViewById(R.id.deviceListStatus);
        String status = "";
        switch (connectedDevice.status) {
            case OUR_REQUEST_ACCEPTED:
                status = "Sync";
                break;
            case OUR_REQUEST_PENDING:
                status = "waiting";
                break;
            case OUR_REQUEST_REJECTED:
                status = "Refused";
                break;
            case PEER_REQUEST_ACCEPTED:
                status = "Sync";
                break;
            case PEER_REQUEST_PENDING:
                status = "waiting";
                break;
            case PEER_REQUEST_REFUSED:
                status = "You refused";
                break;
            default:
                status= "";
                break;
        }
        tv.setText(status);

        return convertView;
    }
}
