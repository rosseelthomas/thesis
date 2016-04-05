package be.ugent.thomasrosseel.ble;

import java.util.ArrayList;

/**
 * Created by thomasrosseel on 4/04/16.
 */
public class BLEResource extends TXTGETResource {
    private ArrayList<String> devices = new ArrayList<>();

    public BLEResource(String name) {
        super(name);
    }

    public void addDevice(String d){
        devices.add(d);
        String txt="";
        for(String de : devices){
            txt+=de+";";
        }
        setTxt(txt);
    }

    public void removeDevice(String d){
        devices.remove(d);
    }





}
