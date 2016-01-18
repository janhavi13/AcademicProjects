BEGIN {
  dropped=0
  totalSent=0

  
}
{

event = $1
pkt_type = $5
src_sddr = $9
dest_addr = $10
node_id = $3

  if(pkt_type == "tcp" && event == "d" && src_sddr == "0.0" && dest_addr == "3.0")
{
dropped++
}
if(pkt_type == "tcp" && event == "-" && node_id == "0")
{
	totalSent++
}
}
END {
printf("%f", dropped*100/totalSent);
}