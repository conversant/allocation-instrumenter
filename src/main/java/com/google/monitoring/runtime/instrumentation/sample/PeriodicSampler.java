package com.google.monitoring.runtime.instrumentation.sample;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Samples periodically. An atomic nextSampleTime keeps track of when the earliest time the next
 * sample can be taken. When the current time is greater than this the next sample time is calculated
 * and a CAS with the nextSampleTime is attempted, if successful canSample() returns true.
 *
 * Created by jmaloney on 11/29/16.
 */
public class PeriodicSampler implements SampleStrategy {
    private final AtomicLong nextSampleTime = new AtomicLong();
    private final long minSampleInterval;
    private final long jitter;

    public PeriodicSampler(final long start, final long minSampleInterval, final long jitter){
        this.nextSampleTime.set(start);
        this.minSampleInterval = minSampleInterval;
        this.jitter = jitter;
    }

    @Override
    public boolean canSample() {
        final long now = System.currentTimeMillis();
        final long nextTime = nextSampleTime.get();
        if (now > nextTime){
            return nextSampleTime.compareAndSet(nextTime, now + minSampleInterval + getJitter());
        } else {
            return false;
        }
    }

    /**
     * Based on the amount of jitter set select a random value between [-1 * jitter, jitter)
     *
     * @return
     */
    private long getJitter(){
        if (jitter == 0){
            return 0;
        } else {
            return ThreadLocalRandom.current().nextLong(-1 * jitter, jitter);
        }
    }

}
