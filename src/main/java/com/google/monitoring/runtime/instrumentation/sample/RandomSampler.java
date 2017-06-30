package com.google.monitoring.runtime.instrumentation.sample;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by jmaloney on 6/1/17.
 */
public class RandomSampler extends SampleStrategy {

    private static final int MASK = (1 << 24) - 1;

    @Override
    public boolean canSample() {
        return (ThreadLocalRandom.current().nextInt() & MASK) == 0;
    }
}
