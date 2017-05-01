package com.corp.tchiky.dontstealme;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.format.DateFormat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class TrackingActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BackendSocketThread socketThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        socketThread = new BackendSocketThread();
        socketThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketThread.cancel();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void changeLocation(final float lat, final float lng, final String collectionTimestamp) {
        Runnable sendDataThread = new Runnable() {
            @Override
            public void run() {
                LatLng deviceLocation = new LatLng(lat, lng);
                mMap.addMarker(new MarkerOptions().position(deviceLocation).title(collectionTimestamp));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(deviceLocation));
            }
        };
        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(sendDataThread);
    }

    private class BackendSocketThread extends Thread {

        Socket socket;

        @Override
        public void run() {
            try {
                IO.Options opts = new IO.Options();
                opts.query = "username=mimo&password=mypassword";
                opts.timeout = 5000;
                socket = IO.socket("http://"+MainActivity.SERVER_ADDRESS, opts);
                socket.connect();
                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        System.out.println("Socket connection");
                    }
                });
                socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        System.out.println("Socket disconnection");
                    }
                });
                socket.on("newLocation", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject obj = (JSONObject) args[0];
                            System.out.println("LATITUDE     :     " + obj.getString("latitude"));
                            float lat = Float.parseFloat(obj.getString("latitude"));
                            float lng = Float.parseFloat(obj.getString("longitude"));
                            String collectionTimestamp = obj.getString("collectedAt");
                            changeLocation(lat, lng, collectionTimestamp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
