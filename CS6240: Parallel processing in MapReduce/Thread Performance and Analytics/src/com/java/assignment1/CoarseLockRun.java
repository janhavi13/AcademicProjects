package com.java.assignment1;

import java.io.IOException;
import java.util.*;

/**
 * Created by janhavi on 1/25/17.
 */
public class CoarseLockRun {

    static HashMap<String, RawTemperatureData> tmaxRecords = new HashMap<String, RawTemperatureData>();
    public static void main(String[] argv) throws IOException, InterruptedException {

        System.out.println("COARSE LOCK EXECUTION ......");
        String fileName = argv[0];
        List<String> fileLines = DataProcessing.accumulateData(fileName);
        long difference = 0 , total=0 , avg=0 , min = Long.MAX_VALUE , max = Long.MIN_VALUE;
        for(int k = 0 ; k < 10 ; k++) {
            long startTime = System.currentTimeMillis();
            Thread threads[] = new Thread[4];
            int threadInputSize = fileLines.size() / threads.length;
            int startIndex = 0;
            int endIndex = startIndex + threadInputSize - 1;
            for (int i = 0; i < threads.length; i++) {
                List<String> splitRecords = DataProcessing.splitRecords(startIndex, endIndex, fileLines);
                threads[i] = new Thread(new ProcessingThread(splitRecords, tmaxRecords, "CoarseLock"));
                threads[i].start();
                threads[i].join();
                startIndex = endIndex + 1;
                endIndex = endIndex + threadInputSize;
                if (i == (threads.length - 1)) {
                    endIndex = fileLines.size() - 1;
                }

            }
            Map<String, Double> generatedOutput;
            generatedOutput = DataProcessing.calculateAverage(tmaxRecords);
            long endTime = System.currentTimeMillis();
            DataProcessing.printAverage(generatedOutput);
            difference = endTime - startTime;
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