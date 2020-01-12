package io.github.dschanoeh.homie_java;

/**
 * This callback gets called when a settable property was set.
 */
public interface PropertySetCallback {
    void performSet(Property property, String value);
}
