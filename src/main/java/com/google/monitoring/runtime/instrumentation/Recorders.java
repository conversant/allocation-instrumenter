package com.google.monitoring.runtime.instrumentation;

import com.google.monitoring.runtime.instrumentation.recorders.FlameRecorder;
import com.google.monitoring.runtime.instrumentation.sample.RandomSampler;

/**
 * Created by jmaloney on 5/31/17.
 */
public class Recorders {

    private static final ThreadLocal<Boolean> RECORDING_ALLOCATION = new ThreadLocal<Boolean>(){
        @Override
        protected Boolean initialValue(){
            return Thread.currentThread().getId() == PRINTER_THREAD_ID;
        }
    };
    private static final RandomSampler RANDOM_SAMPLER = new RandomSampler();
    private static final FlameRecorder RECORDER = AllocationInstrumenter.recorder;

    static long PRINTER_THREAD_ID;


    private static void recordAllocation(final String className, final long size) {
        RECORDER.record(className, size);
//        System.out.println(className + " " + size);
    }


    // Class allocation
    public static void classAllocation(final Class<?> cls) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation(cls.getName(), SizeUtil.getObjectSize(cls));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    // Array allocations
    public static void objectArrayAllocation(final int length, final Class<?> cls) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation(cls.getName() + "[]", SizeUtil.objectArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void booleanArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("boolean[]", SizeUtil.booleanArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void byteArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("byte[]", SizeUtil.byteArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void charArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("char[]", SizeUtil.charArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void shortArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("short[]", SizeUtil.shortArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void intArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("int[]", SizeUtil.intArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void longArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("long[]", SizeUtil.longArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void floatArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("float[]", SizeUtil.floatArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

    public static void doubleArrayAllocation(final int length) {
        if (!RANDOM_SAMPLER.canSample() || RECORDING_ALLOCATION.get()) {
            return;
        }
        RECORDING_ALLOCATION.set(Boolean.TRUE);
        recordAllocation("double[]", SizeUtil.doubleArraySize(length));
        RECORDING_ALLOCATION.set(Boolean.FALSE);
    }

}
