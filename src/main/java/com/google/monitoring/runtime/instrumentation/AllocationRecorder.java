/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.monitoring.runtime.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import com.google.monitoring.runtime.instrumentation.recorders.Recorder;
import com.google.monitoring.runtime.instrumentation.sample.SampleStrategy;

/**
 * The logic for recording allocations, called from bytecode rewritten by
 * {@link AllocationInstrumenter}.
 *
 * @author jeremymanson@google.com (Jeremy Manson)
 * @author fischman@google.com (Ami Fischman)
 */
public class AllocationRecorder {

    private static SampleStrategy sampleStrategy;
    private static Recorder recorder;

    static {
        // Sun's JVMs in 1.5.0_06 and 1.6.0{,_01} have a bug where calling
        // Instrumentation.getObjectSize() during JVM shutdown triggers a
        // JVM-crashing assert in JPLISAgent.c, so we make sure to not call it after
        // shutdown.  There can still be a race here, depending on the extent of the
        // JVM bug, but this seems to be good enough.
        // instrumentation is volatile to make sure the threads reading it (in
        // recordAllocation()) see the updated value; we could do more
        // synchronization but it's not clear that it'd be worth it, given the
        // ambiguity of the bug we're working around in the first place.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                setInstrumentation(null);
            }
        });
    }

    // See the comment above the addShutdownHook in the static block above
    // for why this is volatile.
    private static volatile Instrumentation instrumentation = null;

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    static void setInstrumentation(final Instrumentation inst) {
        instrumentation = inst;
    }

    // Protects mutations of additionalSamplers.  Reads are okay because
    // the field is volatile, so anyone who reads additionalSamplers
    // will get a consistent view of it.
    private static final Object samplerLock = new Object();

    // Used for re-entrancy checks
    private static final ThreadLocal<Boolean> recordingAllocation = new ThreadLocal<Boolean>();

    /**
     * Sets the {@link Recorder} that will get run <b>every time an allocation is
     * sampled from Java code</b>.  Use this with <b>extreme</b> judiciousness!
     *
     * @param recorder The recorder to add.
     */
    static void setRecorder(final Recorder recorder) {
        synchronized (samplerLock) {
            AllocationRecorder.recorder = recorder;
        }
    }

    /**
     * This is a helper method that calls the 3 param version. This is inserted into new allocations by the
     * instrumentation.
     *
     * @param cls class being sampled
     * @param newObj instance being sampled
     */
    public static void recordAllocation(final Class<?> cls, final Object newObj) {
        String typename = cls.getName();
        recordAllocation(-1, typename, newObj);
    }

    /**
     * Records the allocation.  This method is invoked on every allocation
     * performed by the system.
     *
     * @param count  the count of how many instances are being
     *               allocated, if an array is being allocated.  If an array is not being
     *               allocated, then this value will be -1.
     * @param desc   the descriptor of the class/primitive type
     *               being allocated.
     * @param newObj the new <code>Object</code> whose allocation is being
     *               recorded.
     */
    public static void recordAllocation(final int count, final String desc, final Object newObj) {
        // To prevent infinite sampling loop this is disabled while the sampler code is running
        if (recordingAllocation.get() == Boolean.TRUE) {
            return;
        } else {
            recordingAllocation.set(Boolean.TRUE);
        }

        try {
            if (sampleStrategy.canSample()){
                recorder.record(count, desc, newObj);
            }
        } finally {
            recordingAllocation.set(Boolean.FALSE);
        }

    }

    /**
     * Sets the sampling strategy to use for sampling memory allocations.
     *
     * @param sampleStrategy an instance of SampleStrategy
     */
    public static void setSampleStrategy(SampleStrategy sampleStrategy) {
        AllocationRecorder.sampleStrategy = sampleStrategy;
    }

}
