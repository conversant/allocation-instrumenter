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

import com.google.monitoring.runtime.instrumentation.AllocationInstrumenter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A <code>MethodVisitor</code> that instruments all heap allocation bytecodes
 * to record the allocation being done for profiling.
 * Instruments bytecodes that allocateString heap memory to call a recording hook.
 *
 * @author Ami Fischman
 */
class EscapeMethodAdapter extends MethodVisitor {

    public static final String START_SIGNATURE = "()V";
    public static final String STOP_SIGNATURE= "(Ljava/land/String;)V";
    public static final String CLASS_PATH = "com/google/monitoring/runtime/instrumentation/EscapeAnalyzer";
    private int allocIndex = 0;

    // A helper struct for describing the scope of temporary local variables we
    // create as part of the instrumentation.
    private static class VariableScope {
        public final int index;
        public final Label start;
        public final Label end;
        public final String desc;

        public VariableScope(int index, Label start, Label end, String desc) {
            this.index = index;
            this.start = start;
            this.end = end;
            this.desc = desc;
        }
    }

    // Dictionary of primitive type opcode to english name.
    private static final String[] primitiveTypeNames = new String[]{
            "INVALID0", "INVALID1", "INVALID2", "INVALID3",
            "boolean", "char", "float", "double",
            "byte", "short", "int", "long"
    };

    // To track the difference between <init>'s called as the result of a NEW
    // and <init>'s called because of superclass initialization, we track the
    // number of NEWs that still need to have their <init>'s called.
    private int outstandingAllocs = 0;

    // We need to set the scope of any local variables we materialize;
    // accumulate the scopes here and set them all at the end of the visit to
    // ensure all labels have been resolved.  Allocated on-demand.
    private List<VariableScope> localScopes = null;

    private List<VariableScope> getLocalScopes() {
        if (localScopes == null) {
            localScopes = new LinkedList<VariableScope>();
        }
        return localScopes;
    }


    /**
     * The LocalVariablesSorter used in this adapter.  Lame that it's public but
     * the ASM architecture requires setting it from the outside after this
     * AllocationMethodAdapter is fully constructed and the LocalVariablesSorter
     * constructor requires a reference to this adapter.  The only setter of
     * this should be AllocationClassAdapter.visitMethod().
     */
    public LocalVariablesSorter lvs = null;

    /**
     * A new AllocationMethodAdapter is created for each method that gets visited.
     */
    public EscapeMethodAdapter(MethodVisitor mv, String recorderClass,
                               String recorderMethod) {
        super(Opcodes.ASM5, mv);
    }

