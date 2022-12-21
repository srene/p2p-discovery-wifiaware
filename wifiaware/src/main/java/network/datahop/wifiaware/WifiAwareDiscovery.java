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
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import datahop.DiscoveryNotifier;
import datahop.DiscoveryDriver;

public class WifiAwareDiscovery implements DiscoveryDriver, Subscription.Subscribed{

    public static final int  STATUS_MESSAGE = 66;

    private static Context context;

    private static volatile WifiAwareDiscovery mWifiAwareDiscovery;

    private HashMap<byte[],byte[]> advertisingInfo;

    private Subscription sub;


    private static DiscoveryNotifier notifier;

    private WifiAwareSession wifiAwareSession;
    private WifiAwareManager wifiAwareManager;
    private BroadcastReceiver broadcastReceiver;
    private String serviceId,peerId;

    private static final String TAG = WifiAwareDiscovery.class.getSimpleName();

    /**
     * WifiAwareDiscovery class constructor
     * @param context Android context
     */
    private WifiAwareDiscovery(Context context)
    {
        this.context = context;
        this.advertisingInfo = new HashMap<>();

    }

    /* Singleton method that creates and returns a WifiAwareDiscovery instance
     * @return WifiAwareDiscovery instance
     */
    public static synchronized WifiAwareDiscovery getInstance(Context appContext) {
        if (mWifiAwareDiscovery == null) {
            mWifiAwareDiscovery = new WifiAwareDiscovery(appContext);
        }
        return mWifiAwareDiscovery;
    }

    /**
     * Set the notifier that receives the events advertised
     * when creating or destroying the group or when receiving users connections
     * @param notifier instance
     */
    public void setNotifier(DiscoveryNotifier notifier){
        Log.d(TAG,"Trying to start");
        this.notifier = notifier;
    }


    @Override
    public void start(String serviceId,String peerInfo, long scanTime, long idleTime) {

        this.serviceId = serviceId;
        this.peerId = peerInfo;
        Log.d(TAG, "Starting WifiAware ADV " + this.serviceId.toString());

        if (notifier == null || this.serviceId == null) {
            Log.e(TAG, "notifier not found");
            return ;
        }

        this.sub = new Subscription(this);

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

    @Override
    public void stop() {

    }

    @Override
    public void addAdvertisingInfo(String characteristic, String info){

    }

    @Override
    public void messageReceived(byte[] message) {

    }

    private void startDiscovery(WifiAwareSession session){
        sub.closeSession();
        sub.subscribeToService(session,peerId.getBytes(StandardCharsets.UTF_8),advertisingInfo);
    }

    private void closeSession() {

        if (wifiAwareSession != null) {
            wifiAwareSession.close();
            wifiAwareSession = null;
        }
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
