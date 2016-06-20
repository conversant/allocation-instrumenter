package com.google.monitoring.runtime.instrumentation.recorders;

import com.google.monitoring.runtime.instrumentation.InstrumentationProperties;
import com.google.monitoring.runtime.instrumentation.events.EventParser;
import com.google.monitoring.runtime.instrumentation.events.LifetimeEvent;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmaloney on 10/10/16.
 */
public class LifetimePrinter extends Thread {

    private BlockingQueue<LifetimeEvent> queue;
    private final Writer writer;
    private final List<LifetimeEvent> eventList = new ArrayList<>();
    private long gcCount = 0;
    private GarbageCollectorMXBean gcBean;
    private final EventParser.VerbosityLevel verbosityLevel;

    public LifetimePrinter(final InstrumentationProperties properties) throws FileNotFoundException, UnsupportedEncodingException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(properties.outputPath()), "utf-8"));
        verbosityLevel = properties.verbosityLevel();

        for(GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getName().equals("ParNew") || gc.getName().equals("G1_Young_Generation")){
                gcBean = gc;
            }
        }
    }

    @Override
    public void run() {
        Thread.currentThread().setName("LifetimePrinter");
        for(;;){
            try {
                if (gcCount < gcBean.getCollectionCount()){
                    processEventList();
                    queue.clear();
                    gcCount = gcBean.getCollectionCount();
                } else {
                    final LifetimeEvent event = queue.poll(1, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        eventList.add(event);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void processEventList(){
        try {
            writer.write("#GC ");
            writer.write(Long.toString(gcCount));
            writer.write("\n");
            for(int i = 0; i < eventList.size(); i++){
                final LifetimeEvent event = eventList.get(i);
                if (event.alive()){
                    final String parsed = EventParser.parseEvent(event, verbosityLevel);
                    writer.write(parsed);
                }
            }
        } catch (IOException e) {
            System.err.println("Exception in the LifetimePrinter thread printing!");
        }
        eventList.clear();
    }

    public void setQueue(final BlockingQueue<LifetimeEvent> queue) {
        this.queue = queue;
    }

    public void close() throws IOException {
        writer.flush();
    }
}
