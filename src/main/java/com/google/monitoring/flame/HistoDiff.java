package com.google.monitoring.flame;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jmaloney on 4/14/17.
 */
public class HistoDiff {

    private static class Info {
        final int num;
        final long bytes;
        final long instances;

        Info(int num, long bytes, long instances){
            this.num = num;
            this.bytes = bytes;
            this.instances = instances;
        }

        @Override
        public String toString(){
            return num + "\t" + instances + "\t" + bytes;
        }
    }

    private static class Diff {
        final Info first;
        final Info second;
        final long diffBytes;
        final long diffInstances;
        final double percentBytes;
        final double percentInstances;

        Diff(Info info1, Info info2){
            this.first = info1;
            this.second = info2;
            diffBytes = info2.bytes - info1.bytes;
            diffInstances = info2.instances - info1.instances;
            percentBytes = diffBytes * 100.0 / (info1.bytes);
            percentInstances = diffInstances * 100.0 / info1.instances;
        }

//        @Override
        public void print(){
            System.out.format("%6d %14d %11d %9d %14d %11d %+11d      %+10d          %+4.2f            %+4.2f", first.num, first.bytes, first.instances, second.num, second.bytes, second.instances, diffBytes / 1000_000, diffInstances, percentBytes, percentInstances);

//            return first.toString() + "\t" + second.toString() + "\t" + diffBytes + "\t" + diffInstances + "\t" + percentBytes + "\t" + percentInstances;
        }
    }

    private final Map<String, Info> first = new HashMap<>();
    private final Map<String, Info> second = new HashMap<>();
    private final Map<String, Diff> diffs = new HashMap<>();
    private String[] order;

    private void diff(String path1, String path2){
        proccessFile(path1, first);
        proccessFile(path2, second);
        order = new String[first.size() + 1];
        calcDiff();
        printDiff();
    }

    private void printDiff() {
        System.out.println("rank 1    instances 1     bytes 1    rank 2    instances 2     bytes 2    diffbytes    diffinstances   percentbytes   percentinstances");
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------");
        for(int i = 1; i < order.length; i++){
            final String key = order[i];
            if (key == null){
                continue;
            }
            diffs.get(key).print();
            System.out.println("\t" + key );
//            System.out.println(diffs.get(key).toString() + "\t" + key);

        }
    }

    private void calcDiff() {
        for(Map.Entry<String, Info> entry: first.entrySet()){
            final Info firstInfo = entry.getValue();
            final Info secondInfo = second.get(entry.getKey());
            if (secondInfo != null) {
                diffs.put(entry.getKey(), new Diff(firstInfo, secondInfo));
                order[firstInfo.num] = entry.getKey();
            }
        }
    }

    private void proccessFile(String inputPath, Map<String, Info> store){
        try {
            final BufferedReader br = new BufferedReader(new FileReader(inputPath));
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    final String[] items = line.split("\\s+");
                    store.put(items[4], new Info(Integer.parseInt(items[1].substring(0,items[1].length() - 1)), Long.parseLong(items[3]), Long.parseLong(items[2])));
                } catch (Exception e) {
//                    System.err.println("skipping bad input line: " + line);
                }
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

    public static void main(String[] args) throws Exception {
        HistoDiff histoDiff = new HistoDiff();
        histoDiff.diff(args[0], args[1]);
    }
}
