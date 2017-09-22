import os
from random import randrange
import select
import socket
import sys
import signal
import hashlib
from communication import send, receive
import constants
import utility_functions
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.hazmat.backends import default_backend


class ClientState:

    def __init__(self, *args, **kwargs):
        self.state = None
        self.reply = None
        self.current_nonce = None
        self.current_secret = None
        self.client_ip_address_tuple = kwargs.get('client_ip_address_tuple', None)
        self.current_shared_key = None
        self.credentials = kwargs.get('credentials', None)
        self.command = None
        self.username = None
        self.received_data = None
        self.client_public_key = None
        self.listening_port = None

    def fix_listening_port(self):
        """
        fix the port number that this client will listen to for messages
        from other clients

        :return: void
        """
        port_to_exclude = self.get_client_ip_address_tuple()[1]
        flag = True
        while flag:
            random_port = randrange(40000, 50000)
            if random_port != port_to_exclude:
                flag = False
                self.listening_port = random_port

    def get_listening_port(self):
        """
        getter for listening_port_number
        :return:
        """
        return self.listening_port

    def get_client_ip_address_tuple(self):
        """
        getter for client_ip_address_tuple
        :return:
        """
        return self.client_ip_address_tuple

    def get_structured_client_ip_address(self):
        return self.get_client_ip_address_tuple()[0] + ':' + str(self.get_client_ip_address_tuple()[1])

    def get_username(self):
        return self.username

    def get_command(self):
        return self.command

    def get_state(self):
        return self.state

    def get_reply(self):
        return self.reply

    def set_reply(self, reply):
        self.reply = reply

    def get_received_data(self):
        return self.received_data

    def get_client_public_key(self):
        return self.client_public_key

    def verify_nonce(self, new_nonce):
        return new_nonce == self.current_nonce + 1

    def construct_login_attempt_reply(self):
        """
        construct the packet that gets sent when someone sends a login packet

        :return:
        """
        to_encrypt = {}
        to_encrypt[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        n4 = utility_functions.random_number_generator(constants.NONCE_BITS)
        self.current_nonce = n4
        if self.state == constants.SERVER_LOGIN_SUCCESS_STATUS:
            to_encrypt['listening_port'] = self.listening_port
        to_encrypt[constants.PACKET_NONCE_2_KEY] = n4
        to_encrypt = utility_functions.pickle_helper(to_encrypt, constants.PICKLE_SERIALIZE)
        encrypted = utility_functions.aes_encrypt(self.current_shared_key, to_encrypt)
        packet = {}
        packet[constants.PACKET_ENCRYPTED_KEY] = encrypted
        packet[constants.PACKET_PACKET_TYPE_KEY] = self.state
        self.reply = packet

    def verify_login_credentials(self, username, password):
        """
        verifies that the username and password match the stored ones
        :param username: the username to check
        :param password: the password to check
        :return:
        """
        salt = self.credentials[username][0]
        stored_password = self.credentials[username][1]
        #p = repr(stored_password)
        #salted_password = utility_functions.verify_password(salt, password)
        generated_password = hashlib.sha256(salt.encode() + password.encode()).hexdigest()
        #if stored_password == salted_password:
        #    print 'valid'
        return generated_password ==stored_password

    def private_key_decrypt(self, packet):
        """
        use the private key of the server to decrypt the contents of packet['encrypted']

        :param packet: the input packet
        :return: the deserialized and decrypted packet
        """
        server_private_key = utility_functions.read_server_private_key('server_keys/pri_4096_s.der')
        decrypted_packet = utility_functions.public_key_decrypt(
            server_private_key,
            packet[constants.PACKET_ENCRYPTED_KEY]
        )
        deserialized_decrypted_packet = utility_functions.pickle_helper(decrypted_packet, constants.PICKLE_DESERIALIZE)
        return deserialized_decrypted_packet

    def shared_key_decrypt_and_deserialize(self, encrypted_data):
        """
        use the current shared session key between the server and the client to decrypt the input

        :param encrypted_data: the data to decrypt
        :return: the decrypted and deserialized data
        """
        decrypted_packet = utility_functions.aes_decrypt(self.current_shared_key, encrypted_data)
        return utility_functions.pickle_helper(decrypted_packet, constants.PICKLE_DESERIALIZE)

    def shared_key_encrypt(self, data):
        """
        use the current shared session key between the server and the client to decrypt the input

        :param data: the data to encrypt
        :return: the encrypted data
        """
        return utility_functions.aes_encrypt(self.current_shared_key, data)

    def construct_diffie_hellman_packet(self):
        """
        construct the packet where the diffie hellman exchange happens between the client and the server
        :return: void
        """
        client_public_key = self.client_public_key
        data = {}
        data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
        self.current_secret = utility_functions.generate_secret()
        data['parameter'] = utility_functions.calculate_diffie_hellman_contribution(self.current_secret)
        serialized_data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        reply_packet = {}
        reply_packet[constants.PACKET_PACKET_TYPE_KEY] = constants.DH_PACKET_TYPE
        key = load_der_public_key(client_public_key, backend=default_backend())
        reply_packet[constants.PACKET_ENCRYPTED_KEY] = utility_functions.public_key_encrypt(key, serialized_data)
        self.reply = reply_packet

    def construct_list_packet(self, username_list):
        """
        construct the server response to the list command
        :param username_list: the list to include in the packet
        :return: void
        """
        data = {}
        data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
        data[constants.PACKET_COMMAND_OUTPUT_KEY] = username_list
        data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        data = utility_functions.aes_encrypt(self.current_shared_key, data)
        packet = {}
        packet[constants.PACKET_ENCRYPTED_KEY] = data
        packet[constants.PACKET_PACKET_TYPE_KEY] = constants.COMMAND_REPLY_PACKET_TYPE

        self.reply = packet

    def construct_logout_packet(self):
        """
        construct the server reply to the logout command
        :return: void
        """
        data = {}
        data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
        data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
        data[constants.PACKET_COMMAND_OUTPUT_KEY] = constants.SERVER_LOGGED_OUT_STATUS
        data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        data = utility_functions.aes_encrypt(self.current_shared_key, data)
        packet = {}
        packet[constants.PACKET_ENCRYPTED_KEY] = data
        self.reply = packet

    def construct_invalid_packet(self):
        data = {}
        data[constants.PACKET_COMMAND_OUTPUT_KEY] = constants.SERVER_LOGGED_OUT_STATUS
        data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
        data = utility_functions.aes_encrypt(self.current_shared_key, data)
        packet = {}
        packet[constants.PACKET_ENCRYPTED_KEY] = data
        self.reply = packet



    def construct_initial_reply(self):
        """
        constructs the server reply to the initial connect attempt of the client
        :return:
        """
        data = {}
        r = utility_functions.random_number_generator(128)
        data['r'] = r
        hash_dict = {}
        hash_dict['client_ip'] = self.client_ip_address_tuple[0]
        hash = utility_functions.pickle_helper(hash_dict, constants.PICKLE_SERIALIZE)
        data['hash'] = utility_functions.sha256_generator(hash)
        data['packet_type'] = 'connect_reply'
        self.reply = data

    def construct_send_command_reply(self, user_info):
        """
        constructs the server reply to the send command of the client
        :param user_info: a dictionary having information associated with the requested username
        :return: void
        """
        data = {}
        ticket = {}
        packet = {}
        try:
            key = os.urandom(constants.KEY_SIZE_BYTES)
            iv = os.urandom(constants.IV_SIZE_BYTES)
            ticket[constants.PACKET_KEY_KEY] = key
            ticket[constants.PACKET_IV_KEY] = iv
            encrypted_ticket = None
            ticket[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
            recipient_public_key = user_info['public_key']
            ticket['sender_username'] = user_info['sender_username']
            ticket['sender_address'] = user_info['sender_address']
            ticket['slp'] = user_info['sender_listening_port']
            ticket_string = utility_functions.pickle_helper(ticket, constants.PICKLE_SERIALIZE)
            if recipient_public_key is not None:
                recipient_public_key = load_der_public_key(recipient_public_key, backend=default_backend())
                encrypted_ticket = utility_functions.public_key_encrypt(recipient_public_key, ticket_string)

            data['ticket'] = encrypted_ticket
            data[constants.PACKET_KEY_KEY] = key
            data[constants.PACKET_IV_KEY] = iv
            data[constants.PACKET_USERNAME_KEY] = user_info['username']
            data['address'] = user_info['address']
            data['listening_port'] = user_info['listening_port']
            data['new_port'] = self.listening_port
            data[constants.PACKET_COMMAND_OUTPUT_KEY] = 'user_id_success'
            data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
            data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
            self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
            data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
            data = utility_functions.aes_encrypt(self.current_shared_key, data)
            packet[constants.PACKET_PACKET_TYPE_KEY] = constants.COMMAND_REPLY_PACKET_TYPE
            packet[constants.PACKET_ENCRYPTED_KEY] = data
        except KeyError:
            # invalid user
            data[constants.PACKET_COMMAND_OUTPUT_KEY] = constants.INVALID_PACKET_TYPE
            data[constants.PACKET_NONCE_1_KEY] = self.current_nonce + 1
            data[constants.PACKET_NONCE_2_KEY] = utility_functions.random_number_generator(constants.NONCE_BITS)
            data[constants.PACKET_ERROR_KEY] = 'User information not found: ' + user_info[constants.PACKET_USERNAME_KEY]
            self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
            data = utility_functions.pickle_helper(data, constants.PICKLE_SERIALIZE)
            data = utility_functions.aes_encrypt(self.current_shared_key, data)
            packet[constants.PACKET_PACKET_TYPE_KEY] = constants.COMMAND_REPLY_PACKET_TYPE
            packet[constants.PACKET_ENCRYPTED_KEY] = data

        self.reply = packet

    def update_state(self, data):
        """
        updates the current client state based on the input data
        :param data: data received from the client
        :return: void
        """
        if data[constants.PACKET_PACKET_TYPE_KEY] == constants.CONNECT_PACKET_TYPE:
            self.state = 'Initial'
            self.construct_initial_reply()
            self.received_data = data
        elif data[constants.PACKET_PACKET_TYPE_KEY] == constants.DH_PACKET_TYPE:
            self.state = 'Diffie'
            data = self.private_key_decrypt(data)
            # store the received nonce as the current nonce
            self.current_nonce = data[constants.PACKET_NONCE_1_KEY]
            self.client_public_key = data[constants.PACKET_CLIENT_PUBLIC_KEY_KEY]
            self.construct_diffie_hellman_packet()
            self.current_shared_key = utility_functions.generate_shared_secret(
                self.current_secret,
                data['parameter']
            )
            self.received_data = data
        elif data[constants.PACKET_PACKET_TYPE_KEY] == constants.LOGIN_CREDENTIALS_PACKET_TYPE:
            self.state = constants.SERVER_VERIFYING_CREDENTIALS_STATUS
            data = self.shared_key_decrypt_and_deserialize(data[constants.PACKET_ENCRYPTED_KEY])
            if self.verify_nonce(data[constants.PACKET_NONCE_1_KEY]):
                self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
                username = data[constants.PACKET_USERNAME_KEY]
                password = data[constants.PACKET_PASSWORD_KEY]

                if username in self.credentials.keys():
                    if self.verify_login_credentials(username, password):
                        self.state = constants.SERVER_LOGIN_SUCCESS_STATUS
                        self.username = username
                    else:
                        self.state = constants.SERVER_LOGIN_FAILURE_STATUS
                else:
                    self.state = constants.SERVER_LOGIN_FAILURE_STATUS
                self.construct_login_attempt_reply()
            else:
                self.state = constants.SERVER_NONCE_MISMATCH_STATUS
                # TODO
                # self.construct_bad_credentials_packet()
            self.received_data = data
        elif data[constants.PACKET_PACKET_TYPE_KEY] == constants.COMMAND_PACKET_TYPE:
            data = self.shared_key_decrypt_and_deserialize(data[constants.PACKET_ENCRYPTED_KEY])
            if self.verify_nonce(data[constants.PACKET_NONCE_1_KEY]):
                self.current_nonce = data[constants.PACKET_NONCE_2_KEY]
                command = data[constants.PACKET_COMMAND_KEY]
                self.command = command
            else:
                self.state = constants.SERVER_NONCE_MISMATCH_STATUS
                # TODO
                # self.construct_bad_credentials_packet()
            self.received_data = data


class ChatServer(object):

    def __init__(self, port=50000, backlog=5):
        self.credentials = self.parse_passwd()
        self.clients = 0
        # Client map
        self.clientmap = {}
        self.client_secure_map = {}
        # Output socket list
        self.outputs = []
        self.server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server.bind(('', port))
        self.server.listen(backlog)
        # Trap keyboard interrupts
        signal.signal(signal.SIGINT, self.sighandler)

    def parse_passwd(self):
        credentials = {}
        path = 'dir/passwd'
        with open(path) as inf:
            for line in inf:
                words = line.split(':')
                password_tuple = (words[1].rstrip('\n'), words[2].rstrip('\n'))
                #password_tuple = (words[1], words[2])
                credentials[words[0].rstrip('\n')] = password_tuple

        return credentials

    def sighandler(self, signum, frame):
        # Close the server
        print 'Shutting down server...'
        # Close existing client sockets
        for o in self.outputs:
            o.close()
            
        self.server.close()

    def getname(self, client):
        # Return the printable name of the
        # client, given its socket...
        info = self.clientmap[client]
        host, name = info[0][0], info[1]
        return '@'.join((name, host))

    def get_list(self, username_to_exclude):
        list = []
        for client, client_state in self.client_secure_map.iteritems():
            if client_state.get_state() is not None and client_state.get_state() == constants.SERVER_LOGIN_SUCCESS_STATUS:
                if client_state.get_username() != username_to_exclude:
                    list.append(client_state.get_username())

        return constants.SEPARATOR.join(list)

    def get_user_information(self, username, username_to_exclude):
        user_info = {}
        user_info['username'] = username
        for client, client_state in self.client_secure_map.iteritems():
            if client_state.get_state() is not None and client_state.get_state() == constants.SERVER_LOGIN_SUCCESS_STATUS:
                if client_state.get_username() != username_to_exclude and client_state.get_username() == username:
                    user_info['address'] = client_state.get_client_ip_address_tuple()[0]
                    user_info['public_key'] = client_state.get_client_public_key()
                    user_info['listening_port'] = client_state.get_listening_port()
                elif client_state.get_username() == username_to_exclude:
                    user_info['sender_username'] = username_to_exclude
                    user_info['sender_address'] = client_state.get_client_ip_address_tuple()[0]
                    user_info['sender_listening_port'] = client_state.get_listening_port()
        return user_info

    def serve(self):
        
        inputs = [self.server]
        self.outputs = []

        running = 1

        while running:

            try:
                inputready, outputready, exceptready = select.select(inputs, self.outputs, [])
            except select.error, e:
                break
            except socket.error, e:
                break

            for s in inputready:
                if s == self.server:
                    # handle the server socket
                    client, address = self.server.accept()
                    print 'chatserver: got connection %d from %s' % (client.fileno(), address)
                    received = receive(client)

                    client_state = ClientState(
                        client_ip_address_tuple=address,
                        credentials=self.credentials
                    )
                    client_state.fix_listening_port()
                    client_state.update_state(received)
                    current_reply = client_state.get_reply()

                    self.client_secure_map[client] = client_state

                    self.clients += 1
                    send(client, current_reply)
                    inputs.append(client)

                    self.clientmap[client] = (address, 'a')

                    self.outputs.append(client)
                else:
                    # handle all other sockets
                    try:
                        data = receive(s)
                        if data:
                            current_client_state = self.client_secure_map[s]
                            current_client_state.update_state(data)
                            if current_client_state.get_command() is not None:
                                if current_client_state.get_command() == constants.COMMAND_LIST:
                                    list = self.get_list(current_client_state.get_username())
                                    current_client_state.construct_list_packet(list)
                                elif current_client_state.get_command() == constants.COMMAND_LOGOUT:
                                    current_client_state.construct_logout_packet()
                                    self.client_secure_map[s] = ClientState()
                                elif current_client_state.get_command() == constants.COMMAND_SEND:
                                    username_to_send = current_client_state.get_received_data()[constants.PACKET_USERNAME_KEY]
                                    current_username = current_client_state.get_username()
                                    user_info = self.get_user_information(username_to_send, current_username)
                                    current_client_state.construct_send_command_reply(user_info)
                                ## added by janhavi
                                else:
                                    current_client_state.construct_invalid_packet()
                                    current_client_state = ClientState()

                            current_reply = current_client_state.get_reply()
                            send(s, current_reply)
                            current_client_state.set_reply(None)

                        else:
                            print 'chatserver: %d hung up' % s.fileno()
                            self.clients -= 1
                            self.client_secure_map[s] = ClientState()
                            s.close()
                            inputs.remove(s)
                            self.outputs.remove(s)

                            # # Send client leaving information to others
                            # msg = '\n(Hung up: Client from %s)' % self.getname(s)
                            # for o in self.outputs:
                            #     # o.send(msg)
                            #     send(o, msg)
                    except socket.error, e:
                        # Remove
                        inputs.remove(s)
                        self.outputs.remove(s)

        self.server.close()

if __name__ == "__main__":
    ChatServer().serve()