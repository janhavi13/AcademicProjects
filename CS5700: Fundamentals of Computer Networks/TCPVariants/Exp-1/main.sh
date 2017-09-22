
rm -f *.tr
rm -f *~
rm -f TCPTahoeThroughput.txt
rm -f TCPTahoeDropcount.txt
rm -f TCPTahoeLatency.txt

rm -f TCPVegasThroughput.txt
rm -f TCPVegasDropcount.txt
rm -f TCPVegasLatency.txt

rm -f TCPRenoThroughput.txt
rm -f TCPRenoDropcount.txt
rm -f TCPRenoLatency.txt

rm -f TCPNewrenoThroughput.txt
rm -f TCPNewrenoDropcount.txt
rm -f TCPNewrenoLatency.txt

for i in 1 1.5 2 2.5 3 3.5 4 4.5 5 5.5 6 6.5 7 7.5 8 8.5 9 9.5 10
do
   ns TCPTahoe.tcl $i
   awk -f throughput.awk TCPTahoe$i.tr >>TCPTahoeThroughput.txt
   echo >>TCPTahoeThroughput.txt
   awk -f dropcount.awk TCPTahoe$i.tr >>TCPTahoeDropcount.txt
   echo >>TCPTahoeDropcount.txt
   awk -f latency.awk TCPTahoe$i.tr >>TCPTahoeLatency.txt
   echo >>TCPTahoeLatency.txt
   ns TCPReno.tcl $i
   awk -f throughput.awk TCPReno$i.tr >>TCPRenoThroughput.txt
   echo >>TCPRenoThroughput.txt
   awk -f dropcount.awk TCPReno$i.tr >>TCPRenoDropcount.txt
   echo >>TCPRenoDropcount.txt
   awk -f latency.awk TCPReno$i.tr >>TCPRenoLatency.txt
   echo >>TCPRenoLatency.txt
   ns TCPNewreno.tcl $i
   awk -f throughput.awk TCPNewreno$i.tr >>TCPNewrenoThroughput.txt
   echo >>TCPNewrenoThroughput.txt
   awk -f dropcount.awk TCPNewreno$i.tr >>TCPNewrenoDropcount.txt
   echo >>TCPNewrenoDropcount.txt
   awk -f latency.awk TCPNewreno$i.tr >>TCPNewrenoLatency.txt
   echo >>TCPNewrenoLatency.txt
   ns TCPVegas.tcl $i
   awk -f throughput.awk TCPVegas$i.tr >>TCPVegasThroughput.txt
   echo >>TCPVegasThroughput.txt
   awk -f dropcount.awk TCPVegas$i.tr >>TCPVegasDropcount.txt
   echo >>TCPVegasDropcount.txt
   awk -f latency.awk TCPVegas$i.tr >>TCPVegasLatency.txt
   echo >>TCPVegasLatency.txt
   rm -f *.tr
done