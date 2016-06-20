package com.google.monitoring.flame;

/**
 * Created by jmaloney on 8/23/16.
 */
class StackTraceParsers {

    /**
     * Truncates stack frames prior to the filter keyword and squashes recursion.
     * Stack traces that don't contain the filter keyword will be left intact.
     *
     * @param key key
     * @param filter filter
     * @return
     */
    public static String squashTruncateNoFilter(final String key, final String filter){
        if (!key.toLowerCase().contains(filter)){
            return squash(key);
        }
        return truncateAndSquash(key, filter);
    }

    /**
     * Truncates stack frames prior to the filter keyword and squashes recursion.
     * Stack traces that don't contain the filter keyword will be removed.
     *
     * @param key key
     * @param filter filter
     * @return
     */
    public static String squashTruncateFilter(final String key, final String filter){
        if (!key.toLowerCase().contains(filter)){
            return "";
        }
        return truncateAndSquash(key, filter);
    }

    private static String truncateAndSquash(final String key, final String filter){
        final String[] stackFrames = key.split(";");
        final StringBuilder builder = new StringBuilder();
        boolean startPrinting = false;

        for(int i = 0; i < stackFrames.length; i++){
            if (stackFrames[i].toLowerCase().contains(filter)){
                startPrinting = true;
            }
            if (startPrinting) {
                if (i != 0 && stackFrames[i].equals(stackFrames[i - 1])) {
                    continue;
                } else {
                    builder.append(stackFrames[i]).append(";");
                }
            }
        }
        return builder.toString();
    }

//    /**
//     * Squashes recursion into a single frame. This only works on flat recursion (a
//     * method calling itself e.g. a() -> a() -> a().... => a()) if there is some sort
//     * of trampolining it will not be collapsed (e.g. a() -> b() -> a() -> b()....
//     * will remain the same).
//     *
//     * @param key key
//     * @return
//     */
    public static String squash(final String key) {
        final String[] stackFrames = key.split(";");
        final StringBuilder builder = new StringBuilder();

        for(int i = 0; i < stackFrames.length; i++){
            if (i != 0 && stackFrames[i].equals(stackFrames[i - 1])) {
                continue;
            } else {
                builder.append(stackFrames[i]).append(";");
            }
        }
        return builder.toString();
    }
}
