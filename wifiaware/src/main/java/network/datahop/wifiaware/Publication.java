package network.datahop.wifiaware;


import android.net.NetworkSpecifier;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareSession;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

public class Publication {

    interface Published {
        void messageReceived(byte[] message);
    }

    private WifiAwareSession wifiAwareSession;
    private PublishDiscoverySession publishDiscoverySession;
    private Published pubs;
    private PeerHandle peerHandle_;

    public Publication(Published pubs){
        this.pubs = pubs;
    }

    public void publishService(WifiAwareSession wifiAwareSession,byte[] port, byte[] status) {

        this.wifiAwareSession = wifiAwareSession;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { return; }

        PublishConfig config = new PublishConfig.Builder()
                .setServiceName("network.datahop.wifiawaresample")
                .build();

        wifiAwareSession.publish(config, new DiscoverySessionCallback() {
            @Override
            public void onPublishStarted(@NonNull PublishDiscoverySession session) {
                super.onPublishStarted(session);

                publishDiscoverySession = session;

                Log.d("publishService", "onPublishStarted");


            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                peerHandle_ = peerHandle;
                Log.d("publishService", "received message "+message.length+" "+new String(message));
                pubs.messageReceived(message);


            }
        }, null);
        //-------------------------------------------------------------------------------------------- -----
    }

    public PublishDiscoverySession getSession(){
        return publishDiscoverySession;
    }

    public void sendIP(byte[] ip){
        publishDiscoverySession.sendMessage(peerHandle_,WifiAware.IP_MESSAGE,ip);
    }

    public NetworkSpecifier specifyNetwork(byte[] port){
        publishDiscoverySession.sendMessage(peerHandle_,WifiAware.PORT_MESSAGE,port);
        return new WifiAwareNetworkSpecifier.Builder(publishDiscoverySession, peerHandle_)
                .build();
    }

    public void closeSession(){
        if (publishDiscoverySession != null) {
            publishDiscoverySession.close();
            publishDiscoverySession = null;
        }

    }
}
