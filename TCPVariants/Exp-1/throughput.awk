#!/usr/bin/awk -f
BEGIN{

recdSize = 0
begintime = 0
endtime = 0
count = 0
}
{
event = $1
time = $2
src_node_id = $3
tgt_node_id = $4
pkt_type = $5
pkt_size = $6
src_sddr = $9
dest_addr = $10

if(pkt_type == "tcp" && src_sddr == "0.0" && event == "-" && dest_addr == "3.0" && count == 0)
{
begintime = time
count ++
}

if(pkt_type == "tcp" && src_node_id == "2" && event == "r" && tgt_node_id == "3")
	{
		endtime= time
		recdSize=recdSize+pkt_size 
	}
}
END {
printf("%f", (recdSize/(endtime - begintime))*8/1000);
}
