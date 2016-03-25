package be.ugent.thomasrosseel.ble;

import java.util.UUID;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public interface Characteristic {

    public byte[] read();
    public void write(byte[] bytes);
    public UUID getUUID();
    public void startNotify(BLEEventListener listener);
    public void stopNotify(BLEEventListener listener);
}
