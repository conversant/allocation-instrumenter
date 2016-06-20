/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.monitoring.runtime.instrumentation.adapters;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * Instruments bytecodes that allocateString heap memory to call a recording hook.
 * A <code>ClassVisitor</code> that processes methods with a
 * <code>AllocationMethodAdapter</code> to instrument heap allocations.
 *
 * @author jeremymanson@google.com (Jeremy Manson)
 * @author fischman@google.com (Ami Fischman) (Original Author)
 */
public class AllocationClassAdapter extends ClassVisitor {
    private final String recorderClass;
    private final String recorderMethod;

    public AllocationClassAdapter(final ClassVisitor cv,
                                  final String recorderClass,
                                  final String recorderMethod) {
        super(Opcodes.ASM5, cv);
        this.recorderClass = recorderClass;
        this.recorderMethod = recorderMethod;
    }

    /**
     * For each method in the class being instrumented, <code>visitMethod</code>
     * is called and the returned MethodVisitor is used to visit the method.
     * Note that a new MethodVisitor is constructed for each method.
     */
    @Override
    public MethodVisitor visitMethod(final int access,
                                     final String base,
                                     final String desc,
                                     final String signature,
                                     final String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, base, desc, signature, exceptions);
        if (mv != null) {
            // We need to compute stackmaps (see
            // AllocationInstrumenter#instrument).  This can't really be
            // done for old bytecode that contains JSR and RET instructions.
            // So, we remove JSRs and RETs.
            final JSRInlinerAdapter jsria = new JSRInlinerAdapter(mv, access, base, desc, signature, exceptions);
            final AllocationMethodAdapter aimv = new AllocationMethodAdapter(jsria, recorderClass, recorderMethod);
            final LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, aimv);
            aimv.lvs = lvs;
            mv = lvs;
        }
        return mv;
    }
}
