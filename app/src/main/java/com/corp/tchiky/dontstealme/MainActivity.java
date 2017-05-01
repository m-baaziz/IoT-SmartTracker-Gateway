package com.corp.tchiky.dontstealme;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_BT_DEVICE = 2;
    public static final int BUFFER_SIZE = 1024;
    public static final long TIMEOUT = 5000;
    public static final String SERVER_ADDRESS = "34.194.168.187";
    protected BluetoothAdapter mBluetoothAdapter;
    TextView logTxt;
    TextView logTxtBlue;
    Button pairBtn;
    Button trackBtn;
    ConstantConnectionThread constantConnectionThread = null;
    RequestQueue netQueue = null;

    UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    protected final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (newState == BluetoothAdapter.STATE_OFF) {
                    if (constantConnectionThread != null) {
                        try {
                            constantConnectionThread.cancel();
                        } catch (Exception e) { }
                    }
                    logTxt.setText("Bluetooth is off");
                    pairBtn.setEnabled(false);
                    exitDialog("Bluetooth is required");
                } else {
                    logTxt.setText("Bluetooth is on");
                    pairBtn.setEnabled(true);
                }
            }
        }
    };

    protected void startBtDiscoveryActivity() {
        startActivityForResult(new Intent(this, BtDiscoveryActivity.class), REQUEST_BT_DEVICE);
    }

    protected void startTrackingActivity(){
        startActivity(new Intent(this, TrackingActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        netQueue = Volley.newRequestQueue(this);

        logTxt = (TextView) findViewById(R.id.log);
        logTxtBlue = (TextView) findViewById(R.id.logBlue);
        pairBtn = (Button) findViewById(R.id.pairBtn);
        pairBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logTxt.setText("Pair clicked !");
                startBtDiscoveryActivity();
            }
        });

        trackBtn = (Button) findViewById(R.id.trackBtn);
        trackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logTxt.setText("Track clicked !");
                startTrackingActivity();
            }
        });

        logTxt.setText("Ohayo Gosaymasuuu");

        // Set up bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            exitDialog("Your device doesn't support bluetooth");
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                logTxt.setText("Bluetooth is off");
                pairBtn.setEnabled(false);
                Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBt, REQUEST_ENABLE_BT);
            }

            IntentFilter broadcastReceiverFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(broadcastReceiver, broadcastReceiverFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (Exception e) { }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter broadcastReceiverFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastReceiver, broadcastReceiverFilter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) exitDialog("Bluetooth is required");
            if (resultCode == RESULT_OK) {
                logTxt.setText("Bluetooth is on");
                pairBtn.setEnabled(true);
            }
        }
        if (requestCode == REQUEST_BT_DEVICE) {
            if (resultCode == RESULT_OK) {
                BluetoothDevice device = (BluetoothDevice) data.getParcelableExtra("device");
                logTxt.setText("DEVICE : " + device.getName() + " - " + device.getAddress());
                constantConnectionThread = new ConstantConnectionThread(device);
                constantConnectionThread.start();
            }
        }
    }

    public void exitDialog(String msg) {
        new AlertDialog.Builder(this)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    private void updateUIFromThread(final String msg) {
        Runnable sendDataThread = new Runnable() {
            @Override
            public void run() {
                logTxt.setText(msg);
            }
        };
        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(sendDataThread);
    }

    private void updateUIFromThreadBlue(final String msg) {
        Runnable sendDataThread = new Runnable() {
            @Override
            public void run() {
                logTxtBlue.setText(msg);
            }
        };
        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(sendDataThread);
    }

    private class ConstantConnectionThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        BtListenerThread BtListenerThread;

        public ConstantConnectionThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            connectToSocket();
            while (true) {
                SystemClock.sleep(TIMEOUT);
                updateUIFromThread("Dans Loop");
                if (!mBluetoothAdapter.isEnabled() || !mmSocket.isConnected()) {
                    updateUIFromThread("Socket not connected");
                    BluetoothSocket tmp = null;
                    try {
                        updateUIFromThread("AVANT Connection initiated");
                        tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        updateUIFromThread("Connection initiated");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mmSocket = tmp;
                    connectToSocket();
                }
            }
        }

        private void connectToSocket() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;

            try {
                mmSocket.connect();
            } catch (IOException connectException) {
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)

            BtListenerThread = new BtListenerThread(mmSocket);
            BtListenerThread.start();
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class BtListenerThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BtListenerThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[BUFFER_SIZE];  // buffer store for the stream
            int bytes; // bytes returned from read()
            String sequenceBuffer = "";
            JSONObject locations = new JSONObject();

            // starts by saying hello to the device
            write("A".getBytes());
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // logs what we receive
                    updateUIFromThread(readMessage);
                    // we respond to Ping with Ack
                    if (readMessage.equals("P")) write("A".getBytes());
                    else sequenceBuffer += readMessage; //  else, we accumulate the content in a buffer

                    // whenever the buffer contains { and }
                    if (sequenceBuffer.contains("{") && sequenceBuffer.contains("}")) {
                        // we take the content of the buffer
                        String tmp = sequenceBuffer;
                        int startIndex = tmp.indexOf('{');
                        int lastIndex = tmp.lastIndexOf('}');
                        // we cut the parts that are before the first { and the last }
                        tmp = tmp.substring(startIndex, lastIndex+1);
                        // we replace }{ with },{
                        tmp = tmp.replace("}{", "},{");
                        // and surround the whole with array brackets
                        if (!tmp.isEmpty()) {
                            try {
                                JSONArray tmpJson = new JSONArray("[" + tmp + "]");
                                sequenceBuffer = sequenceBuffer.substring(lastIndex+1);
                                for (int i = 0; i<tmpJson.length(); i++) {
                                    JSONObject location = tmpJson.getJSONObject(i);
                                    String senderIp = location.get("senderIp").toString();
                                    String scanId = location.get("scanId").toString();
                                    if (!locations.has(senderIp)) locations.put(senderIp, new JSONObject());
                                    if (!locations.getJSONObject(senderIp).has(scanId)) {
                                        locations.getJSONObject(senderIp).put(scanId, new JSONObject("{\"scan\": [], isComplete: \"0\"}"));
                                    }
                                    JSONObject scanJson = locations.getJSONObject(senderIp).getJSONObject(scanId);
                                    JSONArray currentlyAddedScans = scanJson.getJSONArray("scan");
                                    JSONObject locationToSend = new JSONObject("{\"ssid\":\"" + location.getString("ssid") + "\","
                                            + "\"mac\":\"" + location.getString("mac") + "\","
                                            + "\"signal_level\":\"" + location.getString("signal_level") + "\"}");
                                    currentlyAddedScans.put(locationToSend);
                                    if (location.getString("moreSequence").equals("0")) scanJson.put("isComplete", "1");
                                }
                                Iterator locationIterator = locations.keys();
                                while (locationIterator.hasNext()) {
                                    String ipKey = (String) locationIterator.next();
                                    JSONObject scans = locations.getJSONObject(ipKey);
                                    ArrayList<String> processedScans = new ArrayList<>();
                                    Iterator scanIterator = scans.keys();
                                    while (scanIterator.hasNext()) {
                                        String scanIdKey = (String) scanIterator.next();
                                        JSONObject scanJson = scans.getJSONObject(scanIdKey);
                                        String scanCompleted = scanJson.getString("isComplete");
                                        if (scanCompleted.equals("1")) {
                                            // send post request
                                            JSONArray tmpLocations = scanJson.getJSONArray("scan");

                                            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                                            long collectedAt = cal.getTimeInMillis();

                                            JSONObject jsonToSend = new JSONObject();
                                            jsonToSend.put("collectedAt", Float.toString(collectedAt));
                                            jsonToSend.put("deviceIpv4", ipKey);
                                            jsonToSend.put("scanResult", tmpLocations);

                                            System.out.println(jsonToSend);

                                            try {

                                                String url ="http://"+MainActivity.SERVER_ADDRESS+"/api/locations";

                                                // Request a string response from the provided URL.
                                                JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.POST,url,jsonToSend,
                                                        new Response.Listener<JSONObject>() {
                                                            @Override
                                                            public void onResponse(JSONObject response) {
                                                                // Display the first 500 characters of the response string.
                                                                return;
                                                            }
                                                        }, new Response.ErrorListener() {
                                                    @Override
                                                    public void onErrorResponse(VolleyError error) {
                                                        return;
                                                    }
                                                }) {
                                                    @Override
                                                    public Map<String, String> getHeaders() {
                                                        Map<String,String> headers = new HashMap<String, String>();
                                                        String creds = String.format("%s:%s","mimo","mypassword");
                                                        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
                                                        headers.put("Authorization", auth);
                                                        headers.put("Content-Type", "application/json");
                                                        return headers;
                                                    }
                                                };
                                                // Add the request to the RequestQueue.
                                                System.out.println(stringRequest.toString());
                                                netQueue.add(stringRequest);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }

                                            processedScans.add(scanIdKey);
                                        }
                                    }

                                    for (String scanToDelete : processedScans) {
                                        locations.getJSONObject(ipKey).remove(scanToDelete);
                                    }
                                    processedScans.clear();
                                }
                            } catch (Exception e ) {
                                e.printStackTrace();
                            }
                        }
                    }

                    updateUIFromThread(readMessage);
                    updateUIFromThreadBlue(sequenceBuffer);
                    buffer = new byte[BUFFER_SIZE];
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
    }

}

