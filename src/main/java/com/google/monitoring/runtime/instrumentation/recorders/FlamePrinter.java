package com.google.monitoring.runtime.instrumentation.recorders;

import com.google.monitoring.runtime.instrumentation.InstrumentationProperties;
import com.google.monitoring.runtime.instrumentation.events.AllocationEvent;
import com.google.monitoring.runtime.instrumentation.events.EventParser;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Pulls AllocationEvent's off of the queue and outputs the data in the desired form.
 *
 * Created by jmaloney on 5/23/2016.
 */
public class FlamePrinter extends Thread {
    private BlockingQueue<AllocationEvent> queue;
    private final Writer writer;
    private final EventParser.VerbosityLevel verbosityLevel;

    public FlamePrinter(final InstrumentationProperties properties) throws FileNotFoundException, UnsupportedEncodingException {
        writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(properties.outputPath()), "utf-8"));
        verbosityLevel = properties.verbosityLevel();
    }

    public void close() throws IOException {
        writer.close();
    }

    private void process(final AllocationEvent event){
        final String entry = EventParser.parseEvent(event, verbosityLevel);
        if (!entry.equals("")) {
            try {
                writer.write(entry);
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
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
