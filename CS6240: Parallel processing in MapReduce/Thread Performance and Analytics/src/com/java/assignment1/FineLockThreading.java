package com.java.assignment1;



import java.io.IOException;
import java.util.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by janhavi on 1/26/17.
 */

class FineLockProcessThread implements Runnable {

    private List<String> fileLines;
    private Map<String, RawTemperatureData> tmaxEntries = new ConcurrentHashMap<String, RawTemperatureData>();
    public FineLockProcessThread(List<String> fileLines, Map<String, RawTemperatureData> tmaxEntries) {
        this.tmaxEntries = tmaxEntries;
        this.fileLines = fileLines;
    }

    @Override
    public void run() {
        for (String fileline : fileLines) {
            if (fileline.contains("TMAX")) {
                String record[] = fileline.split(",");
                /*
                    Handle locking by call updateRunningSum and updateRunningCount which is synchronized
                 */
                RawTemperatureData data = tmaxEntries.get(record[0]);
                if (data == null) {
                    data = new RawTemperatureData();
                }
                    data.updateRunningSum(Double.parseDouble(record[3]));
                    //FibonacciSeries.fibonacci(17);
                    data.updateRunningCount();
                    tmaxEntries.put(record[0], data);


            }
        }
    }
}
public class FineLockThreading {
    static Map<String, RawTemperatureData> tmaxRecords = new ConcurrentHashMap<String, RawTemperatureData>();
    public static void main(String[] argv) throws IOException, InterruptedException {
        System.out.println("FINE LOCK EXECUTION ......");
        String filename = argv[0];
        List<String> fileLines = DataProcessing.accumulateData(filename);
        Map<String, Double> generatedOutput;
        long difference = 0 , total=0 , avg=0 , min = Long.MAX_VALUE , max = Long.MIN_VALUE;
            for(int k = 0 ;k<10;k++) {
                long startTime = System.currentTimeMillis();
                Thread threads[] = new Thread[4];
                int threadInputSize = fileLines.size() / threads.length;
                int startIndex = 0;
                int endIndex = startIndex + threadInputSize - 1;
                for (int i = 0; i < threads.length; i++) {
                    List<String> splitRecords = DataProcessing.splitRecords(startIndex, endIndex, fileLines);
                    threads[i] = new Thread(new FineLockProcessThread(splitRecords, tmaxRecords));
                    threads[i].start();
                    threads[i].join();
                    startIndex = endIndex + 1;
                    endIndex = endIndex + threadInputSize;
                    if (i == (threads.length - 1)) {
                        endIndex = fileLines.size() - 1;
                    }
                }
                generatedOutput= DataProcessing.calculateAverage(tmaxRecords);
                long endTime = System.currentTimeMillis();
                DataProcessing.printAverage(generatedOutput);
                difference = endTime - startTime;
                //total += difference;
                min = Math.min(min , difference);
                max = Math.max(max, difference);
                total += difference;
                System.out.println("Difference in Running Time:" + (endTime - startTime));

            }
        avg = total/10;
        System.out.println("Minimum ::" + min);
        System.out.println("Average ::" + avg);
        System.out.println("Maximum ::" + max);
    }
}