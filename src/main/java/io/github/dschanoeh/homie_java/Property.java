package io.github.dschanoeh.homie_java;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.List;

public class Property {

    @Getter @Setter @NonNull private String name = "";
    @Getter private final String id;
    @Getter private boolean settable = false;
    @Getter @Setter private boolean retained = true;
    @Getter @Setter private String unit = "";
    @Getter private String format = "";
    @Getter private DataType dataType = DataType.STRING;
    private final Homie homie;
    private final Node node;
    private PropertySetCallback callback;
    private List<String> enumValues;

    public enum DataType {
        INTEGER, FLOAT, BOOLEAN, STRING, ENUM, COLOR_RGB, COLOR_HSV
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

    public void makeUnsettable() {
        this.settable = false;
        homie.deregisterListener(node.getId() + "/" + this.getId() + "/$settable");
    }

    public void makeSettable(PropertySetCallback callback) {
        this.settable = true;
        this.callback = callback;
        homie.registerListener(node.getId() + "/" + this.getId() + "/set", setMessageListener);
    }

    public void setFormat(String format) {
        if(this.dataType == DataType.COLOR_HSV || this.dataType == DataType.COLOR_RGB) {
            throw new UnsupportedOperationException("Cannot modify the format on properties of the color data type");
        } else if(this.dataType == DataType.ENUM) {
            String[] values = format.split(",");
            enumValues = Arrays.asList(values);
            this.format = format;
        } else {
            this.format = format;
        }
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;

        if(dataType == DataType.COLOR_RGB) {
            this.format = "rgb";
        } else if(dataType == DataType.COLOR_HSV) {
            this.format = "hsv";
        }
    }

    public void send(String value) {
        if("".equals(value) && this.dataType != DataType.STRING) {
            throw new UnsupportedOperationException("An empty string is not a valid value for anything but STRING type");
        }

        if(this.dataType == DataType.STRING) {
            homie.publish(buildPath(""), value, this.isRetained());
        } else if(this.dataType == DataType.ENUM) {
            if(this.enumValues == null || this.enumValues.isEmpty()) {
                throw new UnsupportedOperationException("Trying to send enum value but no list of enum values was provided as format");
            } else if(this.enumValues.stream().anyMatch(s -> s.equals(value))) {
                homie.publish(buildPath(""), value, this.isRetained());
            } else {
                throw new UnsupportedOperationException("Trying to send an enum value which isn't included in the list of provided values");
            }
        } else {
            throw new UnsupportedOperationException("Trying to send String value but property type is " + this.dataType.toString());
        }
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

        String s = String.format("%." + precision + "f", value);
        homie.publish(buildPath(""), s, this.isRetained());
    }

    public void send(Integer a, Integer b, Integer c) {
        if(this.dataType == DataType.COLOR_HSV) {
            if (a < 0 || a > 360 ||
                b < 0 || b > 100 ||
                c < 0 || c > 100) {
                throw new IllegalArgumentException("Provided color values are not within [0:360][0:100][0:100]");
            }
            String s = String.format("%d,%d,%d", a, b, c);
            homie.publish(buildPath(""), s, this.isRetained());
        } else if(this.dataType == DataType.COLOR_RGB) {
            if (a < 0 || a > 255 ||
                b < 0 || b > 255 ||
                c < 0 || c > 255) {
                throw new IllegalArgumentException("Provided color values are not within [0:255][0:255][0:255]");
            }
            String s = String.format("%d,%d,%d", a, b, c);
            homie.publish(buildPath(""), s, this.isRetained());
        } else {
            throw new UnsupportedOperationException("Trying to send color value but property type is " + this.dataType.toString());
        }
    }

    protected void onConnect() {
        if (!"".equals(unit)) {
            homie.publish(buildPath("/$unit"), unit, true);
        }

        homie.publish(buildPath("/$name"), this.getName(), true);
        homie.publish(buildPath("/$settable"), Boolean.toString(settable), true);

        homie.publish(buildPath("/$datatype"), dataType.toString().toLowerCase(), true);

        if(null != this.format && !this.format.equals("")) {
            homie.publish(buildPath("/$format"), this.format, true);
        }
    }

    protected String buildPath(String topic) {
        return node.getId() + "/" + this.getId() + topic;
    }
}
