/*
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.monitoring.runtime.instrumentation;

import com.google.monitoring.runtime.instrumentation.adapters.AllocationClassAdapter;
import com.google.monitoring.runtime.instrumentation.adapters.VerifyingClassAdapter;
import com.google.monitoring.runtime.instrumentation.events.AllocationEvent;
import com.google.monitoring.runtime.instrumentation.recorders.FlamePrinter;
import com.google.monitoring.runtime.instrumentation.recorders.FlameRecorder;
import com.google.monitoring.runtime.instrumentation.events.LifetimeEvent;
import com.google.monitoring.runtime.instrumentation.recorders.LifetimePrinter;
import com.google.monitoring.runtime.instrumentation.recorders.LifetimeRecorder;
import com.google.monitoring.runtime.instrumentation.sample.AllocationCountSampler;
import com.google.monitoring.runtime.instrumentation.sample.PeriodicSampler;
import com.google.monitoring.runtime.instrumentation.sample.SampleStrategy;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Instruments bytecodes that allocate heap memory to call a recording hook.
 * This will add a static invocation to a recorder function to any bytecode that
 * looks like it will be allocating heap memory allowing users to implement heap
 * profiling schemes.
 *
 * @author Ami Fischman
 * @author Jeremy Manson
 */
public class AllocationInstrumenter implements ClassFileTransformer {
    public static final Logger logger = Logger.getLogger(AllocationInstrumenter.class.getName());

    // We can rewrite classes loaded by the bootstrap class loader
    // iff the agent is loaded by the bootstrap class loader.  It is
    // always *supposed* to be loaded by the bootstrap class loader, but
    // this relies on the Boot-Class-Path attribute in the JAR file always being
    // set to the name of the JAR file that contains this agent, which we cannot
    // guarantee programmatically.
    private static volatile boolean canRewriteBootstrap;

    private static boolean canRewriteClass(final String className, final ClassLoader loader) {
        // There are two conditions under which we don't rewrite:
        //  1. If className was loaded by the bootstrap class loader and
        //  the agent wasn't (in which case the class being rewritten
        //  won't be able to call agent methods).
        //  2. If it is java.lang.ThreadLocal, which can't be rewritten because the
        //  JVM depends on its structure.
        if (((loader == null) && !canRewriteBootstrap) ||
                className.startsWith("java/lang/ThreadLocal")) {
            return false;
        }
        // third_party/java/webwork/*/ognl.jar contains bad class files.  Ugh.
        if (className.startsWith("ognl/")) {
            return false;
        }

        return true;
    }

    // No instantiating me except in premain() or in {@link JarClassTransformer}.
    AllocationInstrumenter() {
    }

    public static void premain(final String agentArgs, final Instrumentation inst) {
        System.out.println("Loading allocation instrumentation...");
        AllocationRecorder.setInstrumentation(inst);

        // Force eager class loading here; we need these classes in order to do
        // instrumentation, so if we don't do the eager class loading, we
        // get a ClassCircularityError when trying to load and instrument
        // this class.
        try {
            Class.forName("sun.security.provider.PolicyFile");
            Class.forName("java.util.ResourceBundle");
            Class.forName("java.util.Date");
        } catch (Throwable t) {
            // NOP
        }

        if (!inst.isRetransformClassesSupported()) {
            System.err.println("Some JDK classes are already loaded and " +
                    "will not be instrumented.");
        }

        // Don't try to rewrite classes loaded by the bootstrap class
        // loader if this class wasn't loaded by the bootstrap class
        // loader.
        if (AllocationRecorder.class.getClassLoader() != null) {
            canRewriteBootstrap = false;
            // The loggers aren't installed yet, so we use println.
            System.err.println("Class loading breakage: " +
                    "Will not be able to instrument JDK classes");
            return;
        }
        canRewriteBootstrap = true;

        if (!setupRecorder(agentArgs)){
            return;
        }

        bootstrap(inst);
        System.out.println("Loaded allocation instrumentation!");
    }

