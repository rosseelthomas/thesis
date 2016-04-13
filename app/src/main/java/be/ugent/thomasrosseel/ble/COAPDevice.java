package be.ugent.thomasrosseel.ble;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public class COAPDevice extends BLEProxyDevice {

    private CoapClient client;
    private String host;
    private int port;
    private String path;
    private String name;

    @Override
    public boolean connect() {
        client = new CoapClient("coap",host,port,path);
        client.get();

        return true;
    }

    public boolean observe(CoapHandler handler){
        client = new CoapClient("coap",host,port,path+"/status");
        client.observe(handler);

        return true;
    }

    @Override
    public Collection<Service> discoverServices() {
        ArrayList<Service> services = new ArrayList<>();
        client.get();
        CoapClient well_known_browser = new CoapClient("coap",host,port,".well-known/core");
        CoapResponse response = well_known_browser.get();
        String txt = response.getResponseText();

        ParseResult p = COAPParser.parse(txt);
        Log.i("parsed", "parsed");
        p = p.getChild(path);
        String base = "coap://"+host+":"+port+"/"+path;
        for ( ParseResult child : p.getChildren()) {
            if(!child.getId().equals("mac")){
                COAPService s = new COAPService(child.getTitle(),base+"/"+child.getId());


                for(ParseResult subchild : child.getChildren()){
                    COAPCharacteristic c = new COAPCharacteristic(subchild.getTitle(),base+"/"+child.getId()+"/"+subchild.getId());
                    s.addCharacteristic(c);
                }

                services.add(s);

            }

        }
        return services;
    }

    @Override
    public boolean disconnect() {
        client = null;
        return true;
    }

    public COAPDevice(String h, int p, String pa, String name, String mac, int ttl){
        super(name,mac,"coap://"+h+":"+p+"/"+pa,ttl);
        host = h;
        port = p;
        path = pa;
        this.name = name;
    }

    @Override
    public String toString() {
        return name+"\n"+" via: "+host+":"+port;
    }
}
