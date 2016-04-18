package be.ugent.thomasrosseel.ble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.jar.Attributes;

/**
 * Created by thomasrosseel on 1/03/16.
 */
public class COAPParser {
    public static ParseResult parse(String payload){

        ParseResult result = new ParseResult("root","root");

            String[] resources = payload.split("</");
            for(String resource : resources){
                String[] splitted = resource.split(">;");
                if(splitted.length>1){
                    String id = splitted[0];
                    String title = splitted[1];
                    title = title.replace("title=\"","");
                    title = title.replace("\",","");

                    String[] idparts = id.split("/");
                    ParseResult temp = result;
                    int i;
                    for( i = 0;i<idparts.length-1;++i){
                        temp = temp.getChild(idparts[i]);
                    }

                    temp.addChild(new ParseResult(idparts[i],title));
                }



            }



        return result;
    }

    public static ParseResult parseDevice(String payload){
        ParseResult root = new ParseResult("root","root");
        HashMap<String,ArrayList<String>> resourcemap = new HashMap<>();

        String[] resources = payload.split("(?<!\\\\);");
        for(String resource : resources){
            String[] parts = resource.split("/");
            if(parts.length==2){
                if(!resourcemap.containsKey(parts[0])){
                    resourcemap.put(parts[0],new ArrayList<String>());
                }
                resourcemap.get(parts[0]).add(parts[1]);
            }
        }

        for(String resource : resourcemap.keySet()){
            ParseResult p = new ParseResult(resource, resource);
            for(String sub : resourcemap.get(resource)){
                p.addChild(new ParseResult(sub,sub));
            }
            root.addChild(p);
        }

        return root;
    }


}
