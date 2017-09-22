#!/usr/bin/awk -f
BEGIN {
  received1=0
  latency1=0
  received2=0
  latency2=0
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
  seq_num = $11

    if(event == "-" && src_node_id == "0" && tgt_node_id == "1")
      {
        starttime[seq_num] = time
        }

    if(event == "r" && src_node_id == 2 && src_sddr == "0.0" && dest_addr == "3.0" && tgt_node_id == 3)
      {
        received1 = received1 + 1
        endtime[seq_num] = time
        latency1=latency1+(endtime[seq_num] - starttime[seq_num])
        }
if(event == "-" && src_node_id == "4" && tgt_node_id == "1")
      {
        starttime[seq_num] = time
        }

    if(event == "r" && src_node_id == 2 && src_sddr == "4.0" && dest_addr == "5.0" && tgt_node_id == 5)
      {
        received2 = received1 + 1
        endtime[seq_num] = time
        latency2=latency2+(endtime[seq_num] - starttime[seq_num])
        }
  }

END {
  printf("%f\t%f", latency1*1000/received1,latency2*1000/received2);
  
}