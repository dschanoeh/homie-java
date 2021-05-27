package io.github.dschanoeh.homie_java;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.HashMap;

public class Node {

    @Getter @Setter @NonNull private String name = "";
    @Getter private final String type;
    @Getter private final String id;
    private final Homie homie;
    private final HashMap<String, Property> properties = new HashMap<>();

    protected Node(Homie homie, String id, String type) {
        this.id = id;
        this.type = type;
        this.homie = homie;
    }

    /**
     * Adds a new property to the node or returns an existing property
     * with the specified name.
     */
    public Property getProperty(String id) {
        if(!Homie.isValidTopicID(id)) {
            throw new IllegalArgumentException("Property name doesn't match homie's allowed topic ID pattern");
        }

        if (!properties.containsKey(id)) {
            Property p = new Property(this.homie, this, id);
            properties.put(id, p);
            return p;
        } else {
            return properties.get(id);
        }
    }

    public int getPropCount() {
        return properties.size();
    }

    /**
     * Gets called when homie is successfully connected and allows nodes and
     * properties to advertise their attributes.
     */
    protected void onConnect() {
        sendProperties();
        properties.forEach((key, value) -> value.onConnect());
    }

    /**
     * Advertise supported properties
     */
    private void sendProperties() {
        homie.publish(this.getId() + "/" + "$type", type, true);

        if(properties.size() > 0) {
            String p = String.join(",", properties.keySet());
            homie.publish(this.getId() + "/" + "$properties", p, true);
        } else {
            homie.publish(this.getId() + "/" + "$properties", "", true);
        }

        homie.publish(this.getId() + "/" + "$name", this.getName(), true);
    }
}
