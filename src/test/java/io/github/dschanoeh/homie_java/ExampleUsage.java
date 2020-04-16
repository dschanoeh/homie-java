package io.github.dschanoeh.homie_java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExampleUsage {

    @Test
    void run() throws InterruptedException {
        /* Create a new configuration object with the basic settings to allow homie to connect to the MQTT broker */
        Configuration c = new Configuration();
        c.setBrokerUrl("tcp://127.0.0.1:1883");
        c.setDeviceID("example-device");
        c.setDeviceName("A Homie example device");

        /* Create a new homie instance with the configuration */
        Homie homie = new Homie(c, "My Test Firmware", "0.0.1");

        /* Before homie is started, all nodes and properties must be created so they are known during initialization. */
        Node node = homie.createNode("weather-station", "weather-station");
        node.setName("My weather station");

        Property temperatureProperty = node.getProperty("temperature");
        temperatureProperty.setUnit("°C");
        temperatureProperty.setDataType(Property.DataType.FLOAT);

        Property humidityProperty = node.getProperty("humidity");
        humidityProperty.setUnit("%");
        humidityProperty.setDataType(Property.DataType.INTEGER);

        Property weatherProperty = node.getProperty("weather");
        weatherProperty.setDataType(Property.DataType.ENUM);
        weatherProperty.setFormat("rainy,sunny,overcast");

        /* Start homie. This call is non-blocking and new threads will be created for the main state machine handling
         * connecting and disconnecting.
         */
        homie.setup();

        /* Wait till homie has connected to the broker and is done initializing */
        while(homie.getState() != Homie.State.READY) {
            Thread.sleep(50);
        }

        /* your code here to post sensor values, etc. */
        humidityProperty.send(12);
        temperatureProperty.send(22.546651, 2);
        weatherProperty.send("sunny");

        homie.shutdown();
    }
}
