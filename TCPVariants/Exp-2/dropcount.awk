BEGIN {
  dropped1=0
  totalSent1=0
  dropped2=0
  totalSent2=0
  }
{
event = $1
pkt_type = $5
src_sddr = $9
dest_addr = $10
node_id = $3

  if(pkt_type == "tcp" && event == "d" && src_sddr == "0.0" && dest_addr == "3.0")
{
dropped1++
}
if(pkt_type == "tcp" && event == "-" && node_id == "0")
{
	totalSent1++
}
if(pkt_type == "tcp" && event == "d" && src_sddr == "4.0" && dest_addr == "5.0")
{
dropped2++
}
if(pkt_type == "tcp" && event == "-" && node_id == "4")
{
	totalSent2++
}
}
END {
printf("%f\t%f", dropped1*100/totalSent1,dropped2*100/totalSent2);
}