package com.google.monitoring.runtime.instrumentation.events;

import java.lang.ref.WeakReference;

/**
 * Created during object allocation. It contains data from the sampled allocation that is passed from the allocating
 * thread to the LifetimePrinter thread where the data is processed and recorded.
 *
 * Created by jmaloney on 10/10/16.
 */
public class LifetimeEvent implements Event {
    private final WeakReference<Object> object;
    private final String objectName;
    private final StackTraceElement[] trace;
    private final long size;

    public LifetimeEvent(final WeakReference<Object> reference,
                         final String objectName,
                         final StackTraceElement[] trace,
                         final long size){
        this.object = reference;
        this.objectName = objectName;
        this.trace = trace;
        this.size = size;
    }

    public boolean alive(){
        return object.get() != null;
    }

    @Override
    public String getObjectName(){
        return objectName;
    }

    @Override
    public StackTraceElement[] getTrace() {
        return trace;
    }

    @Override
    public long getSize() {
        return size;
    }
}
