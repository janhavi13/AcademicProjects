package com.java.assignment1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by janhavi on 1/25/17.
 */

/*
    This class creates threads for the NoLock and CoarseLock Version. The thread is craeted with a copy of the HashMap
    storing the running sum and running count , the locktype decides which version to run - NoLock , CoarseLock
    the filelines is the input in-memory
 */
public class ProcessingThread implements Runnable{
    private List<String> fileLines = new ArrayList<String>();
    private HashMap<String,RawTemperatureData> tmaxRecords;
    private String lockType;

    public ProcessingThread(List<String> fileLines , HashMap<String, RawTemperatureData> tmaxRecords,String lockType){
        this.tmaxRecords = tmaxRecords;
        this.fileLines = fileLines;
        this.lockType = lockType;
    }

    @Override
    public void run() {
        for (String fileline : fileLines) {
            if (fileline.contains("TMAX")) {
                String record[] = fileline.split(",");
                if(lockType.equals("NoLock"))
                tmaxRecords = DataProcessing.generateTmaxRecordHashMap(record, tmaxRecords);
                if(lockType.equals("CoarseLock"))
                    // in CoarseLock we want to lock the entire DataStructure storing the stationId and running sum and count
                    synchronized (tmaxRecords)
                    {
                        tmaxRecords = DataProcessing.generateTmaxRecordHashMap(record, tmaxRecords);
                    }

            }
        }
    }
}



