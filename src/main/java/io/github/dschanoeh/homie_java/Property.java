package io.github.dschanoeh.homie_java;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.logging.Logger;

public class Property {
    private final static Logger LOGGER = Logger.getLogger(Node.class.getName());

    private String name = "";
    private final String id;
    private boolean settable = false;
    private boolean retained = true;
    private String unit = "";
    private String format = "";
    private DataType dataType = DataType.STRING;
    private final Homie homie;
    private final Node node;
    private PropertySetCallback callback;

    public enum DataType {
        INTEGER, FLOAT, BOOLEAN, STRING, ENUM, COLOR
    }

    private final IMqttMessageListener setMessageListener = new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            callback.performSet(Property.this, message.toString());
        }
    };

    public Property(Homie homie, Node node, String id) {
        this.id = id;
        this.node = node;
        this.homie = homie;
    }

    public boolean isSettable() {
        return settable;
    }

    public boolean isRetained() {
        return retained;
    }

    public void setRetained(boolean retained) {
        this.retained = retained;
    }

    public void makeUnsettable() {
        this.settable = false;
        homie.deregisterListener(node.getID() + "/" + this.getID() + "/$settable");
    }

    public void makeSettable(PropertySetCallback callback) {
        this.settable = true;
        this.callback = callback;
        homie.registerListener(node.getID() + "/" + this.getID() + "/set", setMessageListener);
    }

    public String getUnit() {
        return unit;
    }

    public String getID() {
        return id;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(name != null) {
            this.name = name;
        }
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public void send(String value) {
        switch (this.dataType) {
        case BOOLEAN:
        case FLOAT:
        case INTEGER:
            throw new UnsupportedOperationException("Trying to send String value but property type is " + this.dataType.toString());
        case ENUM:
        case COLOR:
        case STRING:
            break;
        }
        homie.publish(buildPath(""), value, this.isRetained());
    }

    public void send(Boolean value) {
        if(this.dataType != DataType.BOOLEAN) {
            throw new UnsupportedOperationException("Trying to send Boolean value but property type is " + this.dataType.toString());
        }

        String s = String.valueOf(value).toLowerCase();
        homie.publish(buildPath(""), s, this.isRetained());
    }
    
    public void send(Long value) {
        if(this.dataType != DataType.INTEGER) {
            throw new UnsupportedOperationException("Trying to send Long value but property type is " + this.dataType.toString());
        }
        
        String s = String.valueOf(value);
        homie.publish(buildPath(""), s, this.isRetained());
    }

    public void send(Integer value) {
        if(this.dataType != DataType.INTEGER) {
            throw new UnsupportedOperationException("Trying to send Integer value but property type is " + this.dataType.toString());
        }

        String s = String.valueOf(value);
        homie.publish(buildPath(""), s, this.isRetained());
    }

    public void send(Double value) {
        if(this.dataType != DataType.FLOAT) {
            throw new UnsupportedOperationException("Trying to send Float value but property type is " + this.dataType.toString());
        }
        if(value.isInfinite() || value.isNaN()) {
            throw new IllegalArgumentException("NaN and infinity values are not supported");
        }

        String s = String.valueOf(value);
        homie.publish(buildPath(""), s, this.isRetained());
    }

    public void send(Double value, Integer precision) {
        if(this.dataType != DataType.FLOAT) {
            throw new UnsupportedOperationException("Trying to send Float value but property type is " + this.dataType.toString());
        }
        if(value.isInfinite() || value.isNaN()) {
            throw new IllegalArgumentException("NaN and infinity values are not supported");
        }
        if(precision < 0) {
            throw new IllegalArgumentException("Precision cannot be negative");
        }

        String s = String.format("%." + String.valueOf(precision) + "f", value);
        homie.publish(buildPath(""), s, this.isRetained());
    }

    protected void onConnect() {
        if (!"".equals(unit)) {
            homie.publish(buildPath("/$unit"), unit, true);
        }

        homie.publish(buildPath("/$name"), this.getName(), true);
        homie.publish(buildPath("/$settable"), Boolean.toString(settable), true);
        
        if (DataType.STRING != dataType) {
            homie.publish(buildPath("/$datatype"), dataType.toString().toLowerCase(), true);
        }
    }

    protected String buildPath(String topic) {
        return node.getID() + "/" + this.getID() + topic;
    }
}
