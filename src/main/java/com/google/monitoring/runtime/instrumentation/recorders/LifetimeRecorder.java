package com.google.monitoring.runtime.instrumentation.recorders;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.ObjectSizeMeasurement;
import com.google.monitoring.runtime.instrumentation.events.LifetimeEvent;

import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;

/**
 * Created by jmaloney on 10/10/16.
 */
public class LifetimeRecorder implements Recorder {

    private final BlockingQueue<LifetimeEvent> queue;
    private final long id;
    private final boolean recordSize;

    public LifetimeRecorder(final BlockingQueue<LifetimeEvent> queue,
                            final long id,
                            final boolean recordSize){
        this.queue = queue;
        this.id = id;
        this.recordSize = recordSize;
    }

    @Override
    public void record(final int count, final String desc, final Object newObj) {
        if (Thread.currentThread().getId() != id) {
            final long objectSize;
            final Instrumentation instr;
            if (recordSize && (instr = AllocationRecorder.getInstrumentation()) != null) {
                // Copy value into local variable to prevent NPE that occurs when
                // instrumentation field is set to null by this class's shutdown hook
                // after another thread passed the null check but has yet to call
                // instrumentation.getObjectSize()
                objectSize = ObjectSizeMeasurement.getObjectSize(newObj, (count >= 0), instr);
            } else {
                objectSize = 1;
            }

            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            LifetimeEvent lifetimeEvent = new LifetimeEvent(new WeakReference<>(newObj), desc, trace, objectSize);
            queue.offer(lifetimeEvent);
        }
    }
}
