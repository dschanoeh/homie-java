package io.github.dschanoeh.homie_java;

import lombok.Getter;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MAX_INFLIGHT_DEFAULT;

public class Homie {

    private static final Logger LOGGER = Logger.getLogger(Homie.class.getName());

    private static final String HOMIE_CONVENTION = "4.0.0";
    private static final String IMPLEMENTATION = "java";

    private static final Pattern topicIDPattern = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");

    public enum State {
        INIT, READY, DISCONNECTED, SLEEPING, LOST, ALERT
    }

    private Configuration configuration = new Configuration();
    private final String firmwareName;
    private final String firmwareVersion;
    private MqttClient client;
    private final MqttConnectOptions options;
    @Getter
    private State state = State.INIT;
    private State previousState = State.DISCONNECTED;
    private Thread stateMachineThread;
    private final ZonedDateTime bootTime = ZonedDateTime.now();
    private boolean shutdownRequest = false;
    private Timer statsTimer;
    private Function<Void, String> cpuTemperatureFunction;
    private Function<Void, String> cpuLoadFunction;
    private BroadcastReceiver broadcastReceiver;

    private final HashMap<String, Node> nodes = new HashMap<>();
    private final HashMap<String, IMqttMessageListener> listeners = new HashMap<>();

    /**
     * Allows the user to supply a CPU temperature function that will be called
     * when the stats are collected.
     *
     * @param cpuTemperatureFunction
     */
    public void setCpuTemperatureFunction(Function<Void, String> cpuTemperatureFunction) {
        this.cpuTemperatureFunction = cpuTemperatureFunction;
    }

    /**
     * Allows the user to supply a CPU load function that will be called when
     * the stats are collected.
     *
     * @param cpuLoadFunction
     */
    public void setCpuLoadFunction(Function<Void, String> cpuLoadFunction) {
        this.cpuLoadFunction = cpuLoadFunction;
    }

    private final Runnable stateMachine = () -> {
        try {
            while (!shutdownRequest) {
                switch (state) {
                    case INIT:
                        if (previousState != State.INIT) {
                            LOGGER.log(Level.INFO, "--> init");
                            previousState = State.INIT;
                        }
                        if (connect()) {
                            /* the first message we have to send is the init state */
                            publishStateUpdate();

                            sendAttributes();
                            publishNodes();
                            subscribeListeners();

                            /* Finished reporting all attributes. Now we can transition to ready state */
                            state = State.READY;
                        } else {
                            LOGGER.log(Level.INFO, "Connect failed...");
                            state = State.DISCONNECTED;
                        }
                        break;
                    case READY:
                        if (previousState != State.READY) {
                            if (client.isConnected()) {
                                publishStateUpdate();
                            }
                            LOGGER.log(Level.INFO, "--> ready");
                            previousState = State.READY;
                        }
                        if (!client.isConnected()) {
                            state = State.DISCONNECTED;
                        }
                        break;
                    case ALERT:
                        if (previousState != State.ALERT) {
                            if (client.isConnected()) {
                                publishStateUpdate();
                            }
                            LOGGER.log(Level.INFO, "--> alert");
                            previousState = State.ALERT;
                        }
                        if (!client.isConnected()) {
                            state = State.DISCONNECTED;
                        }
                        break;
                    case DISCONNECTED:
                        if (previousState != State.DISCONNECTED) {
                            LOGGER.log(Level.INFO, "--> disconnected");
                            previousState = State.DISCONNECTED;
                        }
                        Thread.sleep(configuration.getDisconnectRetry());
                        state = State.INIT;
                        break;
                    default:
                        break;
                }

                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "State machine interrupted", e);
        }
    };

    /**
     * Initialize a new Homie instance with the configuration and firmware name
     * and version.
     */
    public Homie(Configuration c, String firmwareName, String firmwareVersion) {
        this.configuration = c;
        this.firmwareName = firmwareName;
        this.firmwareVersion = firmwareVersion;
        this.options = new MqttConnectOptions();
    }

