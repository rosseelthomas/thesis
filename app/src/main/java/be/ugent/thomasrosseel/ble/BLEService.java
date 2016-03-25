package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public class BLEService implements Service {

    private final BluetoothGattService service;
    private  BLEDevice device;

    public BLEDevice getDevice() {
        return device;
    }

    public void setDevice(BLEDevice device) {
        this.device = device;
    }

    public BLEService(BluetoothGattService service) {
        this.service = service;
    }

    @Override
    public Collection<Characteristic> discoverCharacteristics() {
        Collection<Characteristic> collection = new ArrayList<>();
        for(BluetoothGattCharacteristic c : service.getCharacteristics()){
            BLECharacteristic ch = new BLECharacteristic(c);
            ch.setService(this);
            collection.add(ch);
        }

        return  collection;
    }

    @Override
    public String toString() {

        int uuid = (int) (service.getUuid().getMostSignificantBits() >> 32);
        Gatt s = GattTranslate.getInstance().getGatt(uuid);
        if(s==null)
            return uuid+"";
        return s.getDescription();
    }

    @Override
    public UUID getUUID() {
        return service.getUuid();
    }
}
