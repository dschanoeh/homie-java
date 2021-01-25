package io.github.dschanoeh.homie_java;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.lang.reflect.Field;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DisconnectRecoveryTest {
    private static final String FIRMWARE_NAME = "TestFirmware";
    private static final String FIRMWARE_VERSION = "1.0";
    private static final String DEVICE_ID = "device";
    private static final String DEVICE_NAME = "My Device Name";
    private static final String TEST_BROKER_URL = "tcp://127.0.0.1:1883";
    private static final String TEST_VALUE = "foo";
    private static final String TEST_NODE = "node";
    private static final String TEST_PROPERTY = "property";

    private Homie homie;
    private MqttClient client;

    public DisconnectRecoveryTest() {
        Configuration c = new Configuration();
        c.setBrokerUrl(TEST_BROKER_URL);
        c.setDeviceID(DEVICE_ID);
        c.setDeviceName(DEVICE_NAME);
        homie = new Homie(c, FIRMWARE_NAME, FIRMWARE_VERSION);
    }

    @BeforeEach
    void initializeClient() throws MqttException {
        client = new MqttClient(TEST_BROKER_URL, "ClientID", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);
    }

    @AfterEach
    void shutdownClient() throws MqttException {
        client.disconnect();
        homie.shutdown();
    }

    @Test
    public void disconnectFlowTest() throws InterruptedException, IllegalAccessException, MqttException {
        final Boolean wasReceived[] = {false};

        Node node = homie.createNode(TEST_NODE, "type");
        Property property = node.getProperty(TEST_PROPERTY);
        PropertySetCallback listener = new PropertySetCallback() {
            @Override
            public void performSet(Property property, String value) {
                System.out.println("hello");
                if(value.equals(TEST_VALUE)) {
                    wasReceived[0] = true;
                }
            }
        };
        property.makeSettable(listener);
        homie.setup();

        while(homie.getState() != Homie.State.READY) {
            Thread.sleep(50);
        }

        /* Fiddle with reflection to access the internal MQTT client */
        MqttClient homieInternalMQTTClient = null;
        Class<?> homieClass = homie.getClass();
        Field fields[] = homieClass.getDeclaredFields();
        for(Field f: fields) {
            if(f.getName() == "client") {
                f.setAccessible(true);
                homieInternalMQTTClient = (MqttClient) f.get(homie);
            }
        }
        assert(homieInternalMQTTClient != null);

        /* Kill the connection */
        homieInternalMQTTClient.disconnectForcibly();

        /* Wait until Homie reconnects again */
        while(homie.getState() != Homie.State.READY) {
            Thread.sleep(50);
        }

        /* Verify the listener still works after reconnect */
        MqttMessage m = new MqttMessage();
        m.setPayload(TEST_VALUE.getBytes());
        String topic = "homie/" + DEVICE_ID + "/" + TEST_NODE + "/" + TEST_PROPERTY + "/set";
        client.publish(topic, m);
        Thread.sleep(100);
        assert wasReceived[0] == true;
    }
}