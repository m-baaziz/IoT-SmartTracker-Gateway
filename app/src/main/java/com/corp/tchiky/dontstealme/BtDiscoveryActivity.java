package com.corp.tchiky.dontstealme;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by mohamed.b on 30/10/2016.
 */
public class BtDiscoveryActivity extends Activity {
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    TextView btLog;
    ListView btList;
    List<BluetoothDevice> btDeviceList = new ArrayList<BluetoothDevice>();
    ArrayAdapter<String> btNames;

    AdapterView.OnItemClickListener onBtDeviceClickListener;


    @Override
    protected void onCreate(Bundle savedInstaceState) {
        super.onCreate(savedInstaceState);
        setContentView(R.layout.activity_btdiscoveryactivity);
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        btList = (ListView) findViewById(R.id.btList);
        btLog = (TextView) findViewById(R.id.btLog);
        btNames = new ArrayAdapter<String>(this, R.layout.row);

        onBtDeviceClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = btDeviceList.get(position);
                btLog.setText(device.getName() + " : " + device.getAddress());
                Intent returnIntent = new Intent();
                returnIntent.putExtra("device", device);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
        };

        btList.setOnItemClickListener(onBtDeviceClickListener);

        btDeviceList.addAll(mBluetoothAdapter.getBondedDevices());
        updateBtNamesArray();
        btList.setAdapter(btNames);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(broadcastReceiver, filter);
    }

    protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btLog.setText("DEVICE FOUND : " + device.getName());
                for (BluetoothDevice d : btDeviceList) {
                    if (d.getAddress().equals(device.getAddress())) {
                        btLog.setText("Already there");
                        return;
                    }
                }
                btDeviceList.add(device);
                btNames.add(device.getName());
            }
            if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                btLog.setText("Discovering ...");
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            }
            if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                btLog.setText("Discovery finished");
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            }
        }
    };

    protected void updateBtNamesArray(){
        for(BluetoothDevice bt : btDeviceList)
            btNames.add(bt.getName());
    }

    public void onScanClick(View view) {
        if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();
        if (!mBluetoothAdapter.isDiscovering()) {
            btDeviceList.clear();
            btNames.clear();
            btDeviceList.addAll(mBluetoothAdapter.getBondedDevices());
            updateBtNamesArray();
            mBluetoothAdapter.startDiscovery();
        }
    }
}
