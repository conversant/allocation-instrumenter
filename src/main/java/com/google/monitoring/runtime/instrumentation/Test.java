package com.google.monitoring.runtime.instrumentation;

import com.google.monitoring.runtime.instrumentation.adapters.EscapeAnalyzer;
import com.sun.management.ThreadMXBean;

import java.lang.management.ManagementFactory;
import java.util.HashMap;

/**
 * Created by jmaloney on 5/23/2016.
 */
public class Test {

    public static void main(String [] args) {
//        System.out.println("start");
//        long start = System.currentTimeMillis();
//        HashMap<Integer,String> bigMap = allocateMap();
//        Integer count = 0;
//        for (Integer i = 0; i < 100000000; i++){
//            String hello = new String("foo") + i;
//            bigMap.put(i,hello);
//            Integer length = hello.length();
//            count = count + length + i;
//            String str = allocateString();
//            Long val = allocateLong();
//            HashMap<Integer,String> map = allocateMap();
//        }
//
//        System.out.println("count " + count);
//        System.out.println("Elapsed: " + (System.currentTimeMillis() - start));

        //-javaagent:target/java-allocation-instrumenter-3.0-SNAPSHOT.jar=flame.properties
        ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long[] thisThread = new long[1];
        thisThread[0] = Thread.currentThread().getId();
        long total = 10000000;
        long start = bean.getThreadAllocatedBytes(thisThread)[0];
        for (int i = 0; i < total; i++){
            called();
        }
        long end = bean.getThreadAllocatedBytes(thisThread)[0];
        System.out.println((end - start)/total);
        System.out.println(var);
    }

    static long var = 0;
    static void called(){

        Long val = new Long(5);
        int ip = 111111111;
        EscapeAnalyzer.start();
        String st = ((ip >> 24) & 0xff)+ "." +
                ((ip >> 16) & 0xff) + "." +
                ((ip >> 8) & 0xff) + "." +
                (ip & 0xff);
        EscapeAnalyzer.stop("add");

        var += val + (st == null ? 0 : 1);

    }

    /*
     * arrays:
     * int/byte/char/boolean/float 64
     * long/double 48
     * object/(int[64][] but not int[1][1]) 64
     *
     */
    static String allocateString(){
        return new String("dumbffffff123");
    }

    static Long allocateLong(){
        return new Long(124);
    }

    static HashMap<Integer,String> allocateMap(){ return new HashMap<Integer,String>();}



}
