package network.datahop.wifiaware;

import android.annotation.TargetApi;
import android.net.NetworkSpecifier;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

public class Subscription {

    interface Subscribed {
        void messageReceived(byte[] message);
    }

    SubscribeDiscoverySession subscribeDiscoverySession;
    WifiAwareSession wifiAwareSession;

    private Subscribed subs;
    private byte[] peerId,status;
    private PeerHandle peerHandle_;

    public Subscription(Subscribed subs){
        this.subs = subs;
    }
    //-------------------------------------------------------------------------------------------- +++++
    @TargetApi(26)
    public void subscribeToService(WifiAwareSession wifiAwareSession,byte[] peerId,byte[] status) {
        this.wifiAwareSession = wifiAwareSession;
        this.peerId = peerId;
        this.status = status;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { return; }

        SubscribeConfig config = new SubscribeConfig.Builder()
                .setServiceName("network.datahop.wifiawaresample")
                .build();

        wifiAwareSession.subscribe(config, new DiscoverySessionCallback() {

            @Override
            public void onSubscribeStarted(@NonNull SubscribeDiscoverySession session) {
                super.onSubscribeStarted(session);

                subscribeDiscoverySession = session;
                Log.d("subscribeToService", "onSubscribeStarted");

            }

            @Override
            public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                super.onServiceDiscovered(peerHandle, serviceSpecificInfo, matchFilter);
                //peerHandle = peerHandle_;
                Log.d("subscribeToService", "onServiceDiscovered");
               // if(!networkBuilt)
                subscribeDiscoverySession.sendMessage(peerHandle,WifiAware.STATUS_MESSAGE,status);
            }


            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                peerHandle_ = peerHandle;
                Log.d("subscribeToService", "received message "+message.length);
                subs.messageReceived(message);
            }
        }, null);
    }

    public SubscribeDiscoverySession getSession(){
        return subscribeDiscoverySession;
    }

    public NetworkSpecifier specifyNetwork(){
        return new WifiAwareNetworkSpecifier.Builder(subscribeDiscoverySession, peerHandle_)
                .build();
    }

    public void closeSession(){
        if (subscribeDiscoverySession != null) {
            subscribeDiscoverySession.close();
            subscribeDiscoverySession = null;
        }
    }
}

