package com.java.assignment1;

/**
 * Created by janhavi on 1/24/17.
 */


public class RawTemperatureData {

    double runningSum;
    int runningCount;

    public double getRunningSum() {
        return runningSum;
    }

    public void setRunningSum(double runningSum) {
        this.runningSum = runningSum;
    }

    public int getRunningCount() {
        return runningCount;
    }

    public void setRunningCount(int runningCount) {
        this.runningCount = runningCount;
    }

    /*This function is synchronized so only 1 thread can update the runningSum*/
    public synchronized void updateRunningSum(double runningSum) {
        this.runningSum  = this.runningSum +runningSum;
    }

    /*This function is synchronized so only 1 thread can update the runningCount*/
    public synchronized void updateRunningCount() { this.runningCount  = runningCount + 1;}

}
