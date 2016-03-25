package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public class BLECharacteristic implements Characteristic {

    private BluetoothGattCharacteristic characteristic;
    private BLEService service;

    private BLEEventListener notifylistener;


    public BLEService getService() {
        return service;
    }

    public void setService(BLEService service) {
        this.service = service;
    }

    public BLECharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }



    @Override
    public byte[] read() {



        final CountDownLatch waiter = new CountDownLatch(1);
        BLEEventListener l = new BLEEventListener() {
            @Override
            public void onEvent(Object data) {
                getService().getDevice().getCallback().removeReadListener(this);
                BluetoothGattCharacteristic ch = (BluetoothGattCharacteristic) data;
                characteristic.setValue(((BluetoothGattCharacteristic) data).getValue());
                waiter.countDown();

            }
        };
        getService().getDevice().getCallback().addReadListener(l);


                getService().getDevice().connect();
                getService().getDevice().discoverServices();
                BluetoothGatt gatt = getService().getDevice().getConnection();
                Boolean b = gatt.readCharacteristic(characteristic);


        try {
            waiter.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getService().getDevice().disconnect();
        if(waiter.getCount()>0) return null;
        return characteristic.getValue();

    }

    @Override
    public void write(byte[] bytes) {





        final CountDownLatch waiter = new CountDownLatch(1);
        BLEEventListener l = new BLEEventListener() {
            @Override
            public void onEvent(Object data) {
                getService().getDevice().getCallback().removeWriteListener(this);
                waiter.countDown();

            }
        };
        getService().getDevice().getCallback().addWriteListener(l);


        byte [] b = new byte[1];
        b[0] = (byte)255;

        getService().getDevice().connect();
        BluetoothGatt gatt = getService().getDevice().getConnection();
        getService().getDevice().discoverServices();
        characteristic.setValue(b);
        gatt.writeCharacteristic(characteristic);


        try {
            waiter.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getService().getDevice().disconnect();
    }


    public void startNotify(final BLEEventListener listener){

        notifylistener = new BLEEventListener() {
            @Override
            public void onEvent(Object data) {
                characteristic.setValue(((BluetoothGattCharacteristic) data).getValue());
                listener.onEvent(characteristic.getValue());
            }
        };


        getService().getDevice().connect();
        getService().getDevice().discoverServices();
        BluetoothGatt gatt = getService().getDevice().getConnection();
        Boolean b = gatt.setCharacteristicNotification(characteristic, true);
        getService().getDevice().getCallback().addChangeListener(notifylistener);
        // 0x2902 org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);


    }

    public void stopNotify(BLEEventListener listener){
        BluetoothGatt gatt = getService().getDevice().getConnection();
        Boolean b = gatt.setCharacteristicNotification(characteristic, false);
        getService().getDevice().getCallback().removeChangeListener(notifylistener);
        getService().getDevice().disconnect();
    }

    @Override
    public String toString() {
        int uuid = (int) (characteristic.getUuid().getMostSignificantBits() >> 32);
        Gatt s = GattTranslate.getInstance().getGatt(uuid);
        if(s==null)
            return uuid+"";
        return s.getDescription();
    }

    @Override
    public UUID getUUID() {
        return characteristic.getUuid();
    }
}
