package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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


    private boolean refresh_needed = true;

    private Timer ttl_timer;

    private BluetoothGatt connection;

    public BluetoothGatt getConnection() {
        return connection;
    }

    public BLEDevice(BluetoothDevice device, String path, int ttl) {
        super(device.getName(), device.getAddress(), path, ttl);
        this.device = device;
        callback = new BLECallback();
        connected = false;

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
                    if (refresh_needed) {
                        refreshDeviceCache(connection);
                        refresh_needed = false;
                    }

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
            Log.i("connect", "connected");
            setStatus("CONNECTED");
            notifyStatus();

            connected = true;
            ttl_timer = new Timer();
            TimerTask ttl_timer_task = new TimerTask() {
                @Override
                public void run() {
                    setTtl(60);
                }
            };
            ttl_timer.schedule(ttl_timer_task, 0, 5000);

            return true;
        }else{
            return true;
        }



    }

    @Override
    public Collection<Service> discoverServices() {

        final CountDownLatch waiter = new CountDownLatch(1);
        final Collection<BLEService> services = new ArrayList<>();
        final Collection<Service> returnservices = new ArrayList<>();


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

    @Override
    public String toString() {
        return device.getName()+"\n"+device.getAddress();
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
}
