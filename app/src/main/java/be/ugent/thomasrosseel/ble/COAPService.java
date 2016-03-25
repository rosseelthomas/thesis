package be.ugent.thomasrosseel.ble;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Created by thomasrosseel on 9/03/16.
 */
public class COAPService implements Service {

    private ArrayList<Characteristic> characteristicArrayList;
    private String name,path;

    public COAPService(String name, String path) {
        characteristicArrayList = new ArrayList<>();
        this.name = name;
        this.path = path;
    }

    @Override
    public Collection<Characteristic> discoverCharacteristics() {
        return characteristicArrayList;
    }

    @Override
    public UUID getUUID() {
        return null;
    }

    public void addCharacteristic(Characteristic c){
        characteristicArrayList.add(c);
    }

    @Override
    public String toString() {
        return name;
    }
}
