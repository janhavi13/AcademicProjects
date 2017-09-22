compile:
	javac com/java/assignment1/*.java

sequential: compile
	java com.java.assignment1.SequentialRun 1912.csv.gz 
nolock: compile
	java com.java.assignment1.NoLockRun 1912.csv.gz 
nosharing: compile
	java com.java.assignment1.NoSharingRun 1912.csv.gz 
finelock: compile
	java com.java.assignment1.FineLockThreading 1912.csv.gz 
coarselock: compile
	java com.java.assignment1.CoarseLockRun 1912.csv.gz 
