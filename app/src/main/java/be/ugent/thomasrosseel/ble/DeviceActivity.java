package be.ugent.thomasrosseel.ble;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;

public class DeviceActivity extends AppCompatActivity {

    private BLEProxyDeviceAdapter bleProxyDeviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ListView v = (ListView) findViewById(R.id.devices_list);

        ArrayAdapter<BLEProxyDevice> adapter = new ArrayAdapter<BLEProxyDevice>(this, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<BLEProxyDevice>());
        v.setAdapter(adapter);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String res =(String) extras.get("resource");
            bleProxyDeviceAdapter = (BLEProxyDeviceAdapter) MyApplication.getResource(res);
            for(BLEProxyDevice d : bleProxyDeviceAdapter.getAllDevices()){
                adapter.add(d);
            }


        }

        v.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView v = (ListView) findViewById(R.id.devices_list);
                ArrayAdapter<BLEProxyDevice> arradapter = (ArrayAdapter<BLEProxyDevice>) v.getAdapter();

                Intent intent = new Intent(getBaseContext(), ServiceActivity.class);

                intent.putExtra("resource", "DEVICE");
                MyApplication.putResource("DEVICE", arradapter.getItem(position));
                startActivity(intent);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device, menu);
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
}
