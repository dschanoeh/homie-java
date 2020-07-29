package io.github.dschanoeh.homie_java;

public interface BroadcastReceiver {
    void broadcastReceived(String topic, String payload);
}
