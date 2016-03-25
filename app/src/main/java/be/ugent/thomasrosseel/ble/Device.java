package be.ugent.thomasrosseel.ble;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by thomasrosseel on 3/03/16.
 */
public interface Device extends Serializable {

    public boolean connect();
    public Collection<Service> discoverServices();
    public boolean disconnect();




}
