package com.java.assignment1;

/**
 * Created by janhavi on 1/27/17.
 */
public class FibonacciSeries {
    /**
     * @param n : takes a number to generate fibonacci series until that num
     */
    public static long fibonacci(long n) {
        if (n == 1) { return 1; }
        if (n == 2) { return 1; }
        return fibonacci(n - 1) + fibonacci(n - 2);
        }
    }

