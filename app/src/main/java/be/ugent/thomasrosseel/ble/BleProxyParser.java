package be.ugent.thomasrosseel.ble;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasrosseel on 6/04/16.
 */
public class BleProxyParser {

    public static List<BLEProxyDevice> parse(String toparse, String src_ip){

        ArrayList<BLEProxyDevice> parsed = new ArrayList<>();
        String[] splitted = toparse.split("(?<!\\\\);");
        for(int i=0;i<splitted.length;++i){
            String naam = splitted[i];
            i++;
            String mac = splitted[i];
            i++;
            String path = splitted[i];
            URI p = URI.create(path);
            if(p.getHost().equals("127.0.0.1")){


                try {
                    p = new URI(p.getScheme(), p.getUserInfo(), src_ip, p.getPort(), p.getPath(), p.getQuery(), p.getFragment());

                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
            i++;
            int ttl = Integer.parseInt(splitted[i]);
            BLEProxyDevice dev = new COAPDevice(p.getHost(),p.getPort(),p.getPath().substring(1),naam,mac,ttl);
            parsed.add(dev);


        }

        return parsed;
    }

}
