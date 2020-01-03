package io.github.dschanoeh.homie_java;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Node {

    private final static Logger LOGGER = Logger.getLogger(Node.class.getName());

    private final String name;
    private final String type;
    private final Homie homie;
    HashMap<String, Property> properties = new HashMap<>();

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    protected Node(Homie homie, String name, String type) {
        this.name = name;
        this.type = type;
        this.homie = homie;
    }

    /**
     * Adds a new property to the node or returns an existing property
     * with the specified name.
     */
    public Property getProperty(String name) {
        if (!properties.containsKey(name)) {
            Property p = new Property(this.homie, this, name);
            properties.put(name, p);
            return p;
        } else {
            return properties.get(name);
        }
    }

    /**
     * Gets called when homie is successfully connected and allows nodes and
     * properties to advertise their attributes.
     */
    protected void onConnect() {
        sendProperties();
        properties.entrySet().forEach(i -> i.getValue().onConnect());
    }

    /**
     * Advertise supported properties
     */
    private void sendProperties() {
        homie.publish(this.getName() + "/" + "$type", type, true);
        String p = properties.keySet().stream().collect(Collectors.joining(","));
        homie.publish(this.getName() + "/"+ "$properties", p, true);
    }
}
