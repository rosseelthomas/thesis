package be.ugent.thomasrosseel.ble;

import android.app.Application;
import android.content.Context;

import java.util.HashMap;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public class MyApplication extends Application {
    private static Context context;
    private static HashMap<String, Object> resources = new HashMap<>();
    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static void setAppContext(Context c){
        context =c;
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }

    public static void putResource(String s, Object o){
        resources.put(s,o);
    }

    public static Object getResource(String s){
        return resources.get(s);
    }
}
