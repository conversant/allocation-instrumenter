package com.google.monitoring.runtime.instrumentation;

import com.google.monitoring.runtime.instrumentation.events.EventParser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by jmaloney on 12/1/16.
 */
public class InstrumentationPropertiesImpl implements InstrumentationProperties {

    private final String recorder;
    private final boolean recordSize;
    private final String outputPath;
    private final EventParser.VerbosityLevel verbosityLevel;

    private final String sampleStrategy;
    private final long delaySecs;
    private final long sampleRate;
    private final long sampleInterval;

    public InstrumentationPropertiesImpl(final String filePath){
        final Properties properties = loadProperties(filePath);

        recorder = loadString(properties, RECORDER_PROPERTY, DEFAULT_RECORDER);
        recordSize = loadBoolean(properties, RECORD_SIZE_PROPERTY, DEFAULT_RECORD_SIZE);
        outputPath = loadString(properties, OUTPUT_PATH_PROPERTY, DEFAULT_OUTPUT_PATH);
        verbosityLevel = loadVerbosity(properties, VERBOSITY_LEVEL_PROPERTY, DEFAULT_VERBOSITY_LEVEL);

        sampleStrategy = loadString(properties, SAMPLE_STRATEGY_PROPERTY, DEFAULT_SAMPLE_STRATEGY);
        delaySecs = loadLong(properties, DELAY_SECS_PROPERTY, DEFAULT_DELAY_SECS);
        sampleRate = loadLong(properties, SAMPLE_RATE_PROPERTY, DEFAULT_SAMPLE_RATE);
        sampleInterval = loadLong(properties, SAMPLE_INTERVAL_PROPERTY, DEFAULT_SAMPLE_INTERVAL);
    }

    private EventParser.VerbosityLevel loadVerbosity(Properties properties, String propertyName, EventParser.VerbosityLevel defaultValue) {
        final String verbosity = properties.getProperty(propertyName,"");
        switch (verbosity){
            case "methodName":
                return EventParser.VerbosityLevel.METHOD_NAME;
            case "methodClassName":
                return EventParser.VerbosityLevel.METHOD_CLASS_NAME;
            case "methodClassLineNumber":
                return EventParser.VerbosityLevel.METHOD_CLASS_LINE_NUMBER;
            default:
                return defaultValue;
        }
    }

    private static String loadString(final Properties properties, final String propertyName, final String defaultValue){
        final String value = properties.getProperty(propertyName);
        if (value != null){
            return value;
        } else {
            return defaultValue;
        }
    }

    private static long loadLong(final Properties properties, final String propertyName, final long defaultValue){
        final String value = properties.getProperty(propertyName);
        if (value != null){
            return Long.parseLong(value);
        } else {
            return defaultValue;
        }
    }

    private static boolean loadBoolean(final Properties properties, final String propertyName, final boolean defaultValue){
        final String value = properties.getProperty(propertyName);
        if (value != null){
            return Boolean.parseBoolean(value);
        } else {
            return defaultValue;
        }
    }

    private static Properties loadProperties(final String filePath){
        final Properties properties = new Properties();
        if (filePath == null || filePath.trim().equals("")){
            System.out.println("No Properties file specified. Using defaults.");
        } else {
            try {
                final InputStream inputStreamFromFile = new FileInputStream(filePath);
                properties.load(inputStreamFromFile);
                inputStreamFromFile.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception loading properties file! No Properties file loaded. Using defaults.");
            }

        }
        return properties;
    }

    @Override
    public String recorder() {
        return recorder;
    }

    @Override
    public boolean recordSize() {
        return recordSize;
    }

    @Override
    public String outputPath() {
        return outputPath;
    }

    @Override
    public EventParser.VerbosityLevel verbosityLevel() {
        return verbosityLevel;
    }

    @Override
    public String sampleStrategy() {
        return sampleStrategy;
    }

    @Override
    public long delaySecs() {
        return delaySecs;
    }

    @Override
    public long sampleRate() {
        return sampleRate;
    }

    @Override
    public long sampleInterval() {
        return sampleInterval;
    }
}
