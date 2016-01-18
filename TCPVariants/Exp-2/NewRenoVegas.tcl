#Create a simulator object
set ns [new Simulator]

#Define different colors for data flows (for NAM)
$ns color 1 Blue
$ns color 2 Red

#Open the NAM trace file
set nf [open out.nam w]
$ns namtrace-all $nf

#Open the Trace file
set tf [open NewRenoVegas[lindex $argv 0].tr w]
$ns trace-all $tf

#Define a 'finish' procedure
proc finish {} {
        global ns nf
        $ns flush-trace
        close $nf
        #exec nam out.nam &
        exit 0
}

#Create four nodes
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]
set n4 [$ns node]
set n5 [$ns node]
set n6 [$ns node]

#Create links between the nodes
$ns duplex-link $n1 $n2 10Mb 10ms DropTail
$ns duplex-link $n5 $n2 10Mb 10ms DropTail
$ns duplex-link $n2 $n3 10Mb 10ms DropTail
$ns duplex-link $n3 $n4 10Mb 10ms DropTail
$ns duplex-link $n3 $n6 10Mb 10ms DropTail

$ns queue-limit $n2 $n3 10


#Give node position (for NAM)
$ns duplex-link-op $n1 $n2 orient right-down
$ns duplex-link-op $n5 $n2 orient right-up
$ns duplex-link-op $n2 $n3 orient right
$ns duplex-link-op $n3 $n4 orient right-up
$ns duplex-link-op $n3 $n6 orient right-down

#Setup a TCP connection
set tcp1 [new Agent/TCP/Newreno]
$tcp1 set class_ 2
$ns attach-agent $n1 $tcp1
set sink [new Agent/TCPSink]
$ns attach-agent $n4 $sink
$ns connect $tcp1 $sink
$tcp1 set fid_ 1

#Setup a FTP over TCP connection
set ftp1 [new Application/FTP]
$ftp1 attach-agent $tcp1
$ftp1 set type_ FTP


#Setup a TCP connection
set tcp2 [new Agent/TCP/Vegas]
$tcp2 set class_ 2
$ns attach-agent $n5 $tcp2
set sink [new Agent/TCPSink]
$ns attach-agent $n6 $sink
$ns connect $tcp2 $sink
$tcp2 set fid_ 1

#Setup a FTP over TCP connection
set ftp2 [new Application/FTP]
$ftp2 attach-agent $tcp2
$ftp2 set type_ FTP


#Setup a UDP connection
set udp [new Agent/UDP]
$ns attach-agent $n2 $udp
set null [new Agent/Null]
$ns attach-agent $n3 $null
$ns connect $udp $null
$udp set fid_ 2

#Setup a CBR over UDP connection
set cbr [new Application/Traffic/CBR]
$cbr attach-agent $udp
$cbr set type_ CBR
$cbr set packet_size_ 1000
#$cbr set rate_ 1mb
$cbr set rate_ [lindex $argv 0]mb
$cbr set random_ false


#Schedule events for the CBR and FTP agents
$ns at 0.1 "$cbr start"
$ns at 60.5 "$cbr stop"
$ns at 0.5 "$ftp1 start"
$ns at 60.5 "$ftp1 stop"
$ns at 0.5 "$ftp2 start"
$ns at 61.0 "$ftp2 stop"


#Call the finish procedure after 5 seconds of simulation time
$ns at 61.0 "finish"

#Print CBR packet size and interval
puts "CBR packet size = [$cbr set packet_size_]"
puts "CBR interval = [$cbr set interval_]"

#Run the simulation
$ns run
