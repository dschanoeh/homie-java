package com.github.dschanoeh.homie_java;

public class Property {

    private String name = "";
    private boolean settable = false;
    private String unit = "";
    private String format = "";
    private DataType dataType = DataType.STRING;
    private final Homie homie;
    private final Node node;

    public static enum DataType {
        INTEGER, FLOAT, BOOLEAN, STRING, ENUM, COLOR
    };

    public Property(Homie homie, Node node, String name) {
        this.name = name;
        this.node = node;
        this.homie = homie;
    }

    public boolean isSettable() {
        return settable;
    }

    public void setSettable(boolean settable) {
        this.settable = settable;
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
        if (false != settable) {
            homie.publish(node.getName() + "/" + this.name + "/$settable", Boolean.toString(settable), true);
        }
        if (DataType.STRING != dataType) {
            homie.publish(node.getName() + "/" + this.name + "/$datatype", dataType.toString().toLowerCase(), true);
        }
    }
}
