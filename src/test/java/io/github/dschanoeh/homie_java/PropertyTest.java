package io.github.dschanoeh.homie_java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PropertyTest {
    private static Homie homie;

    @BeforeAll
    public static void setup() {
        Configuration c = new Configuration();
        homie = new Homie(c, "name", "version");
    }

    @Test
    void setName() {
        Node n = new Node(homie, "node", "node");
        Property p = n.getProperty("property");
        assertThrows(NullPointerException.class, () -> {
            p.setName(null);
        });
    }

    @Test
    void sendNaN() {
        Node n = new Node(homie, "node", "node");
        Property p = n.getProperty("property");
        p.setDataType(Property.DataType.FLOAT);
        assertThrows(IllegalArgumentException.class, () -> {
            p.send(Double.NaN);
        });
    }
}
