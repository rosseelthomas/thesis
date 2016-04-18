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
import android.provider.SyncStateContract;
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
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.BlockOption;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.EmptyMessage;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Option;
import org.eclipse.californium.core.coap.OptionNumberRegistry;
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
    private ArrayList<String> macs;
    private ArrayList<BLEProxyDevice> alldevices;
    private ArrayList<BLEDevice> phydevices;
    private DeviceResource stateres;
    private String state = "ONLINE";
    private DeviceResource bleproxy;

    private List<String> observing_ips;
    private HashMap<String, BLEProxyDeviceAdapter> proxyAdapterMap;

    private boolean ble_active = true;



    CoapServer srv;
    CoapClient client;

    private static final int COAP_PORT = NetworkConfig.getStandard().getInt(NetworkConfig.Keys.COAP_PORT);
    private static final int COAP_MULTICAST_PORT = 12345;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;



    private Timer ttl_timer;
    private TimerTask ttl_task = new TimerTask() {
        @Override
        public void run() {
            for(BLEProxyDevice d : (ArrayList<BLEProxyDevice>) alldevices.clone()){
                if(d.getTtl()<0){
                    alldevices.remove(d);
                    ListView l = (ListView) findViewById(R.id.list);
                    final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            arradapter.clear();
                            arradapter.addAll(getDiscoveredDevices());


                        }
                    });

                    //srv.remove()
                    if(phydevices.contains(d)){
                        phydevices.remove(d);

                        URI uri = URI.create(d.getPath());
                        String res = uri.getPath().split("/")[1];
                        srv.remove(srv.getRoot().getChild(res));
                    }

                    for(BLEProxyDeviceAdapter adap : getProxyAdapterCollection()){
                        if(adap.getAllDevices().contains(d)){
                            adap.removeDevice(d);
                        }
                    }

                }

                if(d instanceof BLEDevice && d.getStatus().startsWith("PROXY") && ((BLEDevice) d).getProxyTtl()<0){
                    d.setStatus("DISCOVERED");
                }

            }

        }
    };

    private TimerTask check_coap_servers = new TimerTask() {
        @Override
        public void run() {

            Set<String> hosts = new HashSet<>();
            for(BLEProxyDevice d : (ArrayList<BLEProxyDevice>) alldevices.clone()){

                URI u = URI.create(d.getPath());
                String host = u.getHost();
                //add host to set
                hosts.add(host);
            }

            //ping all hosts, if certain host reachable, adapt ttl

            for(String host : hosts){
                CoapClient c = new CoapClient("coap://"+host+":"+COAP_PORT);
                if(c.ping(5000)){
                    //adapt ttl
                    for(BLEProxyDevice d : (ArrayList<BLEProxyDevice>) alldevices.clone()){

                        URI u = URI.create(d.getPath());
                        String my_ip = getIPAddress(true);
                        if(u.getHost().equals(host) && !u.getHost().equals(my_ip)){
                            d.setTtl(60);
                            ListView l = (ListView) findViewById(R.id.list);
                            final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    arradapter.clear();
                                    arradapter.addAll(getDiscoveredDevices());


                                }
                            });
                        }

                    }
                }
            }



        }
    };

    private TimerTask discover_coap_servers = new TimerTask() {
        @Override
        public void run() {
            discoverCOAP();


        }
    };




    public BleListActivity() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        srv.stop();
        srv.destroy();
        if(ble_active)
            mLEScanner.stopScan(mScanCallback);
        if (broadcast != null)
            broadcast.close();

        if (multicastLock != null)
            multicastLock.release();

        mScanning = false;

    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanFailed(int errorCode) {
            Log.d("ble","scan failed");
            BLEScan();
        }

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();

            if (result.getDevice().getName() != null ) {


                Log.i("ble-scan", "device added");

                //devices.add(result.getDevice());


                //final BLEDevice scanned_device = new BLEDevice(result.getDevice());
                //arradapter.insert(scanned_device, arradapter.getCount());


                BLEProxyDevice phydevice = getPHYDeviceByMAC(result.getDevice().getAddress());
                if(phydevice instanceof COAPDevice){
                    alldevices.remove(phydevice);
                    for(BLEProxyDeviceAdapter adap : getProxyAdapterCollection()){
                        if(adap.getAllDevices().contains(phydevice)){
                            adap.removeDevice(phydevice);
                        }
                    }
                    phydevice=null;

                }
                if(phydevice == null){//else adapt ttl
                    String devicename = result.getDevice().getName();
                    String devicebase = devicename;
                    int suffix = 1;
                    while (containsResource(srv.getRoot().getChildren(), devicename)) {
                        devicename = devicebase + suffix;
                        suffix++;
                    }
                    devicename  = devicename.replaceAll(";","\\\\;");
                    devicebase  = devicebase.replaceAll(";","\\\\;");


                    BLEResource bler = (BLEResource) srv.getRoot().getChild("ble");
                    bler.addDevice(devicename);
                    bler.addDevice(result.getDevice().getAddress());

                    BLEDevice bpd = new BLEDevice(result.getDevice(), "coap://"+getIPAddress(true)+":5683/"+devicename, 120);
                    if(!alldevices.contains(bpd)){
                        alldevices.add(bpd);
                        bleproxy.changed();
                        ListView l = (ListView) findViewById(R.id.list);
                        final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                arradapter.clear();
                                arradapter.addAll(getDiscoveredDevices());


                            }
                        });
                        getAdapter(bpd).observeAll();
                    }else{
                        bpd = (BLEDevice)getPHYDeviceByMAC(result.getDevice().getAddress());


                    }
                    if(!phydevices.contains(bpd)){
                        phydevices.add(bpd);
                        bpd.notifyStatus();

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
                    DeviceResource statusResource = new DeviceResource("status", "status");
                    final BLEDevice finalBpd = bpd;
                    statusResource.setRequestHandler(new RequestHandler() {
                        @Override
                        public void handleGET(CoapExchange ex) {
                            ex.respond(finalBpd.getStatus());
                        }

                        @Override
                        public void handlePUT(CoapExchange ex) {

                        }
                    });
                    statusResource.setObservable(true);
                    statusResource.setObserveType(CoAP.Type.CON);
                    statusResource.getAttributes().setObservable();

                    finalBpd.setStatusresource(statusResource);
                    t.add(statusResource);



                    RequestHandler req = new RequestHandler() {
                        @Override
                        public void handleGET(final CoapExchange ex) {
                            ex.accept();
                            if(!finalBpd.getStatus().startsWith("PROXY")){
                                //als deze resource al subresources heeft, gewoon de namen teruggeven, anders eerst deze resources aanmaken
                                Collection<Service> services = finalBpd.discoverServices();

                                if(!t.isExposed()){
                                    t.expose();

                                    for(Service s : services){
                                        int uuid = (int) (s.getUUID().getMostSignificantBits() >> 32);
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
                                        DeviceResource service_resource = new DeviceResource(g_service.getUri(), g_service.getDescription());



                                        for (final Characteristic c : s.discoverCharacteristics()) {


                                            int c_uuid = (int) (c.getUUID().getMostSignificantBits() >> 32);
                                            Gatt g_char = GattTranslate.getInstance().getGatt(c_uuid);
                                            if (g_char == null) {
                                                int suff = 1;
                                                String gattname = "others";
                                                String gattbase = gattname;
                                                while (containsResource(service_resource.getChildren(), gattname)) {
                                                    gattname = gattbase + suff;
                                                    suff++;
                                                }
                                                g_char = new Gatt(uuid, gattbase, gattname);
                                            }

                                            final DeviceResource ch = new DeviceResource(g_char.getUri(), g_char.getDescription());


                                            RequestHandler h = new RequestHandler() {
                                                @Override
                                                public void handleGET(CoapExchange ex) {
                                                    ex.accept();


                                                    finalBpd.discoverServices();
                                                    ex.respond(CoAP.ResponseCode.CONTENT, c.read());




                                                }

                                                @Override
                                                public void handlePUT(CoapExchange ex) {
                                                    ex.accept();

                                                    finalBpd.discoverServices();
                                                    c.write(ex.getRequestPayload());



                                                }
                                            };
                                            ch.setRequestHandler(h);

                                            if (!containsResource(service_resource.getChildren(), ch.getName())) {
                                                service_resource.add(ch);
                                            }


                                        }
                                        t.add(service_resource);

                                    }
                                }
                                String services_string="";
                                for(Resource res : t.getChildren() ){
                                    if(!res.getName().equals("status") && !res.getName().equals("mac")){

                                        for(Resource char_res : res.getChildren()){
                                            services_string+=res.getName()+"/"+char_res.getName()+";";
                                        }


                                    }
                                }
                                ex.respond(services_string);
                            }else{


                                Response r = Response.createResponse(ex.advanced().getRequest(), CoAP.ResponseCode.CONTENT);
                                r.getOptions().addOption(new Option(OptionNumberRegistry.REDIRECT, finalBpd.getProxy_ip()));
                                r.setPayload("PROXY");
                                ex.respond(r);
                            }


                        }


                        @Override
                        public void handlePUT(CoapExchange ex) {

                        }
                    };
                    t.setRequestHandler(req);
                    if (!containsResource(srv.getRoot().getChildren(), t.getName())) {

                        srv.add(t);
                        //srv.getRoot().getChild("macs").add(new DeviceResource(result.getDevice().getAddress(),result.getDevice().getAddress()));

                        //broadcast that we discovered a new ble device, broadcast devicebase, mac address and path
                        sendBroadcast(devicebase + ";" + result.getDevice().getAddress() + ";" + "/" + devicename);

                        //macs.add(result.getDevice().getAddress());
                    }
                }else{
                    BLEProxyDevice searched = getPHYDeviceByMAC(result.getDevice().getAddress());

                    if(searched == null ){

                        searched = searchDeviceByIP(result.getDevice().getAddress(), getIPAddress(true));

                        phydevices.add((BLEDevice)searched);

                    }



                    searched.setTtl(120);
                }

            }

            ListView l = (ListView) findViewById(R.id.list);
            final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    arradapter.clear();
                    arradapter.addAll(getDiscoveredDevices());

                }
            });

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_list);

        ListView l = (ListView) findViewById(R.id.list);
        ArrayList<BLEProxyDeviceAdapter> vals = new ArrayList<>();
        ArrayAdapter<BLEProxyDeviceAdapter> arradapter = new ArrayAdapter<BLEProxyDeviceAdapter>(this, android.R.layout.simple_list_item_1, android.R.id.text1, vals);

        l.setAdapter(arradapter);


        l.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ListView l = (ListView) findViewById(R.id.list);
                ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
                BLEProxyDeviceAdapter ad = arradapter.getItem(position);
                //ad.connect();

                Intent intent = new Intent(getBaseContext(), DeviceActivity.class);

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
           ble_active=false;
            setTitle("CoAP ONLY "+getIPAddress(true));
        }else{
            mLEScanner = adapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<ScanFilter>();
            setTitle("BLE-CoAP "+getIPAddress(true));
        }


        observing_ips = new ArrayList<>();
        srv = new CoapServer();
        Resource bleres = new BLEResource("ble");
        srv.add(bleres);
         bleproxy = new DeviceResource("ble-proxy","ble-proxy");
        bleproxy.setRequestHandler(new RequestHandler() {
            @Override
            public void handleGET(CoapExchange ex) {
                String s = "";
                for (BLEProxyDevice d : alldevices) {
                    s += d.toResource();
                }
                ex.respond(s);

                if (!observing_ips.contains(ex.getSourceAddress().getHostAddress())) {
                    //start observe
                    startProxyObserve(ex.getSourceAddress().getHostAddress());
                }
            }

            @Override
            public void handlePUT(CoapExchange ex) {

            }
        });
        bleproxy.setObservable(true);
        bleproxy.getAttributes().setObservable();

    srv.add(bleproxy);


        final DeviceResource bleproxy_ = new DeviceResource("ble-proxy_","ble-proxy_");
        bleproxy_.setRequestHandler(new RequestHandler() {
            @Override
            public void handleGET(CoapExchange ex) {
                String s = "";
                for (BLEProxyDevice d : alldevices) {
                    s += d.toResource();
                    s += d.getClass().getName() + ";";
                }
                ex.respond(s);


            }

            @Override
            public void handlePUT(CoapExchange ex) {

            }
        });
        bleproxy_.setObservable(true);
        bleproxy_.getAttributes().setObservable();

        srv.add(bleproxy_);





        stateres = new DeviceResource("state","state");
        stateres.setRequestHandler(new RequestHandler() {
            @Override
            public void handleGET(CoapExchange ex) {
                ex.respond(state);
            }

            @Override
            public void handlePUT(CoapExchange ex) {

            }
        });
        stateres.setObservable(true);

        srv.add(stateres);




        try {
            InetSocketAddress bindToAddress = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), COAP_PORT);


            srv.addEndpoint(new CoapEndpoint(bindToAddress));
            Log.i("ip", "listening on : " + "0.0.0.0" + " AT " + COAP_PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        srv.start();
        macs = new ArrayList<>();
        alldevices = new ArrayList<>();
        phydevices = new ArrayList<>();
        proxyAdapterMap = new HashMap<>();
        //srv.getRoot().add(new DeviceResource("macs", "ble macs"));
        ttl_timer = new Timer();
        ttl_timer.scheduleAtFixedRate(ttl_task, 0, 5000);
        //ttl_timer.scheduleAtFixedRate(check_coap_servers,0,5000);

        discoverCOAP();

        ttl_timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ListView l = (ListView) findViewById(R.id.list);
                final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        arradapter.clear();
                        arradapter.addAll(getDiscoveredDevices());


                    }
                });

                bleproxy_.changed();
            }
        },0,1000);

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
        /*mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mLEScanner.stopScan(mScanCallback);
                Log.i("ble-scan", "scan stopped");
                BLEScan();
            }
        }, SCAN_PERIOD);*/

        mScanning = true;
        mLEScanner.startScan(filters, settings, mScanCallback);

        Log.i("ble-scan", "scan started");
    }

    public void refresh_click(View v) {
        /*
        devices.clear();
        ListView l = (ListView) findViewById(R.id.list);
        ArrayAdapter<String> arradapter = (ArrayAdapter<String>) l.getAdapter();
        arradapter.clear();
        BLEScan();*/

        ListView l = (ListView) findViewById(R.id.list);
        final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                arradapter.clear();
                arradapter.addAll(getDiscoveredDevices());

            }
        });

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
       // discoverCOAP();


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




    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeRegistrationListener();
        registerService(COAP_PORT);
        MyApplication.setAppContext(this);
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiManager.MulticastLock multicastLock = wm.createMulticastLock("multicast-lock");
        multicastLock.acquire();

        if (multicastLock.isHeld()) {
            Log.i("lock", "lock acquired");
        }

        devices.clear();
        ListView l = (ListView) findViewById(R.id.list);
        ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
        arradapter.clear();
        if(ble_active)
             BLEScan();
      //  discoverCOAP();




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
                                    final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();
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
                final ArrayAdapter<BLEProxyDeviceAdapter> arradapter = (ArrayAdapter<BLEProxyDeviceAdapter>) l.getAdapter();

                try {

                    URI uri = new URI("coap", null, "255.255.255.255", COAP_PORT, "/ble-proxy", null, null);

                    InetSocketAddress addr = new InetSocketAddress(0);



                    final Request coapRequest = Request.newGet();
                    coapRequest.setURI(uri);

                    coapRequest.setSource(addr.getAddress());

                    coapRequest.setType(CoAP.Type.NON);
                    coapRequest.setMulticast(true);


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


                            Log.i("parsed", "parsed");
                            //final String[] splits = txt.split("(?<!\\\\);");

                            List<BLEProxyDevice> bleProxyDevices;

                            try{
                                bleProxyDevices = BleProxyParser.parse(txt, resp.getSource().getHostAddress());
                            }catch (Exception e){
                                bleProxyDevices = new ArrayList<>();
                            }





                            for(BLEProxyDevice b : bleProxyDevices){
                                BLEProxyDevice dev = searchDevice(b.getMac(), b.getPath());
                                if(dev == null && !URI.create(b.getPath()).getHost().equals(getIPAddress(true))){
                                    //bluetooth device nog niet aanwezig
                                    dev = b;
                                    if(b.getTtl()>=0){

                                        alldevices.add(dev);
                                        URI u = URI.create(dev.getPath());
                                        BLEProxyDevice phydev = getPHYDeviceByMAC(b.getMac());
                                        if(phydev!=null && !phydevices.contains(b)){
                                            //als we dit device ook lokaal vinden,maar het niet het lokale device is, zetten we een observerelatie op

                                            ((COAPDevice) b).observe(getAdapter(b));

                                        }
                                    }

                                }else if(dev!= null && !URI.create(dev.getPath()).getHost().equals(getIPAddress(true))){
                                    //bluetooth device aanwezig, bereikbaar via netwerk (niet lokaal)
                                    URI u = URI.create(dev.getPath());
                                    if(u.getHost().equals(resp.getSource().getHostAddress())){
                                        dev.setTtl(b.getTtl());
                                    }
                                    ((COAPDevice) dev).observe(getAdapter(dev));

                                }

                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    arradapter.clear();
                                    arradapter.addAll(getDiscoveredDevices());

                                }
                            });


                            resp = null;
                        }else{
                            resp = coapRequest.send().waitForResponse(2000);
                        }



                    }
                    //Log.d("resp",resp.toString());


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //check if we have to observe some resources
//                for(BLEProxyDeviceAdapter adapter : getProxyAdapterCollection()){
//                    if(adapter.isPhysical()){
//                        for(BLEProxyDevice b : adapter.getAllDevices()){
//                            if(b instanceof COAPDevice){
//                                COAPDevice coap = (COAPDevice) b;
//                                coap.observe(adapter);
//                            }
//                        }
//                    }
//                }

            }

        }.run();





    }

    public void sendBroadcast(String messageStr) {
        // Hack Prevent crash (sending should be done using an async task)
        StrictMode.ThreadPolicy policy = new   StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            //Open a random port to send the package
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] sendData = messageStr.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), 6666);
            socket.send(sendPacket);
            System.out.println(getClass().getName() + "Broadcast packet sent to: " + "255.255.255.255");
        } catch (IOException e) {
            Log.d("broadcast", "IOException: " + e.getMessage());
        }
    }

    private ArrayList<BLEProxyDeviceAdapter> getDiscoveredDevices(){
        ArrayList<BLEProxyDeviceAdapter> ds = new ArrayList<>();
        for(BLEProxyDeviceAdapter d : getProxyAdapterCollection()){
            ds.add(d);
        }
        return ds;
    }

    private BLEProxyDevice searchDevice(String mac, String path){
        for(BLEProxyDevice d : alldevices){
            if(d.getMac().equals(mac) && d.getPath().equals(path)){
                return d;
            }
        }
        return null;
    }

    private BLEProxyDevice searchDeviceByIP(String mac, String ip){
        for(BLEProxyDevice d : alldevices){
            URI u = URI.create(d.getPath());
            if(d.getMac().equals(mac) && u.getHost().equals(ip)){
                return d;
            }
        }
        return null;
    }



    private BLEProxyDevice getPHYDeviceByMAC(String mac){
        for(BLEProxyDevice d : alldevices){
            URI u = URI.create(d.getPath());
            if(d.getMac().equals(mac) && u.getHost().equals(getIPAddress(true))) return d;
        }
        return null;
    }


    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    private String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }


    private Collection<BLEProxyDeviceAdapter> getProxyAdapterCollection(){



        for(BLEProxyDevice device : (ArrayList<BLEProxyDevice>)alldevices.clone()){

            if(proxyAdapterMap.get(device.getMac())==null){
                //nog geen adapter voor deze mac
                proxyAdapterMap.put(device.getMac(), new BLEProxyDeviceAdapter());

            }

            if(!proxyAdapterMap.get(device.getMac()).getAllDevices().contains(device)){
                //adapter bestaat al, maar device zit er nog niet in
                proxyAdapterMap.get(device.getMac()).addDevice(device);
            }


        }

        for(String mac : proxyAdapterMap.keySet()){
            if(proxyAdapterMap.get(mac).getAllDevices().isEmpty()){
                proxyAdapterMap.remove(mac);
            }
        }


        return proxyAdapterMap.values();
    }


    private BLEProxyDeviceAdapter getAdapter(BLEProxyDevice d){
        for(BLEProxyDeviceAdapter adap : getProxyAdapterCollection()){
            if(adap.getAllDevices().contains(d)){
                return adap;
            }
        }
        return null;

    }

    private void startProxyObserve(final String ip){
        CoapClient coapClient = new CoapClient(ip+"/ble-proxy");
        observing_ips.add(ip);
        coapClient.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                List<BLEProxyDevice> bleProxyDeviceList;
                try{
                    bleProxyDeviceList = BleProxyParser.parse(response.getResponseText(), response.advanced().getSource().getHostAddress());
                }catch(Exception e){
                    bleProxyDeviceList = new ArrayList<>();
                }

                for(BLEProxyDevice bleProxyDevice : bleProxyDeviceList){
                    BLEProxyDevice find = searchDevice(bleProxyDevice.getMac(), bleProxyDevice.getPath());
                    if(find == null){
                        if(bleProxyDevice.getTtl()>0){
                            alldevices.add(bleProxyDevice);
                            BLEProxyDevice phydev = getPHYDeviceByMAC(bleProxyDevice.getMac());
                            if(phydev!=null && !phydevices.contains(bleProxyDevice)){
                                //als we dit device ook lokaal vinden,maar het niet het lokale device is, zetten we een observerelatie op
                                ((COAPDevice) bleProxyDevice).observe(getAdapter(bleProxyDevice));

                            }
                        }
                    }else if(!phydevices.contains(find)){
                        //bluetooth device aanwezig, bereikbaar via netwerk (niet lokaal)
                        URI u = URI.create(find.getPath());
                        if(u.getHost().equals(response.advanced().getSource().getHostAddress())){
                            find.setTtl(bleProxyDevice.getTtl());
                        }

                    }
                }

            }

            @Override
            public void onError() {
                observing_ips.remove(ip);
            }
        });
    }

}


