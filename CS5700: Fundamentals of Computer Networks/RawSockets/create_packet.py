#!/usr/bin/python
import socket
import random
from struct import *


# Random Port selected which will be used in the entire execution
TCP_SOURCE_PORT = random.randint(49152, 65535)


def carry_around_add(a, b):
    c = a + b
    return (c & 0xffff) + (c >> 16)

# checksum calculation is done in this function:
# IP checksum = ~ (checksum(sum (fields in the header)))
# TCP checksum = ~(checksum ( TCP segement))
#TCP segement = psuedo_header + tcp_header  + data

def checksum(msg):
	s = 0
	for i in range(0, len(msg), 2):
		w = (ord(msg[i]) << 8)+ ord(msg[i+1])
		s = carry_around_add(s, w)
	return ~s & 0xffff



# Find IP of the destination or source port
def find_ip():
	s = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
 	s.connect (("8.8.8.8",80))
 	return s.getsockname()[0]

# Function to send a TCP packet
# - create an IP header
# - create a TCP header
# - packet = IP_header + TCP_header + data
# - set the appropriate flags for the type of TCP packet (eg : SYN , ACK, SYN_ACK, FIN_ACK)

def send_syn_packet (seq_no,ack_no,set_syn_flag,set_ack_flag,set_fin_flag,set_psh_flag,data,dest_ip):
	# now start constructing the packet
	global TCP_SOURCE_PORT
	packet = ''
	user_data = data
	source_ip = find_ip()

	# ip header fields
	ip_ihl = 5
	ip_ver = 4
	ip_tos = 0
	ip_tot_len = 0  # kernel will fill the correct total length
	ip_id = 0   #Id of this packet
	ip_frag_off = 0
	ip_ttl = 255
	ip_proto = socket.IPPROTO_TCP
	ip_check = 0    # kernel will fill the correct checksum
	ip_saddr = socket.inet_aton ( source_ip )   #Spoof the source ip address if you want to
	ip_daddr = socket.inet_aton ( dest_ip )
	ip_ihl_ver = (ip_ver << 4) + ip_ihl

	# the ! in the pack format string means network order
	pseudo_ip_header = pack('!BBHHHBBH4s4s' , ip_ihl_ver, ip_tos, ip_tot_len, ip_id, ip_frag_off, ip_ttl, 	ip_proto, ip_check, ip_saddr, ip_daddr)
	ip_check = checksum(pseudo_ip_header)
	ip_header = pack('!BBHHHBBH4s4s' , ip_ihl_ver, ip_tos, ip_tot_len, ip_id, ip_frag_off, ip_ttl, 	ip_proto, ip_check, ip_saddr, ip_daddr)
	if len(ip_header) % 2 == 1:
		ip_header = ip_header + '\x00'

	# tcp header fields
	tcp_source = TCP_SOURCE_PORT    # source port

	tcp_dest = 80   # destination port
	#tcp_seq = 454
	tcp_seq = seq_no
	#tcp_ack_seq = 0
	tcp_ack_seq = ack_no
	tcp_doff = 5    #4 bit field, size of tcp header, 5 * 4 = 20 bytes
	#tcp flags
	tcp_fin = set_fin_flag
	tcp_syn = set_syn_flag
	tcp_rst = 0
	tcp_psh = set_psh_flag
	tcp_ack = set_ack_flag
	tcp_urg = 0
	tcp_window = socket.htons (5840)    #   maximum allowed window size
	tcp_check = 0
	tcp_urg_ptr = 0


	tcp_offset_res = (tcp_doff << 4) + 0
	tcp_flags = tcp_fin + (tcp_syn << 1) + (tcp_rst << 2) + (tcp_psh <<3) + (tcp_ack << 4) + (tcp_urg << 5)

	# the ! in the pack format string means network order
	tcp_header = pack('!HHLLBBHHH' , tcp_source, tcp_dest, tcp_seq, tcp_ack_seq, tcp_offset_res, tcp_flags, \
					  tcp_window, tcp_check, tcp_urg_ptr)



	# pseudo header fields
	source_address = socket.inet_aton( source_ip )
	dest_address = socket.inet_aton(dest_ip)
	placeholder = 0
	protocol = socket.IPPROTO_TCP
	tcp_length = len(tcp_header) + len(user_data)
	psh = pack('!4s4sBBH' , source_address , dest_address , placeholder , protocol , tcp_length);
	psh = psh + tcp_header + user_data;
	if len(psh) % 2 != 0:
		psh += '\x00'

	tcp_check = checksum(psh)




	# make the tcp header again and fill the correct checksum - remember checksum is NOT in network byte order
	tcp_header = pack('!HHLLBBHHH' , tcp_source, tcp_dest, tcp_seq, tcp_ack_seq, tcp_offset_res, tcp_flags,  tcp_window, tcp_check, tcp_urg_ptr)

	# final full packet - syn packets dont have any data
	packet = ip_header + tcp_header + user_data
	ip_id = ip_id+1

	#Send the packet finally - the port specified has no effect
	return packet