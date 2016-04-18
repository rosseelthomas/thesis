package be.ugent.thomasrosseel.ble;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;

/**
 * Created by thomasrosseel on 23/02/16.
 */
public class DeviceResource extends CoapResource {

    private RequestHandler requestHandler;
    private boolean exposed = false;

    public DeviceResource(String id, String name){
        super(id);
        getAttributes().setTitle(name);
    }

    public void setRequestHandler(RequestHandler handler){
        requestHandler = handler;

    }

    public void expose(){
        exposed = true;
    }

    public boolean isExposed(){
        return exposed;
    }

    @Override
    public void handleGET(CoapExchange exchange) {

        requestHandler.handleGET(exchange);
    }

    @Override
    public void handlePUT(CoapExchange exchange) {
        requestHandler.handlePUT(exchange);
    }
}
