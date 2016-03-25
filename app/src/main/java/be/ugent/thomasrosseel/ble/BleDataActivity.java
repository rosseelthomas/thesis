package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.net.nsd.NsdManager;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.UUID;

public class BleDataActivity extends AppCompatActivity {

    private BluetoothDevice device;
    private BluetoothGatt mBluetoothGatt;
    private UUID ud = UUID.fromString("00001523-1212-EFDE-1523-785FEABCD123");
    private String mServiceName;
    private NsdManager.RegistrationListener mRegistrationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_data);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device =(BluetoothDevice) extras.get("DEVICE");
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {

                        Log.i("gatt", "Connected to GATT server.");
                        Log.i("gatt", "Attempting to start service discovery:" +
                                mBluetoothGatt.discoverServices());
                        Looper.prepare();
                        Toast.makeText(BleDataActivity.this,"connection established",Toast.LENGTH_SHORT);


                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Log.i("gatt", "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    for(BluetoothGattService s : mBluetoothGatt.getServices()){

                    }
                }


            };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ble_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void send_0(View v){
        writeCharacteristic(false);
    }

    public void send_1(View v){
        writeCharacteristic(true);
    }

    public boolean writeCharacteristic(Boolean b){

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e("gatt", "lost connection");
            return false;
        }



        BluetoothGattService Service = mBluetoothGatt.getService(ud);

        if (Service == null) {
            Log.e("gatt", "service not found!");
            Toast.makeText(this, "service not found", Toast.LENGTH_LONG).show();
            return false;
        }
        BluetoothGattCharacteristic charac = Service
                .getCharacteristic(UUID.fromString("00001525-1212-EFDE-1523-785FEABCD123"));
        if (charac == null) {
            Toast.makeText(this, "characteristic not found", Toast.LENGTH_LONG).show();
            Log.e("gatt", "char not found!");
            return false;
        }

        byte[] value = new byte[1];
        int i = b ? 1 : 0;
        value[0] = (byte) (i & 0xFF);
        charac.setValue(value);

        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.i("write","wrote "+i);
        return status;
    }
}
