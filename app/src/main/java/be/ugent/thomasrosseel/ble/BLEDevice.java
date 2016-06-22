package be.ugent.thomasrosseel.ble;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public class BLEDevice extends BLEProxyDevice {

    private final BluetoothDevice device;
    private boolean connected;
    private BLECallback callback;
    private String proxy_ip;
    private long proxy_millis;



    private boolean refresh_needed = true;

    private Timer ttl_timer;

    private BluetoothGatt connection;

    public BluetoothGatt getConnection() {
        return connection;
    }

    public int getProxyTtl() {

        return (int)(proxy_millis - System.currentTimeMillis())/1000;
    }

    public void setProxyTtl(int ttl) {

        proxy_millis = System.currentTimeMillis() + ttl*1000;
    }

    @java.lang.Override
    public int getTtl() {
        if(getStatus().startsWith("PROXY")){
            return getProxyTtl();
        }
        return super.getTtl();
    }

    public BLEDevice(BluetoothDevice device, String path, String type, int ttl) {
        super(device.getName(), device.getAddress(), path, type, ttl);
        this.device = device;
        callback = new BLECallback();
        connected = false;

        BLEEventListener disconnectionListener = new BLEEventListener() {
            @Override
            public void onEvent(Object data) {
                setStatus("DISCOVERED");
                connected=false;
                if(ttl_timer!=null)
                    ttl_timer.cancel();
                notifyStatus();

            }
        };


        callback.addDisconnectListener(disconnectionListener);

        BLEEventListener connectionListener = new BLEEventListener() {
            @Override
            public void onEvent(Object data) {

                setStatus("CONNECTED");
                BLEDevice.this.setStatus("CONNECTED");
                notifyStatus();



                connected = true;
                ttl_timer = new Timer();
                TimerTask ttl_timer_task = new TimerTask() {
                    @Override
                    public void run() {
                        setTtl(120);
                    }
                };
                ttl_timer.schedule(ttl_timer_task, 0, 5000);

            }
        };
        callback.addConnectListener(connectionListener);

    }

    public String getProxy_ip() {
        return proxy_ip;
    }

    public void setProxy_ip(String proxy_ip) {
        this.proxy_ip = proxy_ip;
    }

    public BluetoothDevice getDevice() {
        return device;
    }


    public boolean isConnected() {
       return connected;
    }


    @Override
    public boolean connect() {
        if(connected) return  false;

        if(getStatus().equals("DISCOVERED")) {


            final CountDownLatch waiter = new CountDownLatch(1);

            BLEEventListener connectionListener = new BLEEventListener() {
                @Override
                public void onEvent(Object data) {
                    callback.removeConnectListener(this);
                    //if (refresh_needed) {
                        refreshDeviceCache(connection);
                        //refresh_needed = false;
                    //}

                    waiter.countDown();


                }
            };
            callback.addConnectListener(connectionListener);


                    connection = device.connectGatt(MyApplication.getAppContext(), false, callback);





            try {
                waiter.await(10, TimeUnit.SECONDS);


            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (waiter.getCount() > 0) return false;
            /*try {
                synchronized (this){

                    wait(600);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            android.os.SystemClock.sleep(2000);
            Log.i("connect", "connected");


            return true;
        }else{
            return true;
        }



    }

    @Override
    public Collection<Service> discoverServices() {

        if(!connected){

            connect();
            refreshDeviceCache(connection);

        }



        final Collection<Service> returnservices = new ArrayList<>();
        if(connected){

            Log.d("gatt","discovering services");
            final CountDownLatch waiter = new CountDownLatch(1);
            final Collection<BLEService> services = new ArrayList<>();


            BLEEventListener discoveryListener = new BLEEventListener() {
                @Override
                public void onEvent(Object o) {
                    callback.removeDiscoveryListener(this);
                    final Collection<BLEService> collection = (Collection<BLEService>)o;
                    for(BLEService s : collection){

                        s.setDevice(BLEDevice.this);
                        services.add(s);

                    }
                    waiter.countDown();


                }
            };
            callback.addDiscoveryListener(discoveryListener);
            connection.discoverServices();
            try {
                waiter.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            returnservices.addAll(services);
        }


        return returnservices;


    }


    @Override
    public boolean disconnect() {
        if(!connected) return false;

        final CountDownLatch waiter = new CountDownLatch(1);

        BLEEventListener disconnectionListener = new BLEEventListener() {
            @Override
            public void onEvent(Object data) {
                callback.removeDisconnectListener(this);
                waiter.countDown();

            }
        };
        callback.addDisconnectListener(disconnectionListener);



        connection.disconnect();
        connection.close();


        try {
            waiter.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i("connect", "disconnected");

        setStatus("DISCOVERED");
        notifyStatus();
        connected = false;
        if(ttl_timer!=null)
            ttl_timer.cancel();
        return true;



    }



    public BLECallback getCallback() {
        return callback;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e("refresh", "An exception occured while refreshing device");
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toString()+"\nPHYSICAL";
    }
}
