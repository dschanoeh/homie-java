package io.github.dschanoeh.homie_java;

import lombok.Getter;
import lombok.Setter;

public class Configuration {

    @Getter private String deviceID;
    @Getter @Setter private String brokerUrl;
    @Getter @Setter private String brokerPassword;
    @Getter @Setter private String brokerUsername;
    @Getter @Setter private String deviceName = "";
    @Getter private String baseTopic = "homie";
    @Getter @Setter private Integer statsInterval = 10000;
    @Getter @Setter private Integer disconnectRetry = 2000;

    public void setDeviceID(String name) {
        if(!Homie.isValidTopicID(name)) {
            throw new IllegalArgumentException("Device ID doesn't match homie's allowed topic ID pattern");
        }
        this.deviceID = name;
    }

    public void setBaseTopic(String baseTopic) {
        if(!Homie.isValidTopicID(baseTopic)) {
            throw new IllegalArgumentException("Device ID doesn't match homie's allowed topic ID pattern");
        }

        this.baseTopic = baseTopic;
    }
}
