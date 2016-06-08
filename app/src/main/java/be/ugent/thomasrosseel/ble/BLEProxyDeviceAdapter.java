package be.ugent.thomasrosseel.ble;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasrosseel on 11/04/16.
 */
public class BLEProxyDeviceAdapter implements CoapHandler{

    private ArrayList<BLEProxyDevice> devices;



    public BLEProxyDeviceAdapter() {
        devices = new ArrayList<>();
    }

    public void addDevice(BLEProxyDevice d){
        devices.add(d);
    }

    public void removeDevice(BLEProxyDevice d){
        devices.remove(d);
    }

    public List<BLEProxyDevice> getAllDevices(){
        return (ArrayList<BLEProxyDevice>)devices.clone();
    }

    public String getMac(){
        if(devices.size()>0) return devices.get(0).getMac();
        return "";
    }

    public String getName(){
        if(devices.size() > 0) return devices.get(0).getNaam();
        return "";
    }

    public boolean isPhysical(){
        for(BLEProxyDevice b : (ArrayList<BLEProxyDevice>)devices.clone()){
            if(b instanceof BLEDevice) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String st="";
        if(devices.size()>0){
            st = "\n"+getAllDevices().get(0).getStatus();
        }


        for(BLEProxyDevice d : getAllDevices()){
            if(d.getStatus().startsWith("PROXY")){
                st = "\n"+d.getStatus();
            }else if(d.getStatus().startsWith("CONNECT") && d instanceof BLEDevice){
                st = "\n"+d.getStatus();
            }
        }

        return getName()+"\n"+getMac()+"\n via "+devices.size()+(devices.size() == 1 ? " apparaat" : " apparaten")+st;

    }

    public void connect(){
        for(BLEProxyDevice b : (ArrayList<BLEProxyDevice>)devices.clone()){
            if(b instanceof BLEDevice) {
                if(!((BLEDevice) b).isConnected()){
                    b.connect();



                }else{
                    b.disconnect();
                }

            }
        }
    }

    @Override
    public void onLoad(CoapResponse response) {

        for(BLEProxyDevice device : getAllDevices()){
            URI uri = URI.create(device.getPath());

            if(device instanceof BLEDevice){
                if(response.getResponseText().startsWith("CONNECT")){
                    if(!uri.getHost().equals(response.advanced().getSource().getHostAddress())){
                        device.setStatus("PROXY;" + response.advanced().getSource().getHostAddress());
                        ((BLEDevice)device).setProxy_ip(response.advanced().getSource().getHostAddress());
                        device.setTtl(120);
                        ((BLEDevice) device).setProxyTtl(120);
                    }else {
                        device.setStatus("CONNECTED");
                    }

                }else if(response.getResponseText().startsWith("DISCOV")){

                    if(device.getStatus().equals("PROXY;"+response.advanced().getSource().getHostAddress())){
                        device.setStatus("DISCOVERED");
                        ((BLEDevice)device).setProxy_ip("");
                    }else if(uri.getHost().equals(response.advanced().getSource().getHostAddress())){
                        device.setStatus("DISCOVERED");
                    }

                }
            }


        }



    }

    @Override
    public void onError() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BLEProxyDeviceAdapter that = (BLEProxyDeviceAdapter) o;

        return getMac().equals(that.getMac());

    }

    @Override
    public int hashCode() {
        return getMac().hashCode();
    }

    public void observeAll(){
        for(BLEProxyDevice d : getAllDevices()){
            if(d instanceof  COAPDevice){
                ((COAPDevice) d).observe(this);
            }
        }
    }
}
