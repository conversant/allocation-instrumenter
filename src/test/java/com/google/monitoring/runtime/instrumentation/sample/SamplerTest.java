package com.google.monitoring.runtime.instrumentation.sample;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by jmaloney on 12/1/16.
 */
public class SamplerTest {

    @Test
    public void allocationCountBasicTest(){
        final long pastTime = System.currentTimeMillis() - 10;

        SampleStrategy sampleStrategy = new AllocationCountSampler(pastTime, 1, 0);
        for(int i = 0; i < 100; i++){
            Assert.assertTrue(sampleStrategy.canSample());
        }

        sampleStrategy = new AllocationCountSampler(pastTime, 2, 0);
        Assert.assertTrue(sampleStrategy.canSample());
        Assert.assertFalse(sampleStrategy.canSample());
        Assert.assertTrue(sampleStrategy.canSample());
        Assert.assertFalse(sampleStrategy.canSample());
    }

    @Test
    public void allocationCountNotStartedTest() throws InterruptedException {
        final long futureTime = System.currentTimeMillis() + 10;
        SampleStrategy sampleStrategy = new AllocationCountSampler(futureTime, 1, 0);
        Assert.assertFalse(sampleStrategy.canSample());
    }

    @Test
    public void periodicBasicTest() throws InterruptedException {
        final long pastTime = System.currentTimeMillis() - 10;
        SampleStrategy sampleStrategy = new PeriodicSampler(pastTime, 0, 0);
        for(int i = 0; i < 100; i++){
            Thread.sleep(1);
            Assert.assertTrue(sampleStrategy.canSample());
        }
    }

    @Test
    public void periodicNotStartedTest() throws InterruptedException {
        final long futureTime = System.currentTimeMillis() + 10;
        SampleStrategy sampleStrategy = new PeriodicSampler(futureTime, 1, 0);
        Assert.assertFalse(sampleStrategy.canSample());
    }
}
