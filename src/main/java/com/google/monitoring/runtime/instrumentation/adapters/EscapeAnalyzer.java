package com.google.monitoring.runtime.instrumentation.adapters;

import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by jmaloney on 6/27/2016.
 */
public class EscapeAnalyzer {
    private static ConcurrentHashMap<String,Boolean> skipMap = new ConcurrentHashMap<>();

    private static class ByteAllocationTracker {
        private static final int MAX_DEPTH = 128;
        private final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        private final long[] thisThread = new long[1];
        private long[][] start = new long[MAX_DEPTH][2];
        private int depth = 0;
        private long invocationCount = 0;

        public ByteAllocationTracker(){
            thisThread[0] = Thread.currentThread().getId();
        }

        public void start(){
            if (depth < MAX_DEPTH) {
                start[depth][0] = bean.getThreadAllocatedBytes(thisThread)[0];
                start[depth][1] = invocationCount;
            } else {
                System.err.println("Warning exceed maximum depth");
            }
            invocationCount++;
            depth++;
        }

        public void stop(String methodName){
            depth--;
            if (skipMap.get(methodName) == null) {
                long stop = bean.getThreadAllocatedBytes(thisThread)[0];
                if (depth < MAX_DEPTH) {
                    if (stop - start[depth][0] == 24 * (invocationCount - start[depth][1])){
                        skipMap.put(methodName, true);
                        System.out.println(methodName);
                    }
                } else {
                    System.err.println("Warning exceed maximum depth: " + methodName);
                }
                invocationCount++;
            }
        }
    }

    private static ThreadLocal<ByteAllocationTracker> threadLocalByteAllocationTracker = new ThreadLocal(){
        @Override
        protected ByteAllocationTracker initialValue(){
            return new ByteAllocationTracker();
        }
    };

    public static void start(){
        threadLocalByteAllocationTracker.get().start();
    }

    public static void stop(String methodName){
        threadLocalByteAllocationTracker.get().stop(methodName);
    }
}
