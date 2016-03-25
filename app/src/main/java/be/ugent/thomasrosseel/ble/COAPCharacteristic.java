package be.ugent.thomasrosseel.ble;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;

import java.util.UUID;

/**
 * Created by thomasrosseel on 9/03/16.
 */
public class COAPCharacteristic implements Characteristic {

    private String name,path;

    public COAPCharacteristic(String name, String path) {
        this.name = name;
        this.path = path;
    }

    @Override
    public byte[] read() {

        CoapClient c = new CoapClient(path);



        byte[] bytes =  c.get().getPayload();
        return bytes;
    }

    @Override
    public void write(byte[] bytes) {
        CoapClient c = new CoapClient(path);
        c.put(bytes,0);

    }

    @Override
    public UUID getUUID() {
        return null;
    }

    @Override
    public void startNotify(BLEEventListener listener) {

         CoapClient c = new CoapClient(path);
        c.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {

            }

            @Override
            public void onError() {

            }
        });

    }

    @Override
    public void stopNotify(BLEEventListener listener) {

    }

    @Override
    public String toString() {
        return name;
    }
}
