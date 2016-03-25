package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Created by thomasrosseel on 24/02/16.
 */
public class GattGetRequestHandler implements RequestHandler {

    private BluetoothDevice device;
    private BluetoothGattCharacteristic characteristic;
    private Context context;


    @Override
    public void handleGET(final CoapExchange ex) {
        BluetoothGattCallback getcb = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {


                    gatt.discoverServices();

                }

            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                gatt.readCharacteristic(characteristic);


            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                String ret = Base64.encodeToString(characteristic.getValue(), Base64.DEFAULT);
                gatt.disconnect();
                ex.respond(ret);
            }
        };



        device.connectGatt(context, true, getcb);
    }

    @Override
    public void handlePUT(final CoapExchange ex) {
        ex.accept();
        BluetoothGattCallback getcb = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("PUT", ex.getRequestText() + "(" + ex.getRequestPayload().length + ")");

                    characteristic.setValue(ex.getRequestPayload());
                    byte[] value = new byte[1];
                    value[0] = (byte) (1);

                    Log.i("ENCODE", Base64.encodeToString(value, Base64.DEFAULT));


                    characteristic.setValue(Base64.decode(ex.getRequestText(), Base64.DEFAULT));
                    Log.i("LENGTH", Base64.decode(ex.getRequestText(), Base64.DEFAULT).length + "");
                    gatt.writeCharacteristic(characteristic);


                }

            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                gatt.disconnect();
            }
        };



        device.connectGatt(context, true, getcb);

    }

    public GattGetRequestHandler(BluetoothDevice d, BluetoothGattCharacteristic bgc, Context c) {
        device = d;
        characteristic = bgc;
        context = c;
    }

}
