package be.ugent.thomasrosseel.ble;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasrosseel on 11/04/16.
 */
public class BLEProxyDeviceAdapter {

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

    @Override
    public String toString() {

        return getName()+"\n"+getMac()+"\n via "+devices.size()+" apparaten";

    }
}
