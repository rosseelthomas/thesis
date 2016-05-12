package be.ugent.thomasrosseel.ble;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import java.net.URI;

/**
 * Created by thomasrosseel on 5/04/16.
 */
public abstract class BLEProxyDevice implements Device {

    private String naam, mac, path,status="DISCOVERED", type="";
    private long millis_end;


    private DeviceResource statusresource;

    public void setStatusresource(DeviceResource statusresource) {
        this.statusresource = statusresource;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getTtl() {

        return (int)(millis_end - System.currentTimeMillis())/1000;
    }

    public void setTtl(int ttl) {

        millis_end = System.currentTimeMillis() + ttl*1000;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;

    }

    public void notifyStatus() {
        if(statusresource!=null){
            statusresource.changed();
        }

        //BleListActivity.getBleproxy().changed();
            BleListActivity.sendProxyPUT();

    }

    public BLEProxyDevice(String naam, String mac, String path, String type, int ttl) {
        this.naam = naam;
        this.mac = mac;
        this.path = path;
        setType(type);
        setTtl(ttl);
    }

    public String toResource(){
        //return naam+";"+mac+";"+path+";"+getTtl()+";";
        String interf = this instanceof BLEDevice ? "ble":"coap";
        return "<"+path+">;ct=40;rt=\""+type+"\";if=\""+interf+"\";status=\""+getStatus()+"\";mac=\""+mac+"\";title=\""+naam+"\";ttl="+getTtl();
    }

    @Override
    public String toString() {
        return naam+"\n"+mac+"\n"+path+"\n"+getTtl()+"\n"+getStatus();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BLEProxyDevice that = (BLEProxyDevice) o;

        if (!naam.equals(that.naam)) return false;
        if (!mac.equals(that.mac)) return false;
        return path.equals(that.path);

    }

    @Override
    public int hashCode() {
        int result = naam.hashCode();
        result = 31 * result + mac.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }


}
