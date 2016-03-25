package be.ugent.thomasrosseel.ble;

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


}
