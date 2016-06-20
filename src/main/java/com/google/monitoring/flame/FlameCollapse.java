package com.google.monitoring.flame;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility used to process stacktrace data generated from the flamesampler into a format compatible with the
 * FlameGraph tools. This work largely consists of combining all like stacktraces and summing their values.
 *
 * Created by jmaloney on 5/27/2016.
 */
public class FlameCollapse {
    private final Map<String, Long> traces = new HashMap<>();
    private final String outputFile = "collapsed.txt";
    private String inputPath = "stacks.txt";
    private long inputLineCount = 0;
    private String methodFilter = null;

    public static void main(String[] args) throws Exception {
        System.out.println("Begin FlameCollapse...");
        FlameCollapse collapser = new FlameCollapse();
        collapser.handleArgs(args);
        collapser.collapseFile();
        collapser.writeData();
    }

    public void handleArgs(final String[] args){
        if (args.length == 1){
            inputPath = args[0];
        } else if (args.length == 2){
            inputPath = args[0];
            methodFilter = args[1].toLowerCase();
        }
    }

    public void collapseFile(){
        try {
            final BufferedReader br = new BufferedReader(new FileReader(inputPath));
            String line;
            while ((line = br.readLine()) != null) {
                inputLineCount++;
                if (line.startsWith("#")){
                    continue; // skip "comment/meta" lines
                }

                processLine(traces, line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.err.println("File " + inputPath + " could not be found!");
            System.exit(1);
        } catch (IOException e){
            System.err.println("IOException while processing " + inputPath);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void writeData(){
        try {
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(outputFile), "utf-8"));

            long outputCount = 0;
            final Iterator<Map.Entry<String, Long>> it = traces.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                outputCount++;
                writer.write(entry.getKey() + " " + entry.getValue() + "\n");
            }
            writer.close();
            System.out.println("Finished FlameCollapse! Collapsed " + inputLineCount + " rows into " + outputCount);
        } catch (IOException e){
            System.err.println("IOException while writing output data!");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void processLine(final Map<String, Long> traces, final String line) {
        final String[] split = line.split(" ");
        if (split.length < 2){
            System.out.println("Incomplete line(" + inputLineCount +"): "  + line);
            return;
        }
        String key = split[0];
        Long bytesAllocated = Long.valueOf(split[1]);
        if (methodFilter != null) {
            key = StackTraceParsers.squashTruncateNoFilter(key, methodFilter);
        } else {
            key = StackTraceParsers.squash(key);
        }

        //Ignore empty lines
        if (key.trim().equals("")) {
            return;
        }

        if (traces.containsKey(key)) {
            bytesAllocated = traces.get(key) + bytesAllocated;
            traces.put(key, bytesAllocated);
        } else {
            traces.put(key, bytesAllocated);
        }
    }


}
