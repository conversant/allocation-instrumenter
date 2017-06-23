package com.google.monitoring.runtime.instrumentation.recorders;

import com.google.monitoring.runtime.instrumentation.InstrumentationProperties;
import com.google.monitoring.runtime.instrumentation.events.AllocationEvent;
import com.google.monitoring.runtime.instrumentation.events.EventParser;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Pulls AllocationEvent's off of the queue and outputs the data in the desired form.
 *
 * Created by jmaloney on 5/23/2016.
 */
public class FlamePrinter extends Thread {

    private final Writer writer;
    private final EventParser.VerbosityLevel verbosityLevel;
    private final long startTime = System.currentTimeMillis();

    private BlockingQueue<AllocationEvent> queue;
    private int eventCnt = 0;
    private long nextTime = System.currentTimeMillis();

    public FlamePrinter(final InstrumentationProperties properties) throws FileNotFoundException, UnsupportedEncodingException {
        writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(properties.outputPath()), "utf-8"));
        verbosityLevel = properties.verbosityLevel();
    }

    public void close() throws IOException {
        writer.close();
    }

    private void process(final AllocationEvent event){
        try {
            eventCnt++;
            EventParser.parseEvent(event, verbosityLevel, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stats() {
        final long now = System.currentTimeMillis();
        if (now > nextTime) {
            nextTime += 30_000;
            try {
                writer.append("# RUNTIME: ")
                        .append(Double.toString((now - startTime) / 1000.0d))
                        .append(" seconds SAMPLE RATE: ")
                        .append(Double.toString(eventCnt / 30.0d))
                        .append(" allocations/second\n");
                eventCnt = 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setQueue(final BlockingQueue<AllocationEvent> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("FlamePrinter");
        for(;;){
            try {
                final AllocationEvent event = queue.poll(1, TimeUnit.MILLISECONDS);
                if (event != null) {
                    process(event);
                }
                stats();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
