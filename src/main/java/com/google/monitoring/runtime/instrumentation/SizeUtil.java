package com.google.monitoring.runtime.instrumentation;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by jmaloney on 5/31/17.
 */
public class SizeUtil {

    private static final ConcurrentMap<Class<?>, Long> classSizesMap = new ConcurrentHashMap<>(100_000);

    private static final long BOOLEAN_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(boolean[].class);
    private static final long BOOLEAN_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(boolean[].class);
    private static final long BYTE_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(byte[].class);
    private static final long BYTE_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(byte[].class);
    private static final long CHAR_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(char[].class);
    private static final long CHAR_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(char[].class);
    private static final long SHORT_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(short[].class);
    private static final long SHORT_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(short[].class);
    private static final long INT_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(int[].class);
    private static final long INT_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(int[].class);
    private static final long LONG_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(long[].class);
    private static final long LONG_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(long[].class);
    private static final long FLOAT_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(float[].class);
    private static final long FLOAT_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(float[].class);
    private static final long DOUBLE_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(double[].class);
    private static final long DOUBLE_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(double[].class);
    private static final long OBJECT_BASE_OFFSET = UnsafeHolder.UNSAFE.arrayBaseOffset(Object[].class);
    private static final long OBJECT_INDEX_SCALE = UnsafeHolder.UNSAFE.arrayIndexScale(Object[].class);

    private static Instrumentation inst;

    public static void init(Instrumentation instrumentation) {
        if (inst != null)
            throw new IllegalStateException("AProfSizeUtil is already initialized");
        inst = instrumentation;
    }

    private static long align(final long size) {
        return (size + 7) & ~7L;
    }

    static long booleanArraySize(final int length) {
        return align(BOOLEAN_BASE_OFFSET + BOOLEAN_INDEX_SCALE * length);
    }

    static long byteArraySize(final int length) {
        return align(BYTE_BASE_OFFSET + BYTE_INDEX_SCALE * length);
    }

    static long charArraySize(final int length) {
        return align(CHAR_BASE_OFFSET + CHAR_INDEX_SCALE * length);
    }

    static long shortArraySize(final int length) {
        return align(SHORT_BASE_OFFSET + SHORT_INDEX_SCALE * length);
    }

    static long intArraySize(final int length) {
        return align(INT_BASE_OFFSET + INT_INDEX_SCALE * length);
    }

    static long longArraySize(final int length) {
        return align(LONG_BASE_OFFSET + LONG_INDEX_SCALE * length);
    }

    static long floatArraySize(final int length) {
        return align(FLOAT_BASE_OFFSET + FLOAT_INDEX_SCALE * length);
    }

    static long doubleArraySize(final int length) {
        return align(DOUBLE_BASE_OFFSET + DOUBLE_INDEX_SCALE * length);
    }

    static long objectArraySize(final int length) {
        return align(OBJECT_BASE_OFFSET + OBJECT_INDEX_SCALE * length);
    }

    static long getObjectSize(final Class<?> cls) {
        Long classSize = classSizesMap.get(cls);
        if (classSize == null) {
            try {
                classSize = inst.getObjectSize(UnsafeHolder.UNSAFE.allocateInstance(cls));
            } catch (InstantiationException e ) {
                classSize = -1L;
            }
            classSizesMap.put(cls, classSize);
        }

        return classSize;
    }
}