    /**
     * Load the properties file and set the specified settings.
     *
     * @param propertiesPath path to properties file
     * @return true if sampler setup successful
     */
    private static boolean setupRecorder(final String propertiesPath){
        final InstrumentationProperties properties = new InstrumentationPropertiesImpl(propertiesPath);

        // Set sampling properties
        final long start = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(properties.delaySecs());
        final SampleStrategy sampleStrategy;
        switch (properties.sampleStrategy()){
            case "allocationCount":
                sampleStrategy = new AllocationCountSampler(start, properties.sampleRate(), properties.sampleRate());
                break;
            case "time":
                sampleStrategy = new PeriodicSampler(start, properties.sampleInterval(), properties.sampleInterval());
                break;
            default:
                System.err.println("Unknown sample strategy! " + properties.sampleStrategy() + " Stopping instrumentations.");
                return false;
        }
        AllocationRecorder.setSampleStrategy(sampleStrategy);

        // Setup recorder
        try{
            switch (properties.recorder()){
                case "flame":
                    setupFlameSampler(properties);
                    break;
                case "lifetime":
                    setupLifetimeRecorder(properties);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Exception setting up the " + properties.recorder() + " recorder. Stopping instrumentation.");
            return false;
        }

        return true;
    }

    private static void setupLifetimeRecorder(final InstrumentationProperties properties) throws FileNotFoundException, UnsupportedEncodingException {
        final BlockingQueue<LifetimeEvent> queue = new ArrayBlockingQueue<>(2048);
        final LifetimePrinter printer = new LifetimePrinter(properties);
        printer.setQueue(queue);
        printer.start();

        final LifetimeRecorder lifetimeRecorder = new LifetimeRecorder(queue, printer.getId(), properties.recordSize());
        AllocationRecorder.setRecorder(lifetimeRecorder);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    printer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private static void setupFlameSampler(final InstrumentationProperties properties) throws FileNotFoundException, UnsupportedEncodingException {
        final BlockingQueue<AllocationEvent> queue = new ArrayBlockingQueue<>(2048);
        final FlamePrinter printer = new FlamePrinter(properties);
        printer.setQueue(queue);
        printer.start();

        final FlameRecorder flameRecorder = new FlameRecorder(queue,printer.getId(), properties.recordSize());
        AllocationRecorder.setRecorder(flameRecorder);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    printer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void bootstrap(final Instrumentation inst) {
        inst.addTransformer(new AllocationInstrumenter(),
                inst.isRetransformClassesSupported());

        if (!canRewriteBootstrap) {
            return;
        }

        // Get the set of already loaded classes that can be rewritten.
        final Class<?>[] classes = inst.getAllLoadedClasses();
        final ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
        for (int i = 0; i < classes.length; i++) {
            if (inst.isModifiableClass(classes[i])) {
                classList.add(classes[i]);
            }
        }

        // Reload classes, if possible.
        final Class<?>[] workaround = new Class<?>[classList.size()];
        try {
            inst.retransformClasses(classList.toArray(workaround));
        } catch (UnmodifiableClassException e) {
            System.err.println("AllocationInstrumenter was unable to " +
                    "retransform early loaded classes.");
        }


    }

    @Override
    public byte[] transform(final ClassLoader loader,
                            final String className,
                            final Class<?> classBeingRedefined,
                            final ProtectionDomain protectionDomain,
                            final byte[] origBytes) {
        if (!canRewriteClass(className, loader)) {
            return null;
        }

        return instrument(origBytes, loader);
    }


    /**
     * Given the bytes representing a class, go through all the bytecode in it and
     * instrument any occurrences of new/newarray/anewarray/multianewarray with
     * pre- and post-allocation hooks.  Even more fun, intercept calls to the
     * reflection API's Array.newInstance() and instrument those too.
     *
     * @param originalBytes  the original <code>byte[]</code> code.
     * @param recorderClass  the <code>String</code> internal name of the class
     *                       containing the recorder method to run.
     * @param recorderMethod the <code>String</code> name of the recorder method
     *                       to run.
     * @param loader         the <code>ClassLoader</code> for this class.
     * @return the instrumented <code>byte[]</code> code.
     */
    public static byte[] instrument(final byte[] originalBytes,
                                    final String recorderClass,
                                    final String recorderMethod,
                                    final ClassLoader loader) {
        final ClassReader cr = new ClassReader(originalBytes);
        try {

            //Don't instrument this package except for its test class
            if (cr.getClassName().contains("com\\google\\monitoring\\runtime\\instrumentation\\") ){
                if (!cr.getClassName().contains("Test")){
                    return originalBytes;
                }
            }

            // The verifier in JDK7+ requires accurate stackmaps, so we use
            // COMPUTE_FRAMES.
            final ClassWriter cw = new StaticClassWriter(cr, ClassWriter.COMPUTE_FRAMES, loader);

            final VerifyingClassAdapter vcw = new VerifyingClassAdapter(cw, originalBytes, cr.getClassName());
            final ClassVisitor adapter = new AllocationClassAdapter(vcw, recorderClass, recorderMethod);

            cr.accept(adapter, ClassReader.SKIP_FRAMES);

            return vcw.toByteArray();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Failed to instrument class.", e);
            throw e;
        } catch (Error e) {
            logger.log(Level.WARNING, "Failed to instrument class.", e);
            throw e;
        }
    }


    /**
     * @param originalBytes The original version of the class.
     * @param loader        The ClassLoader of this class.
     * @return the instrumented version of this class.
     * @see #instrument(byte[], String, String, ClassLoader)
     * documentation for the 4-arg version.  This is a convenience
     * version that uses the recorder in this class.
     */
    public static byte[] instrument(final byte[] originalBytes, final ClassLoader loader) {
        return instrument(
                originalBytes,
                "com/google/monitoring/runtime/instrumentation/AllocationRecorder",
                "recordAllocation",
                loader);
    }
}
