package io.github.dschanoeh.homie_java;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Property {
    private final static Logger LOGGER = Logger.getLogger(Node.class.getName());

    private String name = "";
    private boolean settable = false;
    private String unit = "";
    private String format = "";
    private DataType dataType = DataType.STRING;
    private final Homie homie;
    private final Node node;
    private PropertySetCallback callback;

    public static enum DataType {
        INTEGER, FLOAT, BOOLEAN, STRING, ENUM, COLOR
    };

    private IMqttMessageListener setMessageListener = new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            callback.performSet(Property.this, message.toString());
        }
    };

    public Property(Homie homie, Node node, String name) {
        this.name = name;
        this.node = node;
        this.homie = homie;
    }

    public boolean isSettable() {
        return settable;
    }

    public void makeUnsettable() {
        this.settable = false;
        homie.deregisterListener(node.getName() + "/" + this.name + "/$settable");
    }

    public void makeSettable(PropertySetCallback callback) {
        this.settable = true;
        this.callback = callback;
        homie.registerListener(node.getName() + "/" + this.name + "/set", setMessageListener);
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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
        homie.publish(node.getName() + "/" + this.name, value, false);
    }

    protected void onConnect() {
        if (!"".equals(unit)) {
            homie.publish(node.getName() + "/" + this.name + "/$unit", unit, true);
        }

        homie.publish(node.getName() + "/" + this.name + "/$settable", Boolean.toString(settable), true);
        
        if (DataType.STRING != dataType) {
            homie.publish(node.getName() + "/" + this.name + "/$datatype", dataType.toString().toLowerCase(), true);
        }
    }
}