    /**
     * newarray shows up as an instruction taking an int operand (the primitive
     * element type of the array) so we hook it here.
     */
    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (opcode == Opcodes.NEWARRAY) {
            // instack: ... count
            // outstack: ... aref
            if (operand >= 4 && operand <= 11) {
                invokeStart();
                super.visitInsn(Opcodes.DUP); // -> stack: ... count count
                super.visitIntInsn(opcode, operand); // -> stack: ... count aref
                invokeRecordAllocation(primitiveTypeNames[operand]);
                // -> stack: ... aref
            } else {
                AllocationInstrumenter.logger.severe("NEWARRAY called with an invalid operand " +
                        operand + ".  Not instrumenting this allocation!");
                super.visitIntInsn(opcode, operand);
            }
        } else {
            super.visitIntInsn(opcode, operand);
        }
    }

    /**
     * Reflection-based allocation (@see java.lang.reflect.Array#newInstance) is
     * triggered with a static method call (INVOKESTATIC), so we hook it here.
     * Class initialization is triggered with a constructor call (INVOKESPECIAL)
     * so we hook that here too as a proxy for the new bytecode which leaves an
     * uninitialized object on the stack that we're not allowed to touch.
     * {@link Object#clone} is also a call to INVOKESPECIAL,
     * and is hooked here.  {@link Class#newInstance} and
     * {@link java.lang.reflect.Constructor#newInstance} are both
     * INVOKEVIRTUAL calls, so they are hooked here, as well.
     */
    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String signature, boolean itf) {

        if (opcode == Opcodes.INVOKESPECIAL) {
            if ("<init>".equals(name) && outstandingAllocs > 0) {
                // Tricky because superclass initializers mean there can be more calls
                // to <init> than calls to NEW; hence outstandingAllocs.
                --outstandingAllocs;

                // Most of the time (i.e. in bytecode generated by javac) it is the case
                // that following an <init> call the top of the stack has a reference ot
                // the newly-initialized object.  But nothing in the JVM Spec requires
                // this, so we need to play games with the stack to make an explicit
                // extra copy (and then discard it).

                dupStackElementBeforeSignatureArgs(signature);
                super.visitMethodInsn(opcode, owner, name, signature, itf);
                super.visitLdcInsn(-1);
                super.visitInsn(Opcodes.SWAP);
                invokeRecordAllocation(owner);
                super.visitInsn(Opcodes.POP);
                return;
            }
        }

        super.visitMethodInsn(opcode, owner, name, signature, itf);
    }

    // Given a method signature interpret the top of the stack as the arguments
    // to the method, dup the top-most element preceding these arguments, and
    // leave the arguments alone.  This is done by inspecting each parameter
    // type, popping off the stack elements using the type information,
    // duplicating the target element, and pushing the arguments back on the
    // stack.
    private void dupStackElementBeforeSignatureArgs(final String sig) {
        final Label beginScopeLabel = new Label();
        final Label endScopeLabel = new Label();
        super.visitLabel(beginScopeLabel);

        Type[] argTypes = Type.getArgumentTypes(sig);
        int[] args = new int[argTypes.length];

        for (int i = argTypes.length - 1; i >= 0; --i) {
            args[i] = newLocal(argTypes[i], beginScopeLabel, endScopeLabel);
            super.visitVarInsn(argTypes[i].getOpcode(Opcodes.ISTORE), args[i]);
        }
        super.visitInsn(Opcodes.DUP);
        for (int i = 0; i < argTypes.length; ++i) {
            int op = argTypes[i].getOpcode(Opcodes.ILOAD);
            super.visitVarInsn(op, args[i]);
            if (op == Opcodes.ALOAD) {
                super.visitInsn(Opcodes.ACONST_NULL);
                super.visitVarInsn(Opcodes.ASTORE, args[i]);
            }
        }
        super.visitLabel(endScopeLabel);
    }

    /**
     * new and anewarray bytecodes take a String operand for the type of
     * the object or array element so we hook them here.  Note that new doesn't
     * actually result in any instrumentation here; we just do a bit of
     * book-keeping and do the instrumentation following the constructor call
     * (because we're not allowed to touch the object until it is initialized).
     */
    @Override
    public void visitTypeInsn(int opcode, String typeName) {
        if (opcode == Opcodes.NEW) {
            // We can't actually tag this object right after allocation because it
            // must be initialized with a ctor before we can touch it (Verifier
            // enforces this).  Instead, we just note it and tag following
            // initialization.
            invokeStart();
            super.visitTypeInsn(opcode, typeName);
            ++outstandingAllocs;
        } else if (opcode == Opcodes.ANEWARRAY) {
            invokeStart();
            super.visitInsn(Opcodes.DUP);
            super.visitTypeInsn(opcode, typeName);
            invokeRecordAllocation(typeName);
        } else {
            super.visitTypeInsn(opcode, typeName);
        }
    }

    private void invokeStart() {
        super.visitMethodInsn(Opcodes.INVOKESTATIC,CLASS_PATH,"start",START_SIGNATURE,false);
    }

    /**
     * Called by the ASM framework once the class is done being visited to
     * compute stack & local variable count maximums.
     */
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (localScopes != null) {
            for (VariableScope scope : localScopes) {
                super.visitLocalVariable("xxxxx$" + scope.index, scope.desc, null,
                        scope.start, scope.end, scope.index);
            }
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    // Helper method to allocateString a new local variable and account for its scope.
    private int newLocal(Type type, String typeDesc,
                         Label begin, Label end) {
        int newVar = lvs.newLocal(type);
        getLocalScopes().add(new VariableScope(newVar, begin, end, typeDesc));
        return newVar;
    }

    private int newLocal(Type type, Label begin, Label end) {
        return newLocal(type, type.getDescriptor(), begin, end);
    }

    private static final Pattern namePattern =
            Pattern.compile("^\\[*L([^;]+);$");

    // Helper method to actually invoke the recorder function for an allocation
    // event.
    // pre: stack: ... count newobj
    // post: stack: ... newobj
    private void invokeRecordAllocation(String typeName) {
        allocIndex++;
        super.visitLdcInsn("methodNamePath:"+ allocIndex);
        super.visitMethodInsn(Opcodes.INVOKESTATIC,
                CLASS_PATH, "stop", STOP_SIGNATURE, false);
    }

    /**
     * multianewarray gets its very own visit method in the ASM framework, so we
     * hook it here.  This bytecode is different from most in that it consumes a
     * variable number of stack elements during execution.  The number of stack
     * elements consumed is specified by the dimCount operand.
     */
    @Override
    public void visitMultiANewArrayInsn(String typeName, int dimCount) {
        // stack: ... dim1 dim2 dim3 ... dimN
        invokeStart();
        super.visitMultiANewArrayInsn(typeName, dimCount);
        invokeRecordAllocation(typeName);
    }

}
