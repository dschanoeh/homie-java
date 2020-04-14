package io.github.dschanoeh.homie_java;

public class Configuration {

    private String deviceID;
    private String brokerUrl;
    private String brokerPassword;
    private String brokerUsername;
    private String deviceName = "";
    private String baseTopic = "homie";
    private Integer statsInterval = 10000;
    private Integer disconnectRetry = 2000;

    public String getDeviceID() {
        return deviceID;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String name) { this.deviceName = name; }

    public void setDeviceID(String name) {
        if(!Homie.isValidTopicID(name)) {
            throw new IllegalArgumentException("Device ID doesn't match homie's allowed topic ID pattern");
        }
        this.deviceID = name;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getBrokerPassword() {
		return brokerPassword;
	}

	public void setBrokerPassword(String brokerPassword) {
		this.brokerPassword = brokerPassword;
	}

	public String getBrokerUsername() {
		return brokerUsername;
	}

	public void setBrokerUsername(String brokerUsername) {
		this.brokerUsername = brokerUsername;
	}

	public String getBaseTopic() {
        return baseTopic;
    }

    public void setBaseTopic(String baseTopic) {
        if(!Homie.isValidTopicID(baseTopic)) {
            throw new IllegalArgumentException("Device ID doesn't match homie's allowed topic ID pattern");
        }

        this.baseTopic = baseTopic;
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
