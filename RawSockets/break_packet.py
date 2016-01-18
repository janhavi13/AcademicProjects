#!/usr/bin/python
from create_packet import *

# Number of TCP Check validation fails
TCP_CHECKSUM_FAILS = 0
# Number of IP Check validation fails
IP_CHECKSUM_FAILS = 0
# flag to check retransission
RETRANSMISSION_IP = False
RETRANSMISSION_TCP = False
# flag to reject the non 200  OK requests
REJECT_RESPONSE = False
FIRST_SEGMENT = True

#Function to recieve packet from the sender
# GIVEN : Packet from the server
# RETURN: Specific data from the packet in an array
def recv_packet(packet):
	global TCP_CHECKSUM_FAILS
	global IP_CHECKSUM_FAILS
	global REJECT_RESPONSE
	global FIRST_SEGMENT
	packet = packet[0]
	ip_header = packet[0:20]

	#now unpack the packet received
	iph = unpack('!BBHHHBBH4s4s' , ip_header)
	version_ihl = iph[0]
	version = version_ihl >> 4
	ihl = version_ihl & 0xF
	iph_length = ihl * 4
	ip_tos = iph[1]
	ttl = iph[5]
	protocol = iph[6]
	s_addr = socket.inet_ntoa(iph[8]);
	d_addr = socket.inet_ntoa(iph[9]);
	ip_checksum = iph[7]
	ip_id = iph[2]

	# pack the recieved packet and calculate the checksum to verify if (received_checksum == calculated_checksum)
	pseudo_ip_header = pack('!BBHHHBBH4s4s' , iph[0], iph[1], iph[2], iph[3], iph[4], iph[5], iph[6], 0, iph[8], iph[9])
	if len(pseudo_ip_header) % 2 != 0:
		pseudo_ip_header += '\x00'

	calc_ip_check = checksum(pseudo_ip_header)
	if hex(calc_ip_check) != hex(ip_checksum):
		IP_CHECKSUM_FAILS+=1
		RETRANSMISSION_IP = True
	else:
		RETRANSMISSION_IP = False

	tcp_header = packet[iph_length:iph_length+20]
	tcph = unpack('!HHLLBBHHH' , tcp_header)

	source_port = tcph[0]
	dest_port = tcph[1]
	sequence = tcph[2]
	acknowledgement = tcph[3]
	doff_reserved = tcph[4]
	flags = tcph[5]
	recv_tcp_checksum = tcph[7]
	tcph_length = doff_reserved >> 4
	tcp_len_with_options = len(ip_header) + tcph_length*4
	h_size = iph_length + tcph_length * 4
	data_size = len(packet) - h_size
	data_with_header = packet[h_size:]


	# HTTP Request other than 200 OK should be discarded and the file should not be downloaded
	http_header_content = data_with_header.split('\r\n\r\n',1)[0]
	#print repr(http_header_content)
	if data_size != 0:
		if FIRST_SEGMENT:
			if not '200 OK' in http_header_content:
				REJECT_RESPONSE = True
			FIRST_SEGMENT = False



	# Get the data without the HTTP header in the data
	data = data_with_header.split('\r\n\r\n',1)[-1]
	options_len = tcp_len_with_options - 40

	tcp_complete_header = pack('!HHLLBBHHH' , tcph[0], tcph[1], tcph[2], tcph[3],tcph[4],tcph[5],tcph[6],0,tcph[8])
	tcp_seg_length = (tcph_length * 4) + data_size

	psh = pack('!4s4sBBH' , iph[8], iph[9] , 0 , iph[6] , tcp_seg_length)

	# The options are not included in the header, however the options are needed to calculate the checksum
	if options_len != 0:
		tcp_options_and_header = packet[40:40+options_len]
		psh = psh + tcp_complete_header + tcp_options_and_header + data_with_header
	else:
		psh = psh + tcp_complete_header + data_with_header

	if len(psh) % 2 != 0:
		psh += '\x00'

	calc_tcp_check = checksum(psh)

	if hex(recv_tcp_checksum) != hex(calc_tcp_check):
		TCP_CHECKSUM_FAILS += 1
		RETRANSMISSION_TCP = True
	else:
		RETRANSMISSION_TCP = False

   # gather the neccessary packet data from the packet in data1 []
	data1 = []
	data1.append(sequence)
	data1.append(acknowledgement)
	data1.append(flags)
	data1.append(data_size)
	data1.append(data)
	data1.append(recv_tcp_checksum)
	data1.append(ip_checksum)
	return data1

# Function : To accept Packets only from requested destination
def check_if_valid_packet(packet, dest_id):
	packet = packet[0]
	ip_header = packet[0:20]
	iph = unpack('!BBHHHBBH4s4s' , ip_header)
	version_ihl = iph[0]
	version = version_ihl >> 4
	ihl = version_ihl & 0xF
	iph_length = ihl * 4
	ttl = iph[5]
	protocol = iph[6]
	s_addr = socket.inet_ntoa(iph[8]);
	return s_addr==dest_id