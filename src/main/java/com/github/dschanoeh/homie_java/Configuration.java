package com.github.dschanoeh.homie_java;

public class Configuration {

    private String deviceID;
    private String brokerUrl;
    private String deviceTopic = "homie";
    private Integer statsInterval = 10000;
    private Integer disconnectRetry = 2000;

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String name) {
        this.deviceID = name;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getDeviceTopic() {
        return deviceTopic;
    }

    public void setDeviceTopic(String deviceTopic) {
        this.deviceTopic = deviceTopic;
    }

    public Integer getStatsInterval() {
        return statsInterval;
    }

    public void setStatsInterval(Integer statsInterval) {
        this.statsInterval = statsInterval;
    }

    public Integer getDisconnectRetry() {
        return disconnectRetry;
    }

    public void setDisconnectRetry(Integer disconnectRetry) {
        this.disconnectRetry = disconnectRetry;
    }

}
