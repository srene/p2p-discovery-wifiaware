package network.datahop.wifiaware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.IdentityChangedListener;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import datahop.AdvertisingDriver;
import datahop.AdvertisementNotifier;

public class WifiAwareAdvertising implements AdvertisingDriver, Publication.Published  {

    private static final String TAG = WifiAwareAdvertising.class.getSimpleName();

    private static volatile WifiAwareAdvertising mWifiAwareAdvertising;

    private Context context;
    private ConnectivityManager connectivityManager;
    private WifiAwareSession wifiAwareSession;
    private WifiAwareManager wifiAwareManager;
    private BroadcastReceiver broadcastReceiver;

    private static AdvertisementNotifier notifier;

    private List<UUID> pendingNotifications;

    private String serviceId,peerInfo;

    private int port;

    private boolean started;

    private HashMap<byte[],byte[]> advertisingInfo;

    private Publication pub;
    private WifiAwareAdvertising(Context context){
        this.context = context;
        this.connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.port = 4324;
        this.advertisingInfo = new HashMap<>();
    }

    /* Singleton method that creates and returns a BLEAdvertising instance
     * @return BLEAdvertising instance
     */
    public static synchronized WifiAwareAdvertising getInstance(Context appContext) {
        if (mWifiAwareAdvertising == null) {
            mWifiAwareAdvertising = new WifiAwareAdvertising(appContext);
        }
        return mWifiAwareAdvertising;
    }

    /**
     * Set the notifier that receives the events advertised
     * when creating or destroying the group or when receiving users connections
     * @param notifier instance
     */
    public void setNotifier(AdvertisementNotifier notifier){
        //Log.d(TAG,"Trying to start");
        this.notifier = notifier;
    }


    @Override
    public void start(String serviceId, String peerInfo) {
        this.serviceId = serviceId;
        this.peerInfo = peerInfo;
        Log.d(TAG, "Starting WifiAware ADV " + this.serviceId.toString());

        if (notifier == null || this.serviceId == null) {
            Log.e(TAG, "notifier not found");
            return ;
        }

        this.pub = new Publication(this);

        PackageManager packageManager = context.getPackageManager();
        boolean hasNan  = false;

        if (packageManager == null) {
            return;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasNan = packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE);
            }
        }

        if (hasNan) {

            wifiAwareManager = (WifiAwareManager)context.getSystemService(Context.WIFI_AWARE_SERVICE);

            if (wifiAwareManager == null) {
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Entering OnResume is executed");
            IntentFilter filter   = new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED);
            broadcastReceiver     = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    wifiAwareManager.getCharacteristics();
                    boolean nanAvailable = wifiAwareManager.isAvailable();
                    Log.d(TAG, "NAN is available");
                    if (nanAvailable) {
                        attachToNanSession();
                        Log.d(TAG, "NAN attached");
                    } else {
                        Log.d(TAG, "NAN unavailable");
                        //return false;
                    }

                }
            };

            context.registerReceiver(broadcastReceiver, filter);

            boolean nanAvailable = wifiAwareManager.isAvailable();
            if (nanAvailable) {
                attachToNanSession();
            }
        }

    }

    /**
     * Handles attaching to NAN session.
     *
     */
    private void attachToNanSession() {
        Log.d(TAG,"attachToNanSession");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        // Only once
        if (wifiAwareSession != null) {
            return;
        }

        if (wifiAwareManager == null || !wifiAwareManager.isAvailable()) {
            //setStatus("NAN is Unavailable in attach");
            return;
        }

        Log.d(TAG,"attaching...");

        wifiAwareManager.attach(new AttachCallback() {
            @Override
            public void onAttached(WifiAwareSession session) {
                super.onAttached(session);
                Log.d(TAG,"onAttached");

                closeSession();
                wifiAwareSession = session;
                //setHaveSession(true);
                startDiscovery(wifiAwareSession);
            }

            @Override
            public void onAttachFailed() {
                super.onAttachFailed();
                //setHaveSession(false);
                //setStatus("attach() failed.");
                Log.d(TAG,"attach() failed");
            }

        }, new IdentityChangedListener() {
            @Override
            public void onIdentityChanged(byte[] mac) {
                super.onIdentityChanged(mac);
                //setMacAddress(mac);
            }
        }, null);
    }


    public void startDiscovery(WifiAwareSession session){
        pub.closeSession();
        pub.publishService(session,portToBytes(port),advertisingInfo);
    }

    private void closeSession() {

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
    }

    @Override
    public void addAdvertisingInfo(String topic, String info) {
        this.advertisingInfo.put(topic.getBytes(StandardCharsets.UTF_8),info.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void notifyEmptyValue() {

    }

    @Override
    public void notifyNetworkInformation(String s, String s1) {

    }

    @Override
    public void stop() {
        closeSession();
    }

    @Override
    public void messageReceived(byte[] message) {

    }

    public int byteToPortInt(byte[] bytes){
        return ((bytes[1] & 0xFF) << 8 | (bytes[0] & 0xFF));
    }

    public byte[] portToBytes(int port){
        byte[] data = new byte [2];
        data[0] = (byte) (port & 0xFF);
        data[1] = (byte) ((port >> 8) & 0xFF);
        return data;
    }
}
