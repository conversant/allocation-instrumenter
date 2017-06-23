package com.google.monitoring.runtime.instrumentation;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by jmaloney on 5/22/17.
 */
public class UnsafeHolder {
    public static final Unsafe UNSAFE;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe)unsafeField.get(null);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}