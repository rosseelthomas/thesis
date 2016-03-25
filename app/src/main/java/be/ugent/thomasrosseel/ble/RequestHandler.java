package be.ugent.thomasrosseel.ble;

import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Created by thomasrosseel on 23/02/16.
 */
public interface RequestHandler {

    void handleGET(CoapExchange ex);
    void handlePUT(CoapExchange ex);

}
