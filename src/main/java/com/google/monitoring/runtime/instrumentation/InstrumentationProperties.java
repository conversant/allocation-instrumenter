package com.google.monitoring.runtime.instrumentation;

import com.google.monitoring.runtime.instrumentation.events.EventParser;

/**
 * Created by jmaloney on 12/1/16.
 */
public interface InstrumentationProperties {

    // PROPERTY NAMES
    String RECORDER_PROPERTY = "recorder";
    String RECORD_SIZE_PROPERTY = "record.size";
    String OUTPUT_PATH_PROPERTY = "output.file";
    String VERBOSITY_LEVEL_PROPERTY = "stack.trace.verbosity";

    String SAMPLE_STRATEGY_PROPERTY = "sample.strategy";
    String DELAY_SECS_PROPERTY = "sample.delay.secs";
    String SAMPLE_RATE_PROPERTY = "sample.rate";
    String SAMPLE_INTERVAL_PROPERTY = "sample.interval.ms";


    // DEFAULTS
    String DEFAULT_RECORDER = "flame";
    boolean DEFAULT_RECORD_SIZE = true;
    String DEFAULT_OUTPUT_PATH = "/tmp/stacks.txt";
    EventParser.VerbosityLevel DEFAULT_VERBOSITY_LEVEL = EventParser.VerbosityLevel.METHOD_CLASS_NAME;

    String DEFAULT_SAMPLE_STRATEGY = "allocationCount";
    long DEFAULT_DELAY_SECS = 0L;
    long DEFAULT_SAMPLE_RATE = 10_000L;
    long DEFAULT_SAMPLE_INTERVAL = 10L;


    // ACCESS METHODS
    String recorder();
    boolean recordSize();
    String outputPath();
    EventParser.VerbosityLevel verbosityLevel();

    String sampleStrategy();
    long delaySecs();
    long sampleRate();
    long sampleInterval();
}
