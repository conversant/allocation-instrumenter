package com.google.monitoring.runtime.instrumentation.events;

/**
 * Created during object allocation. It contains data from the sampled allocation that is passed from the allocating
 * thread to the FlamePrinter thread where the data is processed and recorded.
 *
 * Created by jmaloney on 5/23/2016.
 */
public class AllocationEvent implements Event {
    private final long size;
    private final StackTraceElement[] trace;
    private final String objName;

    public AllocationEvent(final long size,
                           final StackTraceElement[] trace,
                           final String objName){
        this.size = size;
        this.trace = trace;
        this.objName = objName;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public StackTraceElement[] getTrace() {
        return trace;
    }

    @Override
    public String getObjectName() {
        return objName;
    }
}
