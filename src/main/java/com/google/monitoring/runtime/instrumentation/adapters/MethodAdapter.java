package com.google.monitoring.runtime.instrumentation.adapters;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Created by jmaloney on 5/31/17.
 */
public class MethodAdapter extends MethodVisitor {

    private static final String PATH = "com/google/monitoring/runtime/instrumentation/Recorders";

    private static final String METHOD_NAME = "classAllocation";
    private static final String METHOD_SIGNATURE = "(Ljava/lang/Class;)V";

    private static final String PRIMITIVE_ARRAY_SUFFIX = "ArrayAllocation";
    private static final String PRIMITIVE_ARRAY_SIGNATURE = "(I)V";

    private static final String OBJECT_ARRAY_METHOD = "objectArrayAllocation";
    private static final String OBJECT_ARRAY_SIGNATURE = "(ILjava/lang/Class;)V";

    private static final String BOOLEAN_ARR_T_DESC = Type.getType(boolean[].class).getDescriptor();
    private static final String CHAR_ARR_T_DESC = Type.getType(char[].class).getDescriptor();
    private static final String FLOAT_ARR_T_DESC = Type.getType(float[].class).getDescriptor();
    private static final String DOUBLE_ARR_T_DESC = Type.getType(double[].class).getDescriptor();
    private static final String BYTE_ARR_T_DESC = Type.getType(byte[].class).getDescriptor();
    private static final String SHORT_ARR_T_DESC = Type.getType(short[].class).getDescriptor();
    private static final String INT_ARR_T_DESC = Type.getType(int[].class).getDescriptor();
    private static final String LONG_ARR_T_DESC = Type.getType(long[].class).getDescriptor();

    MethodAdapter(final MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String typeName) {
        switch (opcode) {
            case Opcodes.NEW:
                classAllocation(typeName);
                super.visitTypeInsn(opcode, typeName);
                break;
            case Opcodes.ANEWARRAY:
                objectArrayAllocation(typeName);
                super.visitTypeInsn(opcode, typeName);
                break;
            default:
                super.visitTypeInsn(opcode, typeName);
        }
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        if (opcode == Opcodes.NEWARRAY) {
            final String arrayDesc;
            switch (operand) {
                case Opcodes.T_BOOLEAN:
                    arrayDesc = BOOLEAN_ARR_T_DESC;
                    break;
                case Opcodes.T_CHAR:
                    arrayDesc = CHAR_ARR_T_DESC;
                    break;
                case Opcodes.T_FLOAT:
                    arrayDesc = FLOAT_ARR_T_DESC;
                    break;
                case Opcodes.T_DOUBLE:
                    arrayDesc = DOUBLE_ARR_T_DESC;
                    break;
                case Opcodes.T_BYTE:
                    arrayDesc = BYTE_ARR_T_DESC;
                    break;
                case Opcodes.T_SHORT:
                    arrayDesc = SHORT_ARR_T_DESC;
                    break;
                case Opcodes.T_INT:
                    arrayDesc = INT_ARR_T_DESC;
                    break;
                case Opcodes.T_LONG:
                    arrayDesc = LONG_ARR_T_DESC;
                    break;
                default:
                    assert false;  // should not happen
                    return;
            }
            primitiveArrayAllocation(arrayDesc);
            super.visitIntInsn(opcode, operand);
        } else {
            super.visitIntInsn(opcode, operand);
        }
    }

    @Override
    public void visitMethodInsn(final int opcode,
                                final String owner,
                                final String name,
                                final String signature,
                                final boolean itf) {

        if (opcode == Opcodes.INVOKESTATIC &&
                // Array does its own native allocation.  Grr.
                owner.equals("java/lang/reflect/Array") &&
                name.equals("newInstance")) {
            if (signature.equals("(Ljava/lang/Class;I)Ljava/lang/Object;")) {
                super.visitInsn(Opcodes.DUP2);
                super.visitInsn(Opcodes.SWAP);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, PATH, OBJECT_ARRAY_METHOD, OBJECT_ARRAY_SIGNATURE, false);
            }
        }

        super.visitMethodInsn(opcode, owner, name, signature, itf);
    }

    private void objectArrayAllocation(final String typeName) {
        super.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn(Type.getObjectType(typeName));
        super.visitMethodInsn(Opcodes.INVOKESTATIC, PATH, OBJECT_ARRAY_METHOD, OBJECT_ARRAY_SIGNATURE, false);
    }

    private void primitiveArrayAllocation(final String typeName) {
        super.visitInsn(Opcodes.DUP);
        final String name = Type.getType(typeName).getElementType().getClassName();
        super.visitMethodInsn(Opcodes.INVOKESTATIC, PATH, name + PRIMITIVE_ARRAY_SUFFIX, PRIMITIVE_ARRAY_SIGNATURE, false);
    }

    private void classAllocation(final String typeName) {
        mv.visitLdcInsn(Type.getObjectType(typeName));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, PATH, METHOD_NAME, METHOD_SIGNATURE, false);
    }
}
