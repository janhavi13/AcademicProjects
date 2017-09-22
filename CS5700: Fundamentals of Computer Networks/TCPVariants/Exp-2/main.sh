
rm -f *.jpg
rm -f *.tr
rm -f *~
rm -f NewRenoRenoThroughput.txt
rm -f NewRenoRenoDropcount.txt
rm -f NewRenoRenoLatency.txt

rm -f RenoRenoThroughput.txt
rm -f RenoRenoDropcount.txt
rm -f RenoRenoLatency.txt

rm -f NewRenoVegasThroughput.txt
rm -f NewRenoVegasDropcount.txt
rm -f NewRenoVegasLatency.txt

rm -f VegasVegasThroughput.txt
rm -f VegasVegasDropcount.txt
rm -f VegasVegasLatency.txt
for i in 1 2 3 4 5 6 7 8 9 10
do
   ns NewRenoReno.tcl $i
   awk -f throughput.awk NewRenoReno$i.tr >>NewRenoRenoThroughput.txt
   echo >>NewRenoRenoThroughput.txt
   awk -f dropcount.awk NewRenoReno$i.tr >>NewRenoRenoDropcount.txt
   echo >>NewRenoRenoDropcount.txt
   awk -f latency.awk NewRenoReno$i.tr >>NewRenoRenoLatency.txt
   echo >>NewRenoRenoLatency.txt
   ns NewRenoVegas.tcl $i
   awk -f throughput.awk NewRenoVegas$i.tr >>NewRenoVegasThroughput.txt
   echo >>NewRenoVegasThroughput.txt
   awk -f dropcount.awk NewRenoVegas$i.tr >>NewRenoVegasDropcount.txt
   echo >>NewRenoVegasDropcount.txt
   awk -f latency.awk NewRenoVegas$i.tr >>NewRenoVegasLatency.txt
   echo >>NewRenoVegasLatency.txt
   ns RenoReno.tcl $i
   awk -f throughput.awk RenoReno$i.tr >>RenoRenoThroughput.txt
   echo >>RenoRenoThroughput.txt
   awk -f dropcount.awk RenoReno$i.tr >>RenoRenoDropcount.txt
   echo >>RenoRenoDropcount.txt
   awk -f latency.awk RenoReno$i.tr >>RenoRenoLatency.txt
   echo >>RenoRenoLatency.txt
   ns VegasVegas.tcl $i
   awk -f throughput.awk VegasVegas$i.tr >>VegasVegasThroughput.txt
   echo >>VegasVegasThroughput.txt
   awk -f dropcount.awk VegasVegas$i.tr >>VegasVegasDropcount.txt
   echo >>VegasVegasDropcount.txt
   awk -f latency.awk VegasVegas$i.tr >>VegasVegasLatency.txt
   echo >>VegasVegasLatency.txt
   rm -f *.tr
done
