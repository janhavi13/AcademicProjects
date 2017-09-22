import os
import socket
import sys
import select
import threading
from cryptography.hazmat.primitives import serialization
from communication import send, receive
import constants
import utility_functions
import signal

key = None
iv = None
message_to_send = None
connected_users = {}
clients_connected = {}


class ClientThread(threading.Thread):
    """
    A thread that listens/talks to other clients
    """
    def __init__(self, *args, **kwargs):
        """
        the constructor

        :param args: arguments
        :param kwargs: keyword arguments
        :return: void
        """
        threading.Thread.__init__(self)
        self.client_socket = kwargs.get('client_socket')
        self.private_key = kwargs.get('private_key')
        self.current_client_secret = kwargs.get('current_client_secret')
        self.username = kwargs.get('username')
        self.client_client_session_key = None
        self.talking_socket = None
        self.recipient_username = None

    def create_new_talking_socket(self, address, port):
        """
        Creates a new listening socket that this client thread can use to listen to connections

        :param address: the address to listen on
        :param port: the port to listen on
        :return: socket
        """
        recipient_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        recipient_socket.connect((address, int(port)))
        self.talking_socket = recipient_socket
        return recipient_socket

    def identify_ticket(self, data):
        """
        extracts the ticket and the associated username/address from the received packet

        :param data: the received packet
        :return: the reply for this message, of packet type: talk_request_reply
        """
        ticket = data['ticket']
        ticket_plaintext = utility_functions.public_key_decrypt(
            self.private_key,
            ticket
        )
        ticket_deserialized = utility_functions.pickle_helper(ticket_plaintext, constants.PICKLE_DESERIALIZE)
        global key
        key = ticket_deserialized[constants.PACKET_KEY_KEY]
        global iv
        iv = ticket_deserialized[constants.PACKET_IV_KEY]
        decrypted = utility_functions.symmetric_key_decrypt(key, iv, data[constants.PACKET_ENCRYPTED_KEY])
        decrypted = utility_functions.pickle_helper(decrypted, constants.PICKLE_DESERIALIZE)
        sender_username = ticket_deserialized['sender_username']
        sender_port = ticket_deserialized['slp']
        sender_address = ticket_deserialized['sender_address']
        data = {}
        data[constants.PACKET_USERNAME_KEY] = decrypted[constants.PACKET_USERNAME_KEY]
        data['parameter'] = utility_functions.calculate_diffie_hellman_contribution(self.current_client_secret)
        data[constants.PACKET_NONCE_1_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        data['address'] = decrypted['address']
        data['listening_port'] = decrypted['listening_port']
        data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        packet = {}
        packet[constants.PACKET_PACKET_TYPE_KEY] = 'talk_request_reply'
        packet[constants.PACKET_ENCRYPTED_KEY] = utility_functions.symmetric_key_encrypt(key, iv, data)
        self.talking_socket = self.create_new_talking_socket(sender_address, sender_port)
        return packet

    def run(self):
        """
        the run method for this thread

        :return: void
        """
        self.client_socket.listen(5)
        while True:
            client, address = self.client_socket.accept()
            data = receive(client)

            if data[constants.PACKET_PACKET_TYPE_KEY] == 'talk_request':
                reply = self.identify_ticket(data)
                send(self.talking_socket, reply)
            elif data[constants.PACKET_PACKET_TYPE_KEY] == 'talk_request_reply':
                reply = self.construct_exchange_packet(data)
                send(self.talking_socket, reply)
                message = self.construct_message()
                send(self.talking_socket, message)
            elif data[constants.PACKET_PACKET_TYPE_KEY] == 'talk_request_final':
                self.construct_session_key(data)
                data = receive(client)
                if data:
                    self.decrypt_messsage(data)
            elif data[constants.PACKET_PACKET_TYPE_KEY] == 'message':
                self.decrypt_messsage(data)

    def decrypt_messsage(self, packet):
        """
        decrypts and prints the contents of a packet of type: message
        :param packet: the received packet
        :return: void
        """
        packet = utility_functions.aes_decrypt(
            self.client_client_session_key,
            packet[constants.PACKET_ENCRYPTED_KEY]
        )
        packet = utility_functions.pickle_helper(packet, constants.PICKLE_DESERIALIZE)
        print packet['message']

    def construct_message(self):
        """
        constructs a packet of type 'message' using instance data associated with this thread

        :return: the constructed packet
        """
        data = {}
        data['message'] = self.username + '-> ' + message_to_send
        data[constants.PACKET_NONCE_1_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        encrypted = utility_functions.aes_encrypt(
            self.client_client_session_key,
            utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        )
        packet = {}
        packet[constants.PACKET_PACKET_TYPE_KEY] = 'message'
        packet[constants.PACKET_ENCRYPTED_KEY] = encrypted
        return packet

    def construct_session_key(self, data):
        """
        constructs the shared session key that this client will use to talk to other clients
        if this is successful, add the received user to a list of connected users

        :param data: the data that will be used to construct the key
        :return: void
        """
        encrypted = data[constants.PACKET_ENCRYPTED_KEY]
        plaintext = utility_functions.symmetric_key_decrypt(key, iv, encrypted)
        plaintext = utility_functions.pickle_helper(plaintext, constants.PICKLE_DESERIALIZE)
        received_parameter = plaintext['parameter']
        self.client_client_session_key = utility_functions.generate_shared_secret(
            self.current_client_secret,
            received_parameter
        )
        global connected_users
        connected_users[self.recipient_username] = (self.client_client_session_key, self.talking_socket)

    def construct_exchange_packet(self, data):
        """
        construct the packet that the client will send as a response to a talk request
        this involves computing the diffie-hellman contribution and establishing a talking socket
        with the received address/port combination

        :param data: the received packet of type: talk_request_reply
        :return: the new packet
        """
        reply = {}
        encrypted = data[constants.PACKET_ENCRYPTED_KEY]
        plaintext = utility_functions.symmetric_key_decrypt(key, iv, encrypted)
        plaintext = utility_functions.pickle_helper(plaintext, constants.PICKLE_DESERIALIZE)
        received_parameter = plaintext['parameter']
        self.client_client_session_key = utility_functions.generate_shared_secret(
            self.current_client_secret,
            received_parameter
        )
        sender_port = plaintext['listening_port']
        sender_address = plaintext['address']
        self.recipient_username = plaintext['username']
        self.create_new_talking_socket(sender_address, sender_port)

        data = {}
        global clients_connected
        clients_connected[self.recipient_username] = (self.client_client_session_key, sender_port)
        data['parameter'] = utility_functions.calculate_diffie_hellman_contribution(self.current_client_secret)
        data[constants.PACKET_NONCE_1_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        reply[constants.PACKET_PACKET_TYPE_KEY] = 'talk_request_final'
        reply[constants.PACKET_ENCRYPTED_KEY] = utility_functions.symmetric_key_encrypt(key, iv, data)
        return reply


class ChatClient(object):
    """
    The ChatClient
    """
    def __init__(self, name, host='127.0.0.1', port=3490):
        """
        the constructor

        :param name: the name of this client
        :param host: the host
        :param port: the port

        :return: void
        """
        signal.signal(signal.SIGTSTP, self.interrupt_handler)
        self.name = name
        # Quit flag
        self.flag = False
        self.port = int(port)
        self.host = host
        self.current_private_key = None
        self.current_public_key = None
        self.current_nonce = None
        self.current_server_shared_key = None
        self.known_users = {}
        self.inputs = None
        self.outputs = None
        self.current_server_nonce = None
        self.username = None
        self.listening_port = None

        try:
            self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.sock.connect((host, self.port))
            print 'Connected to the server!'

            global inputs
            inputs = [0, self.sock]
            self.inputs = [0, self.sock]
            self.outputs = []

            # send the initial packets to establish a connection with the server
            send(self.sock, self.construct_connect_packet())
            data = receive(self.sock)
            send(self.sock, self.construct_new_packet(data['r']))
            data = receive(self.sock)
            received_plain_text_data = utility_functions.public_key_decrypt(
                self.current_private_key,
                data[constants.PACKET_ENCRYPTED_KEY]
            )
            received_deserialized_data = utility_functions.pickle_helper(
                received_plain_text_data,
                constants.PICKLE_DESERIALIZE
            )
            if self.verify_nonce(received_deserialized_data[constants.PACKET_NONCE_1_KEY]):
                self.current_nonce = received_deserialized_data[constants.PACKET_NONCE_2_KEY]
                self.current_server_nonce = self.current_nonce

                # generate shared key
                self.current_server_shared_key = utility_functions.generate_shared_secret(
                    self.current_client_secret,
                    received_deserialized_data['parameter']
                )
                sys.stdout.write('Username:>')
                sys.stdout.flush()
                entered_username = sys.stdin.readline().strip()
                self.username = entered_username
                sys.stdout.write('Password:>')
                sys.stdout.flush()
                entered_password = sys.stdin.readline().strip()
                send(self.sock, self.create_username_password_packet(
                    entered_username,
                    entered_password
                ))
                data = receive(self.sock)
                received_plain_text_data = utility_functions.aes_decrypt(
                    self.current_server_shared_key,
                    data[constants.PACKET_ENCRYPTED_KEY]
                )
                received_deserialized_data = utility_functions.pickle_helper(
                        received_plain_text_data,
                        constants.PICKLE_DESERIALIZE
                )
                if self.verify_nonce(received_deserialized_data[constants.PACKET_NONCE_1_KEY]):
                    self.current_nonce = received_deserialized_data[constants.PACKET_NONCE_2_KEY]
                    self.current_server_nonce = self.current_nonce
                    if data[constants.PACKET_PACKET_TYPE_KEY] == constants.SERVER_LOGIN_SUCCESS_STATUS:
                        self.create_new_listening_socket('', received_deserialized_data['listening_port'], entered_username)
                        print 'You are logged in. Enter a command!'
                    else:
                        print 'Login Failure!'
                        self.flag = True
                else:
                    print 'Nonce mismatch!'
                    self.flag = True

            else:
                print 'Nonce mismatch'
                self.flag = True
        except socket.error, e:
            print e
            print 'Could not connect to chat server @%d' % self.port
            sys.exit(1)

    def interrupt_handler(self, signum, frame):
        """
        helper to handle interrupts from keyboard

        :param signum: the signum
        :param frame: the frame
        :return: void
        """
        self.sock.close()
        sys.exit('Interrupted!')

    def construct_message(self, username):
        """
        constructs a packet with the message

        :param username: the current username; ie the username of the user sending the message
        :return: the new packet
        """
        data = {}
        # global connected_users
        # session_key = connected_users[username][0]
        global clients_connected
        session_key = clients_connected[username][0]
        data['message'] = self.username + '-> ' + message_to_send
        data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        encrypted = utility_functions.aes_encrypt(
            session_key,
            utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        )
        message = encrypted
        return message

    def construct_new_packet(self, received_random):
        """
        constructs a new packet of type: dh_contribution
        this involves computing of one's own diffie-hellman contribution and using the shared secret

        :param received_random: the received random number from the previous packet
        :return: the new packet
        """
        data = {}
        server_public_key = utility_functions.read_server_public_key('server_keys/pub_4096_s.der')
        keys = utility_functions.generate_private_public_key()
        self.current_private_key = keys[0]
        self.current_public_key = keys[1]

        data[constants.PACKET_CLIENT_PUBLIC_KEY_KEY] = keys[1].public_bytes(
            encoding=serialization.Encoding.DER,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        )

        # generate and set the new nonce as current nonce
        self.current_nonce = utility_functions.random_number_generator(constants.NONCE_BITS)
        data[constants.PACKET_NONCE_1_KEY] = self.current_nonce
        self.current_server_nonce = self.current_nonce
        self.current_client_secret = utility_functions.generate_secret()
        data['parameter'] = utility_functions.calculate_diffie_hellman_contribution(self.current_client_secret)

        serialized_data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        packet = {}
        packet[constants.PACKET_PACKET_TYPE_KEY] = constants.DH_PACKET_TYPE
        packet[constants.PACKET_ENCRYPTED_KEY] = utility_functions.public_key_encrypt(server_public_key, serialized_data)
        packet['r'] = received_random
        return packet

    def verify_nonce(self, new_nonce):
        """
        verifies that the new nonce and the current nonce are consecutive
        :param new_nonce: the new nonce
        :return: True if the verification is successful, false otherwise
        """
        return new_nonce == self.current_nonce + 1

    def construct_command_request_packet(self, *args, **kwargs):
        """
        constructs a packet of type: command
        uses the input arguments to append the command and it's associated data if any

        :param args: arguments
        :param kwargs: keyword arguments; command: the entered command; command_data: associated command data
        :return: the new packet
        """
        data = {}
        command = kwargs.get('command')
        command_data = kwargs.get('command_data', {})
        for key, value in command_data.iteritems():
            data[key] = value
        data[constants.PACKET_NONCE_1_KEY] = self.current_server_nonce + 1
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
        self.current_server_nonce = self.current_nonce
        data[constants.COMMAND_PACKET_TYPE] = command
        data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        packet = {}
        packet[constants.PACKET_PACKET_TYPE_KEY] = constants.COMMAND_PACKET_TYPE
        packet[constants.PACKET_ENCRYPTED_KEY] = utility_functions.aes_encrypt(
            self.current_server_shared_key,
            data
        )
        return packet

    def construct_connect_packet(self):
        """
        constructs the initial packet of type 'connect'

        this is then sent by the client to the server
        :return: the new packet
        """
        data = {}
        data[constants.PACKET_PACKET_TYPE_KEY] = constants.CONNECT_PACKET_TYPE
        return data

    def create_username_password_packet(self, username, password):
        """
        create the packet of type 'login_credentials' that holds the username and the password that
        the current client wishes to login with

        :param username: the username
        :param password: the password
        :return: the new packet
        """
        data = {}
        data[constants.PACKET_USERNAME_KEY] = username
        data[constants.PACKET_PASSWORD_KEY] = password
        data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
        self.current_server_nonce = self.current_nonce
        serialized_packet = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        encrypted_serialized_packet = utility_functions.aes_encrypt(self.current_server_shared_key, serialized_packet)
        packet = {}
        packet[constants.PACKET_PACKET_TYPE_KEY] = constants.LOGIN_CREDENTIALS_PACKET_TYPE
        packet[constants.PACKET_ENCRYPTED_KEY] = encrypted_serialized_packet
        return packet

    def create_new_listening_socket(self, address, port, username):
        """
        creates a new listing socket and starts an associated thread

        :param address: the address for the new socket to listen to
        :param port: the port for the new socket to listen to
        :param username: the username to maintain thread state
        :return: void
        """
        print 'Now listening for connections on port: ' + str(port)
        new_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        new_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        new_socket.bind((address, port))
        ClientThread(
            client_socket=new_socket,
            private_key=self.current_private_key,
            current_client_secret=self.current_client_secret,
            username=username
        ).start()
        return new_socket

    def create_new_talking_socket(self, address, port):
        """
        creates a new talking socket that will connect to a prospective client

        :param address: the address
        :param port: the port
        :return: the new socket
        """
        recipient_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        recipient_socket.connect((address, int(port)))
        return recipient_socket

    def construct_client_talk_request_packet(self, recipient_info):
        """
        constructs the packet of type 'talk_request' that has the data that a client sends to another client

        :param recipient_info: the recipient information to include
        :return: the new packet
        """
        packet = {}
        global key
        key = recipient_info[constants.PACKET_KEY_KEY]
        global iv
        iv = recipient_info[constants.PACKET_IV_KEY]
        to_encrypt = {}
        to_encrypt['address'] = recipient_info['address']
        to_encrypt['listening_port'] = recipient_info['listening_port']
        to_encrypt[constants.PACKET_USERNAME_KEY] = recipient_info[constants.PACKET_USERNAME_KEY]
        to_encrypt[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        to_encrypt[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        self.current_nonce = to_encrypt[constants.PACKET_NONCE_2_KEY]
        to_encrypt[constants.PACKET_PACKET_TYPE_KEY] = 'talk_request'
        to_encrypt = utility_functions.pickle_helper(
            to_encrypt,
            constants.PICKLE_SERIALIZE
        )
        encrypted = utility_functions.symmetric_key_encrypt(key, iv, to_encrypt)
        packet[constants.PACKET_PACKET_TYPE_KEY] = 'talk_request'
        packet['ticket'] = recipient_info['ticket']
        packet[constants.PACKET_ENCRYPTED_KEY] = encrypted
        return packet

    def cmdloop(self):

        while not self.flag:
            try:
                # Wait for inputs
                inputready, outputready, exceptready = select.select(inputs, self.outputs, [])

                for i in inputready:
                    if i == 0:
                        data = sys.stdin.readline().strip()
                        if data:
                            if data == constants.COMMAND_LIST:
                                data = self.construct_command_request_packet(
                                    command=constants.COMMAND_LIST
                                )
                                send(self.sock, data)
                            elif data == constants.COMMAND_LOGOUT:
                                data = self.construct_command_request_packet(
                                    command=constants.COMMAND_LOGOUT
                                )
                                send(self.sock, data)
                            elif data.startswith(constants.COMMAND_SEND):
                                if data.rstrip().split()[0] == 'send':
                                    username_to_send = data.split()[1]
                                    global message_to_send
                                    entered_message = ''
                                    for x in data.split()[2:]:
                                        entered_message = entered_message + ' ' + x
                                        entered_message = entered_message.strip()
                                    if not entered_message:
                                        print 'No Message to send!'
                                    else:
                                        message_to_send = entered_message
                                        global connected_users
                                        if username_to_send == self.username:
                                            print 'Cannot send to self!'
                                        else:
                                            global clients_connected
                                            if username_to_send in clients_connected:
                                                data = self.construct_message(username_to_send)
                                                packet = {}
                                                packet[constants.PACKET_ENCRYPTED_KEY] = data
                                                packet[constants.PACKET_PACKET_TYPE_KEY] = 'message'
                                                recipient_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                                                recipient_socket.connect(('127.0.0.1', int(clients_connected[username_to_send][1])))
                                                send(recipient_socket,packet)
                                            else:
                                                data = self.construct_command_request_packet(
                                                    command=constants.COMMAND_SEND,
                                                    command_data={constants.PACKET_USERNAME_KEY: username_to_send}
                                                )
                                                send(self.sock, data)
                                else:
                                    print 'Enter send to send a message!'
                            else:
                                print 'Invalid command.'

                    elif i == self.sock:
                        data = receive(self.sock)

                        if not data:
                            print 'Shutting down.'
                            self.flag = True
                            break
                        else:
                            # handle a reply
                            received_plain_text_data = utility_functions.aes_decrypt(
                                self.current_server_shared_key,
                                data[constants.PACKET_ENCRYPTED_KEY]
                            )
                            received_deserialized_data = utility_functions.pickle_helper(
                                    received_plain_text_data,
                                    constants.PICKLE_DESERIALIZE
                            )
                            if self.verify_nonce(received_deserialized_data[constants.PACKET_NONCE_1_KEY]):
                                self.current_nonce = received_deserialized_data[constants.PACKET_NONCE_2_KEY]
                                self.current_server_nonce = self.current_nonce
                                command_output = received_deserialized_data[constants.PACKET_COMMAND_OUTPUT_KEY]
                                if command_output == constants.SERVER_LOGGED_OUT_STATUS:
                                    print 'Logged out!'
                                    self.flag = True
                                    self.sock.close()
                                    os._exit(0)
                                elif command_output == 'user_id_success':
                                    # handle a reply of type: user_id_success
                                    print 'Successfully obtained user information from server!'
                                    self.known_users[received_deserialized_data[constants.PACKET_USERNAME_KEY]] = data
                                    self.current_nonce = received_deserialized_data[constants.PACKET_NONCE_2_KEY]
                                    self.current_server_nonce = self.current_nonce
                                    client_listening_socket = self.create_new_talking_socket(
                                        received_deserialized_data['address'],
                                        received_deserialized_data['listening_port']
                                    )
                                    talk_request = self.construct_client_talk_request_packet(received_deserialized_data)
                                    send(client_listening_socket, talk_request)
                                elif command_output == constants.INVALID_PACKET_TYPE:
                                    # handle a reply of packet type: invalid
                                    self.current_nonce = received_deserialized_data[constants.PACKET_NONCE_2_KEY]
                                    self.current_server_nonce = self.current_nonce
                                    print received_deserialized_data[constants.PACKET_ERROR_KEY]
                                else:
                                    # print the output
                                    print command_output
                            else:
                                print 'Nonce mismatch!'
                                self.flag = True
                                self.sock.close()
                                break
                    else:
                        # handle communication requests from other clients
                        data = receive(i)

            except KeyboardInterrupt:
                print 'Interrupted!'
                self.sock.close()
                break

if __name__ == "__main__":
    client = ChatClient('a', '127.0.0.1', 50000)
    client.cmdloop()