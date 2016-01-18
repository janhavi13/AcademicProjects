#!/usr/bin/awk -f
BEGIN{

recdSize1 = 0
begintime1 = 0
endtime1 = 0
count1 = 0
recdSize2 = 0
begintime2 = 0
endtime2 = 0
count2 = 0
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

if(pkt_type == "tcp" && src_sddr == "0.0" && event == "-" && dest_addr == "3.0" && count1 == 0)
{
begintime1 = time
count1 ++
}

if(pkt_type == "tcp" && src_node_id == "2" && event == "r" && tgt_node_id == "3")
{
endtime1 = time
recdSize1 =recdSize1 +pkt_size 
}

if(pkt_type == "tcp" && src_sddr == "4.0" && event == "-" && dest_addr == "5.0" && count2 == 0)
{
begintime2 = time
count2 ++
}

if(pkt_type == "tcp" && src_node_id == "2" && event == "r" && tgt_node_id == "5")
{
endtime2 = time
recdSize2 =recdSize2 +pkt_size 
}

}
END {

printf("%.2f\t%.2f", (recdSize1/(endtime1 - begintime1))*8/1000, (recdSize2/(endtime2 - begintime2))*8/1000);
}
