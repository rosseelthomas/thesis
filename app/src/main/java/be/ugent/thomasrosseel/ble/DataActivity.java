package be.ugent.thomasrosseel.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Collection;

public class DataActivity extends AppCompatActivity {

    private Characteristic characteristic;
    private BLEEventListener listener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String res =(String) extras.get("resource");
            characteristic = (Characteristic) MyApplication.getResource(res);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_data, menu);
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

    public void handleGET(View v){

        new Runnable() {
            @Override
            public void run() {
                TextView tv = (TextView)findViewById(R.id.txtGET);
                tv.setText("");
                byte[] read = characteristic.read();

                String s="";
                if(read==null){

                    tv.setText("communication problem");
                    return;
                }
                for(byte b : read){
                    int i = b & 0xFF;
                    s+=i;
                }
                tv.setText(s);
            }
        }.run();



    }

    public void handlePOST(View v){
        byte[] bytes = new byte[1];
        bytes[0] = 1;

        characteristic.write(bytes);
    }

    public void btnNotifyStart(View v){
        listener = new BLEEventListener() {
            @Override
            public void onEvent(Object c) {


                byte[] data = (byte[]) c;
                final TextView txt = (TextView)findViewById(R.id.txtNotify);
                String s="";
                if(data==null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt.setText("communication problem");
                        }
                    });

                    return;
                }
                for(byte b : data){
                    int i = b & 0xFF;
                    s+=i;
                }
                final String str = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        txt.setText(str);
                    }
                });
            }
        };
        characteristic.startNotify(listener);
    }

    public void btnNotifyStop(View v){
        characteristic.stopNotify(listener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.setAppContext(this);
    }
}
