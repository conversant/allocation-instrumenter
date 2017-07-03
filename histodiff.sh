#!/bin/sh

java -cp target/java-allocation-instrumenter-3.0-SNAPSHOT.jar com.google.monitoring.flame.HistoDiff "$@"
