package com.google.monitoring.runtime.instrumentation;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.MapMaker;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by jmaloney on 10/10/16.
 */
public class ObjectSizeMeasurement {

    // Stores the object sizes for the last ~100000 encountered classes
    private static final ForwardingMap<Class<?>, Long> classSizesMap =
            new ForwardingMap<Class<?>, Long>() {
                private final ConcurrentMap<Class<?>, Long> map = new MapMaker()
                        .weakKeys()
                        .makeMap();

                @Override
                public Map<Class<?>, Long> delegate() {
                    return map;
                }

                // The approximate maximum size of the map
                private static final int MAX_SIZE = 100_000;

                // The approximate current size of the map; since this is not an AtomicInteger
                // and since we do not synchronize the updates to this field, it will only be
                // an approximate size of the map; it's good enough for our purposes though,
                // and not synchronizing the updates saves us some time
                private int approximateSize = 0;

                @Override
                public Long put(final Class<?> key, final Long value) {
                    // if we have too many elements, delete about 10% of them
                    // this is expensive, but needs to be done to keep the map bounded
                    // we also need to randomize the elements we delete: if we remove the same
                    // elements all the time, we might end up adding them back to the map
                    // immediately after, and then remove them again, then add them back, etc.
                    // which will cause this expensive code to be executed too often
                    if (approximateSize >= MAX_SIZE) {
                        for (Iterator<Class<?>> it = keySet().iterator(); it.hasNext(); ) {
                            it.next();
                            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                                it.remove();
                            }
                        }

                        // get the exact size; another expensive call, but we need to correct
                        // approximateSize every once in a while, or the difference between
                        // approximateSize and the actual size might become significant over time;
                        // the other solution is synchronizing every time we update approximateSize,
                        // which seems even more expensive
                        approximateSize = size();
                    }

                    approximateSize++;
                    return super.put(key, value);
                }
            };


    /**
     * Returns the size of the given object. If the object is not an array, we
     * check the cache first, and update it as necessary.
     *
     * @param obj     the object.
     * @param isArray indicates if the given object is an array.
     * @param instr   the instrumentation object to use for finding the object size
     * @return the size of the given object.
     */
    public static long getObjectSize(final Object obj, final boolean isArray, final Instrumentation instr) {
        if (isArray) {
            return instr.getObjectSize(obj);
        }

        final Class<?> clazz = obj.getClass();
        Long classSize = classSizesMap.get(clazz);
        if (classSize == null) {
            classSize = instr.getObjectSize(obj);
            classSizesMap.put(clazz, classSize);
        }

        return classSize;
    }
}
