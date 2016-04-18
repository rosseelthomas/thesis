package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public class BLECallback extends BluetoothGattCallback {

    private ArrayList<BLEEventListener> onconnect;
    private ArrayList<BLEEventListener> ondisconnect;
    private ArrayList<BLEEventListener> onservicediscovery;
    private ArrayList<BLEEventListener> onread;
    private ArrayList<BLEEventListener> onchange;
    private ArrayList<BLEEventListener> onwrite;
    public BLECallback(){
        onconnect = new ArrayList<>();
        ondisconnect = new ArrayList<>();
        onservicediscovery = new ArrayList<>();
        onread = new ArrayList<>();
        onchange = new ArrayList<>();
        onwrite = new ArrayList<>();
    }


    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        String intentAction;
        Log.i("gatt",newState+"");
        if (newState == BluetoothProfile.STATE_CONNECTED) {

            Log.i("gatt", "Connected to GATT server.");

            new Runnable() {
                @Override
                public void run() {
                    for(BLEEventListener l : (ArrayList<BLEEventListener>) onconnect.clone()){
                        l.onEvent(null);
                    }
                }
            }.run();




        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

            Log.i("gatt", "Disconnected from GATT server.");
            new Runnable() {
                @Override
                public void run() {
                    for(BLEEventListener l : (ArrayList<BLEEventListener>)  ondisconnect.clone()){
                        l.onEvent(null);
                    }
                }
            }.run();


        }
    }

    @Override
    public void onServicesDiscovered(final BluetoothGatt gatt, int status) {

        new Runnable() {
            @Override
            public void run() {
                Collection<Service> services = new ArrayList<>();
                for (BluetoothGattService service : gatt.getServices()) {


                    int uuid = (int) (service.getUuid().getMostSignificantBits() >> 32);

                    BLEService srv = new BLEService(service);

                    services.add(new BLEService(service));
                }

                for(BLEEventListener l : (ArrayList<BLEEventListener>)  onservicediscovery.clone()){
                    l.onEvent(services);
                }
            }
        }.run();






    }


    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
        //super.onCharacteristicRead(gatt,characteristic,status);
        Log.i("read", "characteristic read");

        new Runnable() {
            @Override
            public void run() {
                for(BLEEventListener l : (ArrayList<BLEEventListener>) onread.clone()){
                    l.onEvent(characteristic);
                }
            }
        }.run();



    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        Log.i("changed", "characteristic changed");

        new Runnable() {
            @Override
            public void run() {
                for(BLEEventListener l : (ArrayList<BLEEventListener>) onchange.clone()){
                    l.onEvent(characteristic);
                }
            }
        }.run();



    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
        Log.i("written", "characteristic written");
        new Runnable() {
            @Override
            public void run() {
                for(BLEEventListener l : (ArrayList<BLEEventListener>) onwrite.clone()){
                    l.onEvent(characteristic);
                }
            }
        }.run();


    }

    public void addConnectListener(BLEEventListener l){
        onconnect.add(l);
    }

    public void removeConnectListener(BLEEventListener l){
        onconnect.remove(l);
    }

    public void addDisconnectListener(BLEEventListener l){
        ondisconnect.add(l);
    }

    public void removeDisconnectListener(BLEEventListener l){
        ondisconnect.remove(l);
    }

    public void addDiscoveryListener(BLEEventListener l){
        onservicediscovery.add(l);
    }

    public void removeDiscoveryListener(BLEEventListener l){
        onservicediscovery.remove(l);
    }

    public void addReadListener(BLEEventListener l){
        onread.add(l);
    }

    public void removeReadListener(BLEEventListener l){
        onread.remove(l);
    }

    public void addChangeListener(BLEEventListener l){
        onchange.add(l);
    }

    public void removeChangeListener(BLEEventListener l){
        onchange.remove(l);
    }

    public void addWriteListener(BLEEventListener l){
        onwrite.add(l);
    }

    public void removeWriteListener(BLEEventListener l){
        onwrite.remove(l);
    }


}


