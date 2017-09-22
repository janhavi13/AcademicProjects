import socket,sys,argparse



parser = argparse.ArgumentParser(description='Description of your program')
parser.add_argument('-sp', help='Server port not given', required=True)
args = vars(parser.parse_args())
port = int(sys.argv[2])


def sign_in():
    if address not in client_addresses.values():
            print dataFromClient
            if dataFromClient.split()[1] not in client_addresses.keys():

                username = dataFromClient.split()[1]
                listening_port = dataFromClient.split()[2]
                client_listening_ports[username] = [address[0], listening_port]
                client_addresses[username] = address
                message = ""
                server_socket.sendto(message, (address[0], address[1]))

            else:
                message = "User Already Exists .."
                server_socket.sendto(message, (address[0], address[1]))


def list_clients():
    response = 'Signed In Users: '
    for key, value in client_addresses.iteritems():
        if value != address:
            response += " " + key + ","
    server_socket.sendto(response, (address[0], address[1]))


def talk():
        user_to_check = dataFromClient.split()[2]
        if user_to_check in client_addresses and user_to_check in client_listening_ports:
            listening_port_of_user_to_check = client_listening_ports[user_to_check]
            listening_port_of_user_to_check = listening_port_of_user_to_check[0] + " " + listening_port_of_user_to_check[1]
            sender = client_addresses.keys()[client_addresses.values().index(address)]
            server_socket.sendto(listening_port_of_user_to_check + " " + sender, (address[0], address[1]))
        else:
            server_socket.sendto(user_to_check + " does not exist.", (address[0], address[1]))


acceptedPacket = {'SIGN-IN': sign_in, 'LIST': list_clients, 'MESSAGE': talk}


try:
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket.bind(('', port))
    print "Initializing Server"
except socket.error, msg:
    print "Error in initializing server ..."
    print 'Error Code : ' + str(msg[0]) + ' Message ' + msg[1]

client_addresses = {}
client_listening_ports = {}


while True:
    dataFromClient, address = server_socket.recvfrom(256)
    perform_task = dataFromClient.split()[0]

    if perform_task in acceptedPacket:
        acceptedPacket[perform_task]()
        continue
