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
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.interceptors.MessageInterceptor;
import org.eclipse.californium.core.network.stack.BlockwiseStatus;
import org.eclipse.californium.core.observe.ObserveManager;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObservingEndpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;

import java.lang.reflect.Array;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BleListActivity extends AppCompatActivity {

    private BluetoothAdapter adapter;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private boolean mScanning;
    private Handler mHandler = new Handler();
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private String mServiceName;
    private NsdManager.RegistrationListener mRegistrationListener;
    public static final String SERVICE_TYPE = "_coap._udp.";
    private DatagramSocket broadcast;
    private WifiManager.MulticastLock multicastLock;
    private long timer;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private  boolean discovering;


    CoapServer srv;
    CoapClient client;

    private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    private static final int COAP_MULTICAST_PORT = 12345;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;






    public BleListActivity() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        srv.stop();
        srv.destroy();

    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            if (result.getDevice().getName() != null && !devices.contains(result.getDevice())) {


                Log.i("ble-scan", "device added");

                devices.add(result.getDevice());
                ListView l = (ListView) findViewById(R.id.list);
                ArrayAdapter<Device> arradapter = (ArrayAdapter<Device>) l.getAdapter();
                final BLEDevice scanned_device = new BLEDevice(result.getDevice());
                arradapter.insert(scanned_device, arradapter.getCount());



                if(!containsResource(srv.getRoot().getChild("macs").getChildren(),result.getDevice().getAddress())){
                    String devicename = scanned_device.getDevice().getName();
                    String devicebase = devicename;
                    int suffix = 1;
                    while (containsResource(srv.getRoot().getChildren(), devicename)) {
                        devicename = devicebase + suffix;
                        suffix++;
                    }


                    final DeviceResource t = new DeviceResource(devicename, devicename);
                    DeviceResource macResource = new DeviceResource("mac", "mac address");
                    macResource.setRequestHandler(new RequestHandler() {
                        @Override
                        public void handleGET(CoapExchange ex) {
                            ex.respond(result.getDevice().getAddress());
                        }

                        @Override
                        public void handlePUT(CoapExchange ex) {

                        }
                    });
                    t.add(macResource);
                    RequestHandler req = new RequestHandler() {
                        @Override
                        public void handleGET(final CoapExchange ex) {
                            ex.accept();

                            scanned_device.connect();
                            Collection<Service> services = scanned_device.discoverServices();
                            scanned_device.disconnect();



                            for (Service service : services) {


                                if(t.getChildren().size()>services.size()){
                                    continue;
                                }

                                int uuid = (int) (service.getUUID().getMostSignificantBits() >> 32);
                                Log.i("uuid", "" + uuid);


                                Gatt g_service = GattTranslate.getInstance().getGatt(uuid);




                                if (g_service == null) {
                                    int suff = 1;
                                    String gattname = "others";
                                    String gattbase = gattname;
                                    while (containsResource(t.getChildren(), gattname)) {
                                        gattname = gattbase + suff;
                                        suff++;
                                    }
                                    g_service = new Gatt(uuid, gattbase, gattname);
                                }

                                DeviceResource s = new DeviceResource(g_service.getUri(), g_service.getDescription());

                                for (final Characteristic c : service.discoverCharacteristics()) {


                                    int c_uuid = (int) (c.getUUID().getMostSignificantBits() >> 32);
                                    Gatt g_char = GattTranslate.getInstance().getGatt(c_uuid);
                                    if (g_char == null) {
                                        int suff = 1;
                                        String gattname = "others";
                                        String gattbase = gattname;
                                        while (containsResource(s.getChildren(), gattname)) {
                                            gattname = gattbase + suff;
                                            suff++;
                                        }
                                        g_char = new Gatt(uuid, gattbase, gattname);
                                    }

                                    final DeviceResource ch = new DeviceResource(g_char.getUri(), g_char.getDescription());

                                    ch.setObservable(true);
                                    ch.getAttributes().setObservable();

                                    RequestHandler h = new RequestHandler() {
                                        @Override
                                        public void handleGET(CoapExchange ex) {
                                            ex.accept();
                                            if(ex.getRequestOptions().getObserve()!=null && ex.getRequestOptions().getObserve() == 0){
                                                //user wants to observe
                                                ObserveManager observeManager = new ObserveManager();
                                                ObservingEndpoint remote = observeManager.findObservingEndpoint(new InetSocketAddress(ex.getSourceAddress(), ex.getSourcePort()));
                                                final ObserveRelation relation = new ObserveRelation(remote, ch, ex.advanced());
                                                remote.addObserveRelation(relation);
                                                ch.addObserveRelation(relation);
                                                ex.advanced().setRelation(relation);
                                                scanned_device.connect();
                                                scanned_device.discoverServices();
                                                c.startNotify(new BLEEventListener() {
                                                    @Override
                                                    public void onEvent(Object data) {
                                                        ch.changed();

                                                    }
                                                });
                                            }else if(ex.getRequestOptions().getObserve()!=null && ex.getRequestOptions().getObserve() == 1){
                                                c.stopNotify(null);
                                                scanned_device.disconnect();
                                            }else{
                                                scanned_device.connect();
                                                scanned_device.discoverServices();
                                                ex.respond(CoAP.ResponseCode.CONTENT, c.read());
                                                scanned_device.disconnect();
                                            }


                                        }

                                        @Override
                                        public void handlePUT(CoapExchange ex) {
                                            ex.accept();
                                            scanned_device.connect();
                                            scanned_device.discoverServices();
                                            c.write(ex.getRequestPayload());
                                            scanned_device.disconnect();


                                        }
                                    };
                                    ch.setRequestHandler(h);

                                    if (!containsResource(s.getChildren(), ch.getName())) {
                                        s.add(ch);
                                    }


                                }
                                if (!containsResource(t.getChildren(), s.getName())) {
                                    t.add(s);
                                }


                            }

                        }


                        @Override
                        public void handlePUT(CoapExchange ex) {

                        }
                    };
                    t.setRequestHandler(req);
                    if (!containsResource(srv.getRoot().getChildren(), t.getName())) {

                        srv.add(t);
                        srv.getRoot().getChild("macs").add(new DeviceResource(result.getDevice().getAddress(),result.getDevice().getAddress()));
                    }
                }

            }


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_list);

        ListView l = (ListView) findViewById(R.id.list);
        ArrayList<Device> vals = new ArrayList<>();
        ArrayAdapter<Device> arradapter = new ArrayAdapter<Device>(this, android.R.layout.simple_list_item_1, android.R.id.text1, vals);

        l.setAdapter(arradapter);


        l.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getBaseContext(), ServiceActivity.class);
                ListView l = (ListView) findViewById(R.id.list);
                ArrayAdapter<Device> arradapter = (ArrayAdapter<Device>) l.getAdapter();
                intent.putExtra("resource", "DEVICE");
                MyApplication.putResource("DEVICE", arradapter.getItem(position));
                startActivity(intent);
            }
        });



        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wm.createMulticastLock("multicast-lock");
        multicastLock.acquire();





        Logger logger = Logger.getLogger("be.ugent.thomasrosseel.ble");
        logger.setLevel(Level.ALL);


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
        srv = new CoapServer();




        try {
            InetSocketAddress bindToAddress = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), COAP_PORT);


            srv.addEndpoint(new CoapEndpoint(bindToAddress));
            Log.i("ip", "listening on : " + "0.0.0.0" + " AT " + COAP_PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        srv.start();
        srv.getRoot().add(new DeviceResource("macs", "ble macs"));


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ble_list, menu);
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


    private void BLEScan() {
        Log.i("ble-log", "BLE SEARCHING....");



        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mLEScanner.stopScan(mScanCallback);
                Log.i("ble-scan", "scan stopped");
            }
        }, SCAN_PERIOD);

        mScanning = true;
        mLEScanner.startScan(filters, settings, mScanCallback);
        Log.i("ble-scan", "scan started");
    }

    public void refresh_click(View v) {
        devices.clear();
        ListView l = (ListView) findViewById(R.id.list);
        ArrayAdapter<Device> arradapter = (ArrayAdapter<Device>) l.getAdapter();
        arradapter.clear();
        BLEScan();

/*
        //TESTING TIMES
        NsdManager mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        for(int i=0;i<100;++i){

            discovering = true;
            NSDScan();
            //discoverCOAP();
            while(discovering);
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);


        }
*/
        discoverCOAP();


    }

    private boolean containsResource(Collection<Resource> collection, String name) {
        for (Resource r : collection) {
            if (r.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterService();

        if (broadcast != null)
            broadcast.close();

        if (multicastLock != null)
            multicastLock.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeRegistrationListener();
        registerService(COAP_PORT);
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wm.createMulticastLock("multicast-lock");
        multicastLock.acquire();

        if (multicastLock.isHeld()) {
            Log.i("lock", "lock acquired");
        }

        devices.clear();
        ListView l = (ListView) findViewById(R.id.list);
        ArrayAdapter<Device> arradapter = (ArrayAdapter<Device>) l.getAdapter();
        arradapter.clear();
        BLEScan();
        discoverCOAP();


    }


    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("THOMAS-BLE");
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);

        NsdManager mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }


    public void unregisterService() {
        NsdManager mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
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


    public void NSDScan() {
        timer = System.currentTimeMillis();
        final NsdManager mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);


        final NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e("nsd", "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo) {
                Log.e("nsd", "Resolve Succeeded. " + serviceInfo);





                boolean is_same_host = false;

                for (InetAddress ipaddr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
                    // only binds to IPv4 addresses and localhost
                    if (ipaddr instanceof Inet4Address || ipaddr.isLoopbackAddress()) {

                        if (ipaddr.getHostAddress().equals(serviceInfo.getHost().getHostAddress())) {
                            Log.i("response", "not sending request to same host");
                            is_same_host = true;
                        }

                    }
                }

                if (!is_same_host) {
                    Log.i("response", "request to other host");
                    CoapClient c = new CoapClient(serviceInfo.getHost().getHostAddress()+"/.well-known/core");
                    final CoapResponse response = c.get();
                    String txt = response.getResponseText();

                    ParseResult p = COAPParser.parse(txt);
                    Log.i("parsed", "parsed");


                    for (final ParseResult child : p.getChildren()) {
                        boolean is_ble_device = false;
                        for (ParseResult subchild : child.getChildren()) {
                            if (subchild.getId().equals("mac")) {
                                is_ble_device = true;
                            }
                        }
                        if (is_ble_device) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ListView l = (ListView) findViewById(R.id.list);
                                    final ArrayAdapter<Device> arradapter = (ArrayAdapter<Device>) l.getAdapter();
                                    // arradapter.add(new COAPDevice());


                                }
                            });
                            Log.i("timer-nsd", "resource found in " + (System.currentTimeMillis() - timer) + " ms");
                            discovering = false;
                        }
                    }


                    if (serviceInfo.getServiceName().equals(mServiceName)) {
                        Log.d("nsd", "Same IP.");
                        return;
                    }

                }
            }
        };


        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d("nsd", "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d("nsd", "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d("nsd", "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d("nsd", "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("")) {
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e("nsd", "service lost " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i("nsd", "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("nsd", "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e("nsd", "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };

        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);


    }


    private void discoverCOAP() {
        timer = System.currentTimeMillis();
        new Runnable() {
            @Override
            public void run() {
                ListView l = (ListView) findViewById(R.id.list);
                final ArrayAdapter<Device> arradapter = (ArrayAdapter<Device>) l.getAdapter();

                try {

                    URI uri = new URI("coap", null, "255.255.255.255", COAP_PORT, "/.well-known/core", null, null);

                    InetSocketAddress addr = new InetSocketAddress(0);



                    final Request coapRequest = Request.newGet();
                    coapRequest.setURI(uri);

                    coapRequest.setSource(addr.getAddress());
                    //coapRequest.cancel();
                    coapRequest.setType(CoAP.Type.NON);
                    coapRequest.setMulticast(true);



                    CoapEndpoint e = new CoapEndpoint();

                    e.setExecutor(new ScheduledThreadPoolExecutor(10));
                    e.addInterceptor(new MessageInterceptor() {
                        @Override
                        public void sendRequest(Request request) {

                        }

                        @Override
                        public void sendResponse(Response response) {

                        }

                        @Override
                        public void sendEmptyMessage(EmptyMessage message) {

                        }

                        @Override
                        public void receiveRequest(Request request) {

                        }

                        @Override
                        public void receiveResponse(final Response response) {
                            boolean is_same_host = false;

                            for (InetAddress ipaddr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
                                // only binds to IPv4 addresses and localhost
                                if (ipaddr instanceof Inet4Address || ipaddr.isLoopbackAddress()) {

                                    if (ipaddr.getHostAddress().equals(response.getSource().getHostAddress())) {
                                        Log.i("response", "dropping response from same host");
                                        is_same_host = true;
                                    }

                                }
                            }

                            if (!is_same_host) {

                                Log.i("response", "response from other host");
                                final Response ui_response = response;
                                Log.i("resp", response.getPayloadString());
                                String txt = response.getPayloadString();


                                ParseResult p = COAPParser.parse(txt);
                                Log.i("parsed", "parsed");


                                for (final ParseResult child : p.getChildren()) {
                                    boolean is_ble_device = false;
                                    for (ParseResult subchild : child.getChildren()) {
                                        if (subchild.getId().equals("mac")) {
                                            is_ble_device = true;
                                        }
                                    }
                                    if (is_ble_device) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                arradapter.add(new COAPDevice(response.getSource().getHostAddress(), response.getSourcePort(), child.getId(), child.getTitle()));

                                            }
                                        });
                                        Log.i("timer-multicast", "resource found in " + (System.currentTimeMillis() - timer) + " ms");
                                    }
                                }

                            }

                        }

                        @Override
                        public void receiveEmptyMessage(EmptyMessage message) {

                        }
                    });

                    CoapClient c = new CoapClient(uri);

                    c.setEndpoint(e);
                    Response resp = coapRequest.send().waitForResponse(2000);
                    while(resp!=null){




                        boolean is_same_host = false;

                        for (InetAddress ipaddr : EndpointManager.getEndpointManager().getNetworkInterfaces()) {
                            // only binds to IPv4 addresses and localhost
                            if (ipaddr instanceof Inet4Address || ipaddr.isLoopbackAddress()) {

                                if (ipaddr.getHostAddress().equals(resp.getSource().getHostAddress())) {
                                    Log.i("response", "dropping response from same host");
                                    is_same_host = true;
                                }

                            }
                        }

                        if (!is_same_host) {

                            Log.i("response", "response from other host");

                            Log.i("resp", resp.getPayloadString());
                            String txt = resp.getPayloadString();


                            ParseResult p = COAPParser.parse(txt);
                            Log.i("parsed", "parsed");


                            for (final ParseResult child : p.getChildren()) {
                                boolean is_ble_device = false;
                                for (ParseResult subchild : child.getChildren()) {
                                    if (subchild.getId().equals("mac")) {
                                        is_ble_device = true;
                                    }
                                }
                                if (is_ble_device) {
                                    final Response finalResp = resp;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            arradapter.add(new COAPDevice(finalResp.getSource().getHostAddress(), finalResp.getSourcePort(), child.getId(), child.getTitle()));

                                        }
                                    });
                                    //Log.i("timer-multicast", "resource found in " + (System.currentTimeMillis() - timer) + " ms");
                                }
                            }

                        }




                        resp = coapRequest.send().waitForResponse(2000);






                    }
                    //Log.d("resp",resp.toString());


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }.run();





    }



}


