package com.google.monitoring.flame;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by jmaloney on 1/30/17.
 */
public class Remote {

    private ThreadMXBean bean;
    private Writer writer;

    private static final long interval = TimeUnit.MILLISECONDS.toNanos(100);
    private static final long jitter = interval / 2;

    private long nextTime = System.nanoTime();
    private long endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);

    private long samples = 0;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Need to specify a server!");
            System.exit(1);
        }

        Remote remote = new Remote();
        remote.run(args[0]);
    }

    private void run(final String host) throws IOException {
        System.out.println("Starting...");
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + "/jmxrmi");
        final JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
        final MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        System.out.println("Connected to " + host);

        bean =  ManagementFactory.getPlatformMXBean(mbsc, ThreadMXBean.class);
        writer = new BufferedWriter(new OutputStreamWriter( new FileOutputStream("stacks.txt"), "utf-8"));

        while (running()){
            busyWait();
            getAndWriteTrace();
        }

        writer.close();
        System.out.println("Finished! Collected " + samples + " samples");
    }


    private void getAndWriteTrace() throws IOException {
        final ThreadInfo[] threadInfos = bean.dumpAllThreads(false,false);
        for (int i = 0; i < threadInfos.length; i++){
            final ThreadInfo info = threadInfos[i];
            if (filter(info)){
                writer.write(parseEvent(info.getStackTrace()));
            }
        }
        samples += threadInfos.length;
    }

    private void busyWait(){
        while (System.nanoTime() < nextTime){
        }
        nextTime += interval + ThreadLocalRandom.current().nextLong(-jitter, jitter);
    }

    private boolean running(){
        return System.nanoTime() < endTime;
    }

    public static String parseEvent(final StackTraceElement[] stackTraceElements){
        final StringBuilder builder = new StringBuilder();
        for (int i = stackTraceElements.length - 1; i >= 0 ; i--){
            final StackTraceElement stackTraceElement = stackTraceElements[i];
            builder.append(stackTraceElement.getClassName())
                    .append(".")
                    .append(stackTraceElement.getMethodName())
                    .append(";");
        }
        builder.append(" 1\n");
        return builder.toString();
    }

    private boolean filter(ThreadInfo info){
        if (info.getThreadState() != Thread.State.RUNNABLE){
            return false;
        }

        final String threadName = info.getThreadName();
        if (threadName.equals("Signal Dispatcher") ||
                threadName.equals("Attach Listener") ||
                threadName.equals("Service Thread")
                ){
            return false;
        }

        final StackTraceElement[] stackTrace = info.getStackTrace();
        for (int i = 0; i < stackTrace.length; i++){
            final String methodName = stackTrace[i].getMethodName();
            if (methodName.equals("epollWait") ||
                    methodName.equals("socketAccept") ||
                    methodName.equals("socketRead0") ||
                    methodName.equals("receive0") ||
                    methodName.equals("dumpThreads0")
                    ){
                return false;
            }
        }

        return true;
    }


}
