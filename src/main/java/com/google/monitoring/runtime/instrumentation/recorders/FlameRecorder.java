package com.google.monitoring.runtime.instrumentation.recorders;

import com.google.monitoring.runtime.instrumentation.AllocationRecorder;
import com.google.monitoring.runtime.instrumentation.ObjectSizeMeasurement;
import com.google.monitoring.runtime.instrumentation.events.AllocationEvent;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.BlockingQueue;

/**
 * Samples memory allocations. Each invocation makes an AllocationEvent which is put onto a queue to be processed on a
 * separate thread. The sampleAllocation method is executed on the same thread where the actual allocation occurs.
 *
 * Created by jmaloney on 5/23/2016.
 */
public class FlameRecorder implements Recorder {

    private final BlockingQueue queue;
    private final long id;
    private final boolean recordSize;

    public FlameRecorder(final BlockingQueue queue,
                         final long id,
                         final boolean recordSize){
        this.queue = queue;
        this.id = id;
        this.recordSize = recordSize;
    }

    @Override
    public void record(final int count, final String desc, final Object newObj){
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
            AllocationEvent allocationEvent = new AllocationEvent(objectSize, trace, desc);
            queue.offer(allocationEvent);
        }
    }


}
