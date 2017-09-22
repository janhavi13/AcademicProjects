package com.java.assignment1;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by janhavi on 1/25/17.
 */

/*
 * This class is a common util provided to all versions of the code. It has common functionality across all versions of code.
 * accumulating data in the memory , calculating average , printing the average , chunking the file for each thread,
 * generating TMAX records in a DataStructure. MOst common used DataStructure across all the files is Map. Map because
 * it allows to store Key , Value pair. FOr us, Key is the stationId.
 * Another RawTemperatureData Object has been used. It is created to holc the running sum and running count.
 */
public class DataProcessing {

   /**
     * @param fileName : name of the input file
     * @return fileLines : List of all lines read from input file
     * @throws IOException
     */
    public static List<String> accumulateData(String fileName) throws IOException {
        List<String> fileLines = new ArrayList<String>();
        FileInputStream fin = new FileInputStream(fileName);
        GZIPInputStream gzis = new GZIPInputStream(fin);
        InputStreamReader xover = new InputStreamReader(gzis);
        BufferedReader is = new BufferedReader(xover);
        String line;
        while ((line = is.readLine()) != null) {
            fileLines.add(line);
        }
        return fileLines;
    }

    /**
     * @param tmaxRecords hashMap which contains every unique stationId
     * @return Map that contains the average of each stationId for TMAX records
     * Use of HashMap ensures every entry in the HashMap is a unique entry.
    */
    public static Map<String,Double> calculateAverage(Map<String,RawTemperatureData> tmaxRecords)
    {
        Map<String,Double> generatedAverage = new HashMap<String, Double>();
        Iterator<Map.Entry<String, RawTemperatureData>> entries = tmaxRecords.entrySet().iterator();
        while (entries.hasNext()) {
            double average = 0;
            Map.Entry<String,  RawTemperatureData> entry = entries.next();
            average = entry.getValue().getRunningSum()/entry.getValue().getRunningCount();
            generatedAverage.put(entry.getKey(),average);
        }
        return generatedAverage;
    }


    /**
     * @param tmaxRecordsFromEachThread : Map held by each thread which is used by an intermediate HashMap to accumulate
     *                                   unique entries for all threads
     * @return generatedAverage: This generates a HashMap which contains the final values gathered from all threads
     */
    public static Map<String,Double> calculateAverageFromAllThreads(List<Map<String,RawTemperatureData>> tmaxRecordsFromEachThread)
    {
        Map<String, RawTemperatureData> accumulateSumAndCountForAll = new HashMap<String, RawTemperatureData>();
        Map<String,Double> generatedAverage = new HashMap<String, Double>();
        for(Map<String, RawTemperatureData> entry:tmaxRecordsFromEachThread)
        {
            for(String key :entry.keySet())
            {
                RawTemperatureData data = new RawTemperatureData();
                if(accumulateSumAndCountForAll.containsKey(key))
                {
                    data.setRunningSum(entry.get(key).getRunningSum() +
                            accumulateSumAndCountForAll.get(key).getRunningSum());
                    data.setRunningCount(entry.get(key).getRunningCount() +
                            accumulateSumAndCountForAll.get(key).getRunningCount());
                    accumulateSumAndCountForAll.put(key,data);
                }else{
                    accumulateSumAndCountForAll.put(key,entry.get(key));
                }
            }
        }
        generatedAverage = DataProcessing.calculateAverage(accumulateSumAndCountForAll);
          return generatedAverage;
    }

    /**
     * @param generatedAverage : Map that contains the unique entries for each stationId
     *        generatedAverage is generated in calculateAverage or calculateAverageFromAllThreads
     */
    public static void printAverage(Map<String, Double> generatedAverage)
    {
        Iterator<Map.Entry<String, Double>> entries = generatedAverage.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String,  Double> entry = entries.next();
            System.out.println(entry.getKey()+" : "+entry.getValue());
        }
    }

    /**
     * splitRecords functions generates chunks of total input.
     * Each of this chunck is equally divided between number of threads.
     * @param startIndex : starting index for an input chunk
     * @param endIndex : ending index for an input chuck
     * @param fileLines : entire List of lines
     * @return Sublist of the complete list of lines
     */
    public static List<String> splitRecords(int startIndex, int endIndex, List<String> fileLines)
    {
        return  fileLines.subList(startIndex, endIndex);
    }


    /**
     * @param record : 1 line from the list of input lines in-memory
     * @param tmaxRecords : HashMap which may or maynot contain values for a particular stationId
     * if tmaxRecords contains the particular record, if it does then it updates the running sum and running count
     * else creates a new entry in tmaxRecords
     * @return tmaxRecords : which contains values for all the unique stationId
     */
    public static HashMap<String, RawTemperatureData> generateTmaxRecordHashMap(String[] record , HashMap<String,RawTemperatureData> tmaxRecords)
    {
        if(tmaxRecords.containsKey(record[0]))
        {
            RawTemperatureData data = tmaxRecords.get(record[0]);
            int count = data.getRunningCount()+ 1;
            data.setRunningCount(count);
            double sum = data.getRunningSum() + Double.parseDouble(record[3]);
            data.setRunningSum(sum);
            //FibonacciSeries.fibonacci(17);
            tmaxRecords.put(record[0], data);
        }
        else
        {
            RawTemperatureData data = new RawTemperatureData();
            data.setRunningSum(Double.parseDouble(record[3]));
            data.setRunningCount(1);
            //FibonacciSeries.fibonacci(17);
            tmaxRecords.put(record[0],data);
        }
        return  tmaxRecords;
    }

}
