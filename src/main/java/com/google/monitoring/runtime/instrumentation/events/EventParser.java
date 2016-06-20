package com.google.monitoring.runtime.instrumentation.events;


/**
 * Created by jmaloney on 11/29/16.
 */
public class EventParser {
    public enum VerbosityLevel{
        METHOD_NAME,
        METHOD_CLASS_NAME,
        METHOD_CLASS_LINE_NUMBER;
    }

    /**
     * Events should be parsed to the following format depending on verbosityLevel:
     *   METHOD_NAME - methodName1;methodName2;methodName3 1253
     *   METHOD_CLASS_NAME - classPathAndName1.methodName1;classPathAndName2.methodName2 1253
     *   METHOD_CLASS_LINE_NUMBER - classPathAndName1.methodName1:1;classPathAndName2.methodName2:2 1253
     *
     * where 1253 is the items size in bytes
     *
     * @param event the event to parse
     * @param verbosityLevel the verbosityLevel to parse the event into
     * @return a string representation of the event
     */
    public static String parseEvent(final Event event, final VerbosityLevel verbosityLevel){
        final StringBuilder builder = new StringBuilder();
        for (int i = event.getTrace().length - 1; i >= 0 ; i--){
            final StackTraceElement stackTraceElement = event.getTrace()[i];
            if (stackTraceElement.getClassName() == null || stackTraceElement.getMethodName() == null) {
                return "";
            } else if (stackTraceElement.getClassName().startsWith("com.google.monitoring.runtime.instrumentation.")){
                //truncate the stack trace once it gets into instrumentation
                break;
            }

            switch (verbosityLevel){
                case METHOD_NAME:
                    builder.append(stackTraceElement.getMethodName());
                    break;
                case METHOD_CLASS_NAME:
                    builder.append(stackTraceElement.getClassName())
                            .append(".")
                            .append(stackTraceElement.getMethodName());
                    break;
                case METHOD_CLASS_LINE_NUMBER:
                    builder.append(stackTraceElement.getClassName())
                            .append(".")
                            .append(stackTraceElement.getMethodName())
                            .append(":")
                            .append(stackTraceElement.getLineNumber());
                    break;
            }

            builder.append(";");
        }
        builder.append(event.getObjectName()).append(" ").append(event.getSize()).append("\n");
        return builder.toString();
    }
}