    /**
     * Initialize a new Homie instance with the configuration and firmware name
     * and version. The provided MqttConnectOptions will be used to set up the
     * broker communication.
     */
    public Homie(Configuration c, String firmwareName, String firmwareVersion, MqttConnectOptions options) {
        this.configuration = c;
        this.firmwareName = firmwareName;
        this.firmwareVersion = firmwareVersion;
        this.options = options;
    }

    /**
     * Calling setup causes the state machine to start and Homie to connect.
     */
    public void setup() {
        stateMachineThread = new Thread(stateMachine);
        stateMachineThread.start();
    }

    protected void registerListener(String topic, IMqttMessageListener listener) {
        listeners.put(topic, listener);
    }

    private void subscribeListeners() {
        for (Map.Entry<String, IMqttMessageListener> entry : listeners.entrySet()) {
            try {
                client.subscribe(buildPath(entry.getKey()), entry.getValue());
            } catch (MqttException ex) {
                LOGGER.log(Level.WARNING,
                        String.format("Was not able to subscribe listener for topic '%s'", entry.getKey()), ex);
            }
        }
    }

    protected void deregisterListener(String topic) {
        listeners.remove(topic);
    }

    private boolean connect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }

            client = new MqttClient(configuration.getBrokerUrl(), configuration.getDeviceID(), new MemoryPersistence());

            if (configuration.getBrokerPassword() != null && !configuration.getBrokerPassword().isEmpty()) {
                options.setPassword(configuration.getBrokerPassword().toCharArray());
            }
            if (configuration.getBrokerUsername() != null && !configuration.getBrokerUsername().isEmpty()) {
                options.setUserName(configuration.getBrokerUsername());
            }

            /*
             * If the user didn't set a custom MaxInFlight, we'll try to pick a good default
             */
            if (options.getMaxInflight() == MAX_INFLIGHT_DEFAULT) {
                options.setMaxInflight(
                        Math.max(MAX_INFLIGHT_DEFAULT, nodes.values().stream().mapToInt(Node::getPropCount).sum() * 2));
            }

            /* Last will will be used in case of an ungraceful disconnect */
            options.setWill(buildPath("$state"), State.LOST.toString().toLowerCase().getBytes(), 1, true);
            client.connect(options);

            if (broadcastReceiver != null) {
                IMqttMessageListener listener = new IMqttMessageListener() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        broadcastReceiver.broadcastReceived(topic.replace(getBroadcastPath(), ""), message.toString());
                    }
                };
                client.subscribe(getBroadcastPath() + "#", listener);
            }

            if (statsTimer != null) {
                statsTimer.cancel();
            }

            statsTimer = new Timer();

            TimerTask statsTask = new TimerTask() {
                @Override
                public void run() {
                    sendStats();
                }
            };

            statsTimer.scheduleAtFixedRate(statsTask, configuration.getStatsInterval(),
                    configuration.getStatsInterval());
            return true;
        } catch (MqttException e) {
            LOGGER.log(Level.SEVERE, "Could not connect", e);
            return false;
        }
    }

    private void publishStateUpdate() {
        publish("$state", state.toString().toLowerCase(), true);
    }

    private void sendAttributes() {
        publish("$homie", HOMIE_CONVENTION, true);
        publish("$implementation", IMPLEMENTATION, true);
        publish("$stats/interval", Integer.toString(configuration.getStatsInterval()), true);
        publish("$fw/name", firmwareName, true);
        publish("$fw/version", firmwareVersion, true);
        publish("$extensions", "", true);

        /* Device attributes */
        publish("$name", configuration.getDeviceName(), true);
    }

    private void sendStats() {
        long uptime = Duration.between(bootTime, ZonedDateTime.now()).getSeconds();
        publish("$stats/uptime", Long.toString(uptime), true);

        if (cpuTemperatureFunction != null) {
            publish("$stats/cputemp", cpuTemperatureFunction.apply(null), true);
        }

        if (cpuLoadFunction != null) {
            publish("$stats/cpuload", cpuLoadFunction.apply(null), true);
        }
    }

    /**
     * Publish an MQTT message.
     */
    protected void publish(String topic, String payload, Boolean retained) {
        if (client != null && client.isConnected()) {
            MqttMessage message = new MqttMessage();
            message.setRetained(retained);
            message.setQos(1);
            message.setPayload(payload.getBytes(UTF_8));
            try {
                client.publish(buildPath(topic), message);
            } catch (MqttException e) {
                LOGGER.log(Level.SEVERE, "Could not publish message", e);
            }
        } else {
            LOGGER.log(Level.WARNING,
                    () -> String.format("Couldn't publish message to topic '%s' - not connected.", topic));
        }
    }

    /**
     * Publish an MQTT message.
     * WARNING: Posting to the wrong topic can cause issues with the homie
     * convention.
     */
    public boolean publish(String topic, MqttMessage message) {
        if (client != null && client.isConnected()) {
            try {
                client.publish(topic, message);
            } catch (MqttException e) {
                LOGGER.log(Level.SEVERE, "Could not publish message", e);
                return false;
            }
        } else {
            LOGGER.log(Level.WARNING,
                    () -> String.format("Couldn't publish message to topic '%s' - not connected.", topic));
            return false;
        }
        return true;
    }

    private void publishNodes() {
        if (nodes.size() > 0) {
            String n = String.join(",", nodes.keySet());
            publish("$nodes", n, true);

            nodes.forEach((key, value) -> value.onConnect());
        } else {
            publish("$nodes", "", true);
        }
    }

    private String buildPath(String attribute) {
        return configuration.getBaseTopic() + "/" + configuration.getDeviceID() + "/" + attribute;
    }

    private String getBroadcastPath() {
        return configuration.getBaseTopic() + "/" + "$broadcast" + "/";
    }

    public void setAlert(boolean alertStatus) {
        if (alertStatus) {
            if (state == State.READY) {
                previousState = state;
                state = State.ALERT;
            } else if (state == State.ALERT) {
                LOGGER.log(Level.FINE, "Homie device is already in \"alert\" state.");
            } else {
                LOGGER.log(Level.WARNING, "An \"alert\" state can be reached only from \"ready\" state.");
            }
        } else {
            if (state == State.ALERT) {
                previousState = state;
                state = State.READY;
            } else if (state == State.READY) {
                LOGGER.log(Level.FINE, "Homie device is already in \"ready\" state.");
            } else {
                LOGGER.log(Level.WARNING, "An \"alert\" state can be disabled only from \"alert\" state.");
            }
        }
    }

    public void shutdown() {
        LOGGER.log(Level.INFO, "Shutdown request received");
        shutdownRequest = true;
        try {
            stateMachineThread.join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.INFO, "Interrupted", e);
        }
        disconnect();

        /* reset state variables so that a future re-initialization is possible */
        previousState = State.DISCONNECTED;
        state = State.INIT;
        shutdownRequest = false;

        LOGGER.log(Level.INFO, "Terminating");
    }

    private void disconnect() {
        try {
            if (client.isConnected()) {
                publish("$state", State.DISCONNECTED.toString().toLowerCase(), true);
                client.disconnect();
            }
            statsTimer.cancel();
        } catch (MqttException e) {
            LOGGER.log(Level.INFO, "Failed to disconnect", e);
        }
    }

    /**
     * Generates and registers a new node within Homie.
     */
    public Node createNode(String id, String type) {
        if (Boolean.FALSE.equals(isValidTopicID(id))) {
            throw new IllegalArgumentException("Node id doesn't match homie's allowed topic ID pattern");
        }

        if (nodes.containsKey(id)) {
            return nodes.get(id);
        } else {
            Node n = new Node(this, id, type);
            nodes.put(id, n);
            return n;
        }
    }

    /**
     * Checks if a given topic ID is valid
     */
    protected static Boolean isValidTopicID(String id) {
        Matcher m = topicIDPattern.matcher(id);
        return m.matches();
    }

    /**
     * Sets the broadcastReceiver. Must be called prior to setup().
     *
     * @param receiver the receiver to be notified
     */
    public void setBroadcastReceiver(BroadcastReceiver receiver) {
        this.broadcastReceiver = receiver;
    }

}
