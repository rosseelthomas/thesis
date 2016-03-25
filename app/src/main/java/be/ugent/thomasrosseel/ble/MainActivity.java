package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    BluetoothAdapter adapter;
    private boolean mScanning;
    private Handler mHandler = new Handler();
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mBluetoothGatt;
    private UUID ud = UUID.fromString("00001523-1212-EFDE-1523-785FEABCD123");
    private String mServiceName;
    private NsdManager.RegistrationListener mRegistrationListener;



    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GattTranslate.setContext(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bluetoothManager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        mLEScanner = adapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();




    }


    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();


        Intent intent = new Intent(this, BleListActivity.class);

        startActivity(intent);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void searchBLE(View v){

        writeCharacteristic();


    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            if(result.getDevice().getName() != null && result.getDevice().getName().equals("thble")){

                mLEScanner.stopScan(mScanCallback);
                Log.i("ble-scan", "device found, scan stopped");

                 mBluetoothGatt = result.getDevice().connectGatt(MainActivity.this, false, mGattCallback);


                //mBluetoothGatt.disconnect();
            }


            //connectToDevice(btDevice);
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
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };


    public boolean writeCharacteristic(){

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e("gatt", "lost connection");
            return false;
        }



        BluetoothGattService Service = mBluetoothGatt.getService(ud);

        if (Service == null) {
            Log.e("gatt", "service not found!");
            return false;
        }
        BluetoothGattCharacteristic charac = Service
                .getCharacteristic(UUID.fromString("00001525-1212-EFDE-1523-785FEABCD123"));
        if (charac == null) {
            Log.e("gatt", "char not found!");
            return false;
        }

        byte[] value = new byte[1];
        CheckBox cb = (CheckBox)findViewById(R.id.checkBox);
        int i = cb.isChecked() ? 1 : 0;
        value[0] = (byte) (i & 0xFF);
        charac.setValue(value);

        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        Log.i("write","wrote "+i);
        return status;
    }


    public void connectclick(View v){
        Log.i("ble-log","BLE SEARCHING....");



        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mLEScanner.stopScan(mScanCallback);
                Log.i("ble-scan","scan stopped");
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mLEScanner.startScan(filters, settings, mScanCallback);
        Log.i("ble-scan", "scan started");
    }

    public void disconnectclick(View v){

        mBluetoothGatt.disconnect();

    }


    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName("THOMAS-BLE");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setPort(port);


        NsdManager mNsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }


    public void unregisterService(){
        NsdManager mNsdManager = (NsdManager)getSystemService(Context.NSD_SERVICE);
        mNsdManager.unregisterService(mRegistrationListener);

    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

}
