package com.google.monitoring.runtime.instrumentation.recorders;

/**
 * Created by jmaloney on 10/10/16.
 */
public interface Recorder {
    void record(int count, String desc, Object newObj);
}
