package be.ugent.thomasrosseel.ble;

/**
 * Created by thomasrosseel on 24/02/16.
 */
public class Gatt {

    private int uuid;
    private String description;
    private String uri;

    public Gatt(int uuid, String description, String uri) {
        this.uuid = uuid;
        this.description = description;
        this.uri = uri;
    }

    public int getUuid() {
        return uuid;
    }

    public void setUuid(int uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
