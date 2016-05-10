package be.ugent.thomasrosseel.ble;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by thomasrosseel on 6/04/16.
 */
public class BleProxyParser {


    public static List<BLEProxyDevice> parse(String toparse, String src_ip){

        ArrayList<BLEProxyDevice> parsed = new ArrayList<>();
        HashMap<String,String> now_parsing = null;
        int ttl;
        while(!toparse.isEmpty()){


            if(toparse.startsWith("<")){
                //this is the URI
                if(now_parsing!=null){
                    //add ble device
                    BLEProxyDevice dev = new COAPDevice(now_parsing.get("uri"),now_parsing.get("title"),now_parsing.get("mac"),now_parsing.get("rt"),Integer.parseInt(now_parsing.get("ttl")));
                    dev.setStatus(now_parsing.get("status"));
                    parsed.add(dev);
                }


                 String uri = toparse.substring(0, toparse.indexOf(">")+1);
                toparse = toparse.substring(toparse.indexOf(">")+1);

                    uri = uri.substring(1, uri.length()-1);

                now_parsing = new HashMap<>();
                now_parsing.put("uri",uri);


            }else if(toparse.startsWith(",")){
                toparse = toparse.substring(1);
            }else if(toparse.startsWith(";")){
                toparse = toparse.substring(1);
                int pos_is = toparse.indexOf("=");
                String key = toparse.substring(0, pos_is);
                String value ="";
                if(toparse.charAt(pos_is+1) == '"'){
                    //value staat tussen quotes
                    int pos_puntkomma = toparse.indexOf("\";");
                    int pos_komma = toparse.indexOf("\",");


                    int pos_komma_puntkomma=-1;
                    if(pos_komma==-1){
                        pos_komma_puntkomma = pos_puntkomma;
                    }else if(pos_puntkomma==-1){
                        pos_komma_puntkomma = pos_komma;
                    }else if(pos_komma > pos_puntkomma){
                        pos_komma_puntkomma = pos_puntkomma;
                    }else{
                        pos_komma_puntkomma = pos_komma;
                    }
                     value = toparse.substring(pos_is+2, pos_komma_puntkomma);
                    toparse = toparse.substring(pos_komma_puntkomma+1);
                }else{
                    //value niet tussen quotes
                    int pos_puntkomma = toparse.indexOf(";");
                    int pos_komma = toparse.indexOf(",");


                    int pos_komma_puntkomma=-1;
                    if(pos_komma==-1){
                        pos_komma_puntkomma = pos_puntkomma;
                    }else if(pos_puntkomma==-1){
                        pos_komma_puntkomma = pos_komma;
                    }else if(pos_komma > pos_puntkomma){
                        pos_komma_puntkomma = pos_puntkomma;
                    }else{
                        pos_komma_puntkomma = pos_komma;
                    }
                    if(pos_komma_puntkomma==-1){
                        pos_komma_puntkomma = toparse.length();
                    }
                     value = toparse.substring(pos_is + 1, pos_komma_puntkomma);
                    toparse = toparse.substring(pos_komma_puntkomma);
                }
                now_parsing.put(key,value);
            }else{
                throw  new RuntimeException("unparsable bleproxy");
            }
        }


        if(now_parsing!=null){
            //add ble device
            BLEProxyDevice dev = new COAPDevice(now_parsing.get("uri"),now_parsing.get("title"),now_parsing.get("mac"),now_parsing.get("rt"),Integer.parseInt(now_parsing.get("ttl")));
            parsed.add(dev);
        }
        return parsed;
    }
}
