package com.google.monitoring.runtime.instrumentation.sample;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A thread local allocation count sampling strategy. This strategy keeps thread local counts of
 * allocations and compares that the the thread local sample rate. If the sample rate is the same
 * as the count canSample() returns true, resets the local count, and calculates the next sampleRate.
 *
 * Created by jmaloney on 11/29/16.
 */
public class AllocationCountSampler implements SampleStrategy {

    private final ThreadLocal<Long> threadLocalSampleCnt = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return 0L;
        }
    };
    private final ThreadLocal<Long> threadLocalSampleRate = new ThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return -1L;
        }
    };

    private volatile boolean started = false;
    private final long startTime;
    private final long jitter;
    private final long desiredSampleRate;

    public AllocationCountSampler(final long start, final long desiredSampleRate, final long jitter){
        this.startTime = start;
        this.jitter = jitter;
        this.desiredSampleRate = desiredSampleRate;
    }

    @Override
    public boolean canSample() {
        final long currentSampleRate = threadLocalSampleRate.get();
        final long currentSampleCnt = threadLocalSampleCnt.get() + 1;

        if (currentSampleRate == -1 && !started && System.currentTimeMillis() > startTime){
            started = true;
            threadLocalSampleCnt.set(0L);
            threadLocalSampleRate.set(desiredSampleRate + getJitter());
            return true;
        } else if (currentSampleRate == currentSampleCnt){
            threadLocalSampleCnt.set(0L);
            threadLocalSampleRate.set(desiredSampleRate + getJitter());
            return true;
        } else {
            threadLocalSampleCnt.set(currentSampleCnt);
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
