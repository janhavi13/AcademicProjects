import socket, sys,threading,os
import argparse


class SendingMessage(threading.Thread):
    # Packet type sent to the server
    global protocol
    protocol = ['SIGN-IN', 'LIST', 'MESSAGE']
    # Commands given on the client
    global command
    command = ['send','list']


    def __init__(self, name, client_socket, server_socket):
        threading.Thread.__init__(self)
        self.name = name
        self.client_socket = client_socket
        self.server_socket = server_socket

    def run(self):
        try:
            server_socket.sendto(protocol[0] + " " + username + " " + str(client_socket.getsockname()[1]), (serverIp, port))
            recv_data, addr = server_socket.recvfrom(256)

        except:
            print "Unexpected error:", sys.exc_info()[0]
            os._exit(1)

        # if is to check if user exists. recv_data will have message to display
        if len(recv_data) > 0:
            print recv_data
            client_socket.close()
            server_socket.close()
            os._exit(1)
        else:
            while True:
                sys.stdout.write('+> ')
                sys.stdout.flush()
                message = sys.stdin.readline()
                # Check for invalid commands
                if message.rstrip().split()[0] not in command:
                    print "+> Enter list or send"

                if message.rstrip().startswith('list'):
                    if message.rstrip().split()[0] == 'list':
                        server_socket.sendto(protocol[1] + " " + message, (serverIp, port))
                        recv_data, addr = server_socket.recvfrom(256)
                        print "<- " + recv_data

                if message.rstrip().startswith('send'):
                    if message.rstrip().split()[0] == 'send':
                        server_socket.sendto(protocol[2] + " " + message, (serverIp, port))
                        recv_data, addr = server_socket.recvfrom(256)
                        sendMsg = ""
                        # to receive white space separated messages
                        for x in message.split()[2:]:
                            sendMsg = sendMsg + " " + x
                            sendMsg = sendMsg.strip()
                        if not sendMsg:
                            print "+> No Message Entered"
                        else:
                            try:
                                client_socket.sendto(sendMsg + " " + recv_data.split()[2], (recv_data.split()[0], int(recv_data.split()[1])))
                            except:
                                print "<- ", recv_data


class ReceiveMessage(threading.Thread):
    def __init__(self, name, client_socket):
        threading.Thread.__init__(self)
        self.name = name

    def run(self):
        while True:
            recv_data, addr = client_socket.recvfrom(256)
            recv_data = recv_data.strip()
            receivedMsg = ""
            # handle long messages which contain white spaces
            for x in recv_data.split()[0:-1]:
                receivedMsg = receivedMsg + " " + x
                receivedMsg = receivedMsg.strip()
            print "\r"
            print "<- <From ", addr[0], ":", addr[1], ":", recv_data.split()[-1], ">:" + receivedMsg
            sys.stdout.write('+>')
            sys.stdout.flush()


if __name__ == "__main__":

    # Command validation
    parser = argparse.ArgumentParser(description='Description of your program')
    parser.add_argument('-u', help='Username not given', required=True)
    parser.add_argument('-sip', help='Server IP not given', required=True)
    parser.add_argument('-sp', help='Server port not given', required=True)
    args = vars(parser.parse_args())
    username = sys.argv[2]
    serverIp = sys.argv[4]
    try:
        port = int(sys.argv[6])
    except:
        print "Please input the port number"
        sys.exit(1)

    client_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    client_socket.bind(('', 0))
    # senderThread performs the sending
    senderThread = SendingMessage("sender", client_socket, server_socket)
    # receiverThread performs listening
    receiverThread = ReceiveMessage("receiver", client_socket)
    senderThread.start()
    receiverThread.start()

