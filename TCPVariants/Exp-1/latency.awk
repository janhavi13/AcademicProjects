#!/usr/bin/awk -f
BEGIN {
  received=0
  latency=0
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
        received = received + 1
        endtime[seq_num] = time
        latency=latency+(endtime[seq_num] - starttime[seq_num])
        }
  }

END {
  printf("%f", latency*1000/received);
}