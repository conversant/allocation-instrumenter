package com.google.monitoring.runtime.instrumentation.events;

/**
 * Created by jmaloney on 11/29/16.
 */
public interface Event {

    /**
     * Get the size in bytes of the allocation
     *
     * @return the size in bytes of the object
     */
    long getSize();

    /**
     * Get the class name of the object allocated
     *
     * @return the name of the object sampled
     */
    String getObjectName();

    /**
     * Get the stacktrace associated with the sample
     *
     * @return an array of StackTraceElements one for each stack frame
     */
    StackTraceElement[] getTrace();
}
