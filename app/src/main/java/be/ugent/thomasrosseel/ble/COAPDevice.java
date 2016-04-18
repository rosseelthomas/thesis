package be.ugent.thomasrosseel.ble;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

        CoapClient resource_browser = new CoapClient("coap",host,port,path);
        CoapResponse response = resource_browser.get();
        if(response!=null){
            String ip = host;
            if(response.getOptions().hasOption(OptionNumberRegistry.REDIRECT)){
                List<Option> options = response.getOptions().asSortedList();
                for(Option o :options){
                    if(o.getNumber()==OptionNumberRegistry.REDIRECT){
                        ip = o.getStringValue();
                    }
                }
                resource_browser = new CoapClient("coap",ip,port,path);
                response = resource_browser.get();
            }

            String txt = response.getResponseText();

            ParseResult p = COAPParser.parseDevice(txt);
            String base = "coap://"+ip+":"+port+"/"+path;
            for ( ParseResult child : p.getChildren()) {

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
        setStatus("ONLINE");
    }


}
