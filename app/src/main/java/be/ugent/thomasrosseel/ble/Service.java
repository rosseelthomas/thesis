package be.ugent.thomasrosseel.ble;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public interface Service {
    public Collection<Characteristic> discoverCharacteristics();
    public UUID getUUID();

}
