#!/usr/bin/awk -f
BEGIN {
  ctr1=0
  transmitted1=0
  start1=0
  stopt1=0
  throughput1=0

  ctr2=0
  transmitted2=0
  start2=0
  stop2=0
  throughput2=0

  timer=1
}

{

	if($5 == "tcp" && $3 == 0 && $4 == 1 && $9 == "0.0" && $1 == "-" && $10 == "3.0" && ctr1 == 0)
	{
	start1=$2
	ctr1++
	}
        if($5 == "cbr" && $3 == 4 && $4 == 1 && $1 == "-" && $9 == "4.0" && $10 == "5.0" && ctr2 == 0)
	{
	start2=$2
	ctr2++
	}

	if($5 == "tcp" && $3 == "2" && $4 == "3" && $9 == "0.0" && $1 == "r" && $10 == "3.0")
	{
	if($2 > stop1)
	stop1=$2
	}

	if($5 == "cbr" && $3 == "2" && $4 == "5" && $9 == "4.0" && $1 == "r" && $10 == "5.0")
	{
	if($2 > stop2)
	stop2=$2
	}
        if($2 < timer)
        {
 	if($1 == "r" && $5 == "tcp" && $9 == "0.0" && $3 == "2" && $4 == "3" && $10 == "3.0")
	{
	transmitted1=transmitted1+$6	
	}

 	if($1 == "r" && $5 == "cbr" && $9 == "4.0" && $3 == "2" && $4 == "5" && $10 == "5.0")
	{
	transmitted2=transmitted2+$6	
	}
        }
        else
        {
        timer+=1
        throughput1 = (transmitted1/(stop1 - start1))*8/1024
	if((stop2 - start2) > 0)
	{
 	throughput2 = (transmitted2/(stop2 - start2))*8/1024
	}
	printf("%f\t%f\n", throughput1,throughput2);
	transmitted1 = 0 
        ctr1 = 0
	transmitted2 = 0 
        ctr2 = 0

	}	
}

END {

}