package network.datahop.wifiawaredemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

import datahop.AdvertisementNotifier;
import datahop.DiscoveryNotifier;
import network.datahop.wifiaware.WifiAwareAdvertising;
import network.datahop.wifiaware.WifiAwareDiscovery;


public class MainActivity extends AppCompatActivity implements AdvertisementNotifier, DiscoveryNotifier {

    private static final int          MY_PERMISSION_NEARBY_DEVICES_REQUEST_CODE = 88;

    private Button startAdvButton,startDiscButton,stopButton,refreshButton;

    private TextView status, discovery, peerId;
    private WifiAwareAdvertising advertisingDriver;
    private WifiAwareDiscovery discoveryDriver;

    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_WIFI_STATE = 2;

    private static final String TAG = "BleDemo";

    private String stat;

    private int counter;

    private boolean adv;

    private boolean disc;

    private String id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        counter=0;
        adv=false;
        disc=false;
        advertisingDriver = WifiAwareAdvertising.getInstance(getApplicationContext());
        discoveryDriver = WifiAwareDiscovery.getInstance(getApplicationContext());
        advertisingDriver.setNotifier(this);
        discoveryDriver.setNotifier(this);
        setupPermissions();

        //advertisingDriver.setPassword("pass");

        //discoveryDriver.setPassword("pass");
        peerId = (TextView) findViewById(R.id.textview_peerid);

        this.id = randomString();
        peerId.setText("PeerId: "+id);

        startDiscButton = (Button) findViewById(R.id.startdiscovery);
        startAdvButton = (Button) findViewById(R.id.startadvertisement);

        stopButton = (Button) findViewById(R.id.stopbutton);
        refreshButton = (Button) findViewById(R.id.refreshbutton);

        status = (TextView) findViewById(R.id.textview_status);
        discovery = (TextView) findViewById(R.id.textview_discovery);

        discovery.setText("Users discovered: "+counter);
        //requestForPermissions();
        startDiscButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stat = randomString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setText("Status: "+stat);
                    }
                });
                //advertisingDriver.addAdvertisingInfo("bledemo",stat);
                discoveryDriver.addAdvertisingInfo("bledemo",stat);
                disc=true;
                //advertisingDriver.start(TAG,"peerId");
                discoveryDriver.start(TAG,id,2000,30000);
            }
        });

        startAdvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stat = randomString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        status.setText("Status: "+stat);
                    }
                });
                adv=true;
                advertisingDriver.addAdvertisingInfo("bledemo",stat);
                //discoveryDriver.addAdvertisingInfo("bledemo",stat);
                advertisingDriver.start(TAG,id);
                //discoveryDriver.start(TAG,"peerId",2000,30000);
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                advertisingDriver.stop();
                discoveryDriver.stop();
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //advertisingDriver.addAdvertisingInfo();

                stat = randomString();
                status.setText("Status: "+stat);

                advertisingDriver.addAdvertisingInfo("bledemo",stat);
                discoveryDriver.addAdvertisingInfo("bledemo",stat);
                advertisingDriver.stop();
                advertisingDriver.start(TAG,id);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        advertisingDriver.stop();
        discoveryDriver.stop();
        super.onDestroy();
    }


    @Override
    public void advertiserPeerDifferentStatus(String topic, byte[] bytes, String peerinfo) {
        Log.d(TAG,"differentStatusDiscovered "+topic+" "+peerinfo);
        advertisingDriver.notifyNetworkInformation(stat,stat);

    }

    /**
     * App Permissions for nearby devices
     **/
    private void setupPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            // And if we're on SDK M or later...
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Ask again, nicely, for the permissions.
                String[] permissionsWeNeed = new String[]{ Manifest.permission.NEARBY_WIFI_DEVICES };
                requestPermissions(permissionsWeNeed, MY_PERMISSION_NEARBY_DEVICES_REQUEST_CODE);
            }
        }


        //-------------------------------------------------------------------------------------------- -----
    }


    @Override
    public void advertiserPeerSameStatus() {
        Log.d(TAG,"sameStatusDiscovered");
        advertisingDriver.notifyEmptyValue();

    }

    @Override
    public void discoveryPeerDifferentStatus(String device, String topic, String network, String pass, String info) {
        Log.d(TAG,"peerDifferentStatusDiscovered "+device+" "+topic+" "+network+" "+pass+" "+info);
        stat = network;
        counter++;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                discovery.setText("Users discovered: "+counter);
                status.setText("Status: "+stat);
            }
        });
        if(adv){
            advertisingDriver.addAdvertisingInfo("bledemo",stat);
            advertisingDriver.stop();
            advertisingDriver.start(TAG,"peerId");
        }
        if(disc){
            discoveryDriver.addAdvertisingInfo("bledemo",stat);
            //discoveryDriver.stop();
            //discoveryDriver.start(TAG,"peerId",2000,30000);
        }

    }

    /*@Override
    public void peerDiscovered(String s) {
        Log.d(TAG,"peerDiscovered "+s);
    }*/

    @Override
    public void discoveryPeerSameStatus(String device, String topic) {
        Log.d(TAG,"peerSameStatusDiscovered "+device+" "+topic);

    }


    public String randomString() {

        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();

    }


}