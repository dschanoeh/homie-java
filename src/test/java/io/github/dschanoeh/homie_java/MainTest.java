package io.github.dschanoeh.homie_java;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MainTest {

    private static final String FIRMWARE_NAME = "TestFirmware";
    private static final String FIRMWARE_VERSION = "1.0";
    private static final String DEVICE_ID = "TestDeviceID";
    private static final String TEST_BROKER_URL = "tcp://127.0.0.1:1883";

    Homie homie;
    private MqttClient client;

    public MainTest() {
        Configuration c = new Configuration();
        c.setBrokerUrl(TEST_BROKER_URL);
        c.setDeviceID(DEVICE_ID);
        homie = new Homie(c, FIRMWARE_NAME, FIRMWARE_VERSION);
    }

    @BeforeAll
    void initializeClient() throws MqttException {
        client = new MqttClient(TEST_BROKER_URL, "ClientID", new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        client.connect(options);
    }

    @AfterAll
    void shutdownClient() throws MqttException {
        client.disconnect();
    }

    @Test
    void testDeviceID() throws MqttException {
        final Boolean[] wasReceived = {false};

        IMqttMessageListener listener = new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                if(payload.equals(DEVICE_ID)) {
                    wasReceived[0] = true;
                }
            }
        };

        assert client.isConnected();

        String topic = "homie/" + DEVICE_ID + "/$name";

        client.subscribe(topic, listener);
        homie.setup();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        homie.shutdown();
        assert wasReceived[0];
    }
}
