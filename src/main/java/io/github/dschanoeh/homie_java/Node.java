package io.github.dschanoeh.homie_java;

import java.util.HashMap;
import java.util.logging.Logger;

public class Node {

    private final static Logger LOGGER = Logger.getLogger(Node.class.getName());

    private String name;
    private final String type;
    private final String id;
    private final Homie homie;
    private final HashMap<String, Property> properties = new HashMap<>();

    public String getName() {
        return name;
    }

    public String getID() {
        return id;
    }

    public String getType() {
        return type;
    }

    protected Node(Homie homie, String id, String type) {
        this.id = id;
        this.type = type;
        this.homie = homie;
    }

    public void setName(String name) {
        this.name = name;
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
        homie.publish(this.getID() + "/" + "$type", type, true);
        String p = String.join(",", properties.keySet());
        homie.publish(this.getID() + "/"+ "$properties", p, true);
        homie.publish(this.getID() + "/" + "$name", this.getName(), true);
    }
}
