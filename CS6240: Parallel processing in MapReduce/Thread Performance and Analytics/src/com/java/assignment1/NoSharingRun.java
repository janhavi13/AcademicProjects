package com.java.assignment1;


import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by janhavi on 1/26/17.
 */

class NoShareProcessThread implements Callable<Map<String, RawTemperatureData>>
{

    private List<String> fileLines = new ArrayList<String>();
    private HashMap<String , RawTemperatureData> individualtmaxRecords = new HashMap<>();
    public NoShareProcessThread(List<String> fileLines ) {
        this.fileLines = fileLines;
    }

    @Override
    public Map<String, RawTemperatureData> call() throws Exception {

        for (String fileline : fileLines) {
            if (fileline.contains("TMAX")) {
                String record[] = fileline.split(",");
                if (individualtmaxRecords.containsKey(record[0])) {
                    RawTemperatureData data = individualtmaxRecords.get(record[0]);
                    int count = data.getRunningCount() + 1;
                    data.setRunningCount(count);
                    double sum = data.getRunningSum() + Double.parseDouble(record[3]);
                    data.setRunningSum(sum);
                    //FibonacciSeries.fibonacci(17);
                    individualtmaxRecords.put(record[0], data);

                }
                else
                {
                    RawTemperatureData data = new RawTemperatureData();
                    data.setRunningSum(Double.parseDouble(record[3]));
                    data.setRunningCount(1);
                    //FibonacciSeries.fibonacci(17);
                    individualtmaxRecords.put(record[0], data);
                }
            }
        }
        return individualtmaxRecords;
    }

}
public class NoSharingRun {


    static Map<String, RawTemperatureData> tmaxRecords = new HashMap<String, RawTemperatureData>();
    public static void main(String[] argv) throws IOException, InterruptedException, TimeoutException, ExecutionException {
        System.out.println("NO SHARE EXECUTION......");
        List<Map<String,RawTemperatureData>> resultsFromThreads = new ArrayList<>();
        String filename = argv[0];
        List<String> fileLines = DataProcessing.accumulateData(filename);
        long difference = 0 , total=0 , avg=0 , min = Long.MAX_VALUE , max = Long.MIN_VALUE;
        for(int k = 0 ; k <10  ; k++) {
            ExecutorService executeThread = Executors.newFixedThreadPool(4);
            Future<Map<String, RawTemperatureData>> doTask;
            long startTime = System.currentTimeMillis();
            Thread threads[] = new Thread[4];
            int threadInputSize = fileLines.size() / threads.length;
            int startIndex = 0;
            int endIndex = startIndex + threadInputSize - 1;
            for (int i = 0; i < threads.length; i++) {
                List<String> splitRecords = DataProcessing.splitRecords(startIndex, endIndex, fileLines);

                doTask = executeThread.submit(new NoShareProcessThread(splitRecords));
                resultsFromThreads.add(doTask.get(10,TimeUnit.SECONDS));   // add the time

                startIndex = endIndex + 1;
                endIndex = endIndex + threadInputSize;
                if (i == (threads.length - 1)) {
                    endIndex = fileLines.size() - 1;
                }
            }
            executeThread.shutdown();
            executeThread.awaitTermination(10,TimeUnit.SECONDS);

            Map<String, Double> averageFromAllThreads = DataProcessing.calculateAverageFromAllThreads(resultsFromThreads);
            long endTime = System.currentTimeMillis();
            DataProcessing.printAverage(averageFromAllThreads);
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