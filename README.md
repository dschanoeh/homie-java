
[![build](https://github.com/dschanoeh/homie-java/actions/workflows/build.yaml/badge.svg)](https://github.com/dschanoeh/homie-java/actions/workflows/build.yaml)
![GitHub](https://img.shields.io/github/license/dschanoeh/homie-java)


# homie-java

A Java implementation of the Homie Convention (https://github.com/homieiot/convention).

Features that are currently supported:
* Basic device behavior and state machine
* Required homie attributes
* Nodes and properties
* Settable properties
* Broadcasts

Installation and Usage
====

Currently, no builds are uploaded to a build server. You will have to add the following lines to your gradle
project configuration to ensure that homie-java is built from source together with your project:

build.gradle:
```
dependencies {
    implementation('io.github.dschanoeh:homie-java') {
        version {
            branch = 'master'
        }
    }
}
```

settings.gradle:
```
sourceControl {
    gitRepository("https://github.com/dschanoeh/homie-java.git") {
        producesModule("io.github.dschanoeh:homie-java")
    }
}
```


See [ExampleUsage.java](https://github.com/dschanoeh/homie-java/blob/master/src/test/java/io/github/dschanoeh/homie_java/ExampleUsage.java)
for an example how the homie client can be used.
