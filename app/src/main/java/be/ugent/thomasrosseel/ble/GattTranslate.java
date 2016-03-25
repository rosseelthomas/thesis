package be.ugent.thomasrosseel.ble;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by thomasrosseel on 24/02/16.
 */
public class GattTranslate {


    private static GattTranslate instance;
    private Map<Integer, Gatt> gattMap;
    private static Context context;

    public static void setContext(Context context) {
        GattTranslate.context = context;
    }

    private GattTranslate(){



        gattMap = new HashMap<>();
        BufferedReader reader = null;
        try {



             reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.gatt)));
            String line="";
            while((line = reader.readLine()) != null){
                String[] array = line.split(";");

                int id = Integer.parseInt(array[0].replace("0x","").replace("0X",""),16);


                Gatt g = new Gatt(id,array[1],array[2]);
                gattMap.put(id, g);

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static GattTranslate getInstance(){
        if(instance == null)
            instance = new GattTranslate();
        return instance;
    }

    public Gatt getGatt(int id){
        return gattMap.get(id);


    }

    public Gatt getGatt(String id) {
        return getGatt(Integer.parseInt(id.replace("0X","").replace("0x",""),16));


    }

}
