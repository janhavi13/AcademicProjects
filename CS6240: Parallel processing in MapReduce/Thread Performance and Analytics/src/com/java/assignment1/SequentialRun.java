/**
 * Created by janhavi on 1/24/17.
 */

package com.java.assignment1;


import java.io.*;
import java.util.*;

public class SequentialRun {

    /**
     * @param fileLines : List of input lines
     * @return tmaxRecords : All TMAX records with its an value containing sum and count
     */
    public static HashMap<String,RawTemperatureData> generateTMAXRecords(List<String> fileLines)
    {
        HashMap<String, RawTemperatureData> tmaxRecords = new HashMap<String, RawTemperatureData>();
        for (String fileline : fileLines) {
            if (fileline.contains("TMAX")) {
                String record[] = fileline.split(",");
                tmaxRecords = DataProcessing.generateTmaxRecordHashMap(record, tmaxRecords);
            }
        }
        return tmaxRecords;
    }
    public static void main(String[] argv) throws IOException {
        String fileName = argv[0];
        List<String> fileLines = DataProcessing.accumulateData(fileName);
        long diff = 0, avg = 0, total = 0 , min = Long.MAX_VALUE , max = Long.MIN_VALUE;
        for(int i = 0 ; i <10 ; i++)
        {
            long startTime = System.currentTimeMillis();
            Map<String, RawTemperatureData> tmaxRecords = SequentialRun.generateTMAXRecords(fileLines);
            Map<String, Double> generatedOutput;
            generatedOutput = DataProcessing.calculateAverage(tmaxRecords);
            long endTime = System.currentTimeMillis();
            DataProcessing.printAverage(generatedOutput);
            System.out.println("Difference:" + (endTime - startTime));

            diff = endTime - startTime;
            min = Math.min(min , diff);
            max = Math.max(max, diff);

            total +=diff;

        }
        avg = total/10;
        System.out.println("Minimum ::" + min);
        System.out.println("Average ::" + avg);
        System.out.println("Maximum ::" + max);


    }
}

