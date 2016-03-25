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

public class ServiceActivity extends AppCompatActivity {

    private Device device;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        ListView v = (ListView) findViewById(R.id.services_list);


        v.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getBaseContext(), CharacteristicActivity.class);
                ListView l = (ListView) findViewById(R.id.services_list);
                ArrayAdapter<Service> arradapter = (ArrayAdapter<Service>) l.getAdapter();
                intent.putExtra("resource", "SERVICE");
                MyApplication.putResource("SERVICE",arradapter.getItem(position));
                startActivity(intent);
            }
        });
        ArrayAdapter<Service> adapter = new ArrayAdapter<Service>(this, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<Service>());
        v.setAdapter(adapter);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String res =(String) extras.get("resource");
            device = (Device) MyApplication.getResource(res);

            if(!device.connect()){
                device.disconnect();

                finish();
                Log.i("service","connection failed");
                return;

            }
            adapter.addAll(device.discoverServices());
            device.disconnect();


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_, menu);
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

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.setAppContext(this);
    }
}
