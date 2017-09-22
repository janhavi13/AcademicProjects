import hashlib
import pickle
import random
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import constants
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding as aes_padding

G = 23489
P = 128903289023
Y = 1023
KEY_SIZE = 32
salt = b''

def random_number_generator(bits):
    """
    generate a random number of the number of bits specified

    :param bits: the number of bits

    :return: the generated number
    """
    random_number = random.getrandbits(bits)
    return random_number


def sha256_generator(data):
    """
    generate a SHA256 hash of the input data
    :param data: the data to hash
    :return: the hashed data
    """
    hash_value = hashlib.sha256(data).digest()
    return hash_value


def symmetric_key_decrypt(key, iv, ciphertext):
    """
    constructs an AES key from the key and the iv and decrypts the ciphertext
    :param key: the key
    :param iv: the iv
    :param ciphertext: the ciphertext to encrypt
    :return: the decrypted text
    """
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    unpadder = aes_padding.PKCS7(128).unpadder()
    decrypted_data = decryptor.update(ciphertext) + decryptor.finalize()
    return unpadder.update(decrypted_data) + unpadder.finalize()


def symmetric_key_encrypt(key, iv, message):
    """
    constructs an AES key from the key and the iv and encrypts the message
    :param key: the key
    :param iv: the iv
    :param message: the message
    :return: the encrypted text
    """
    padder = aes_padding.PKCS7(128).padder()
    message = padder.update(message)
    message += padder.finalize()
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    cipher_text = encryptor.update(message) + encryptor.finalize()
    return cipher_text


def aes_encrypt(key, message):
    """
    use the key to carry out an AES encryption on the message
    :param key: the key
    :param message: the message
    :return: the encrypted message
    """
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=48,
        salt=salt,
        iterations=100000,
        backend=default_backend()
    )

    symmetric_components = kdf.derive(key.encode())
    aes_key = symmetric_components[0:32]
    iv = symmetric_components[32:]

    # padding
    padder = aes_padding.PKCS7(constants.BLOCK_LENGTH).padder()
    message = padder.update(message)
    message += padder.finalize()
    cipher = Cipher(algorithms.AES(aes_key), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    cipher_text = encryptor.update(message) + encryptor.finalize()
    return cipher_text


def aes_decrypt(key, message):
    """
    use the key to decrypt the message
    :param key: the key
    :param message: the encrypted message
    :return: the decrypted message
    """
    kdf = PBKDF2HMAC(
        algorithm=hashes.SHA256(),
        length=48,
        salt=salt,
        iterations=100000,
        backend=default_backend()
    )

    server_symmetric_components = kdf.derive(key.encode())
    aes_key = server_symmetric_components[0:32]
    iv = server_symmetric_components[32:]

    cipher = Cipher(algorithms.AES(aes_key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    # remove padding from the decrypted message
    unpadder = aes_padding.PKCS7(constants.BLOCK_LENGTH).unpadder()
    decrypted_data = decryptor.update(message) + decryptor.finalize()
    return unpadder.update(decrypted_data) + unpadder.finalize()


def generate_secret():
    """
    generate a 16 bit random number
    :return: the generated number
    """
    secret = random_number_generator(16)
    return secret


def public_key_encrypt(public_key, data):
    """
    use the public key to encrypt data
    :param public_key: the public key
    :param data: the data
    :return: the encrypted data
    """
    return public_key.encrypt(
        data,
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None
        )
    )


def public_key_decrypt(private_key, data):
    """
    use a private key to decrypt data encrypted previously by a public key
    :param private_key: the private key
    :param data: the encrypted data
    :return: the decrypted data
    """
    return private_key.decrypt(
        data,
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None
        )
    )


def calculate_diffie_hellman_contribution(secret):
    """
    calculate a diffie hellman contribution using the secret
    :param secret: the secret
    :return: the contribution
    """
    contribution = (G ** secret) % P
    return contribution


def generate_shared_secret(secret, received_parameter):
    """
    calculate a diffie hellman shared key using the secret and another party's parameter
    :param secret: the secret
    :param received_parameter: the received parameter
    :return: the shared secret
    """
    session_key = (received_parameter ** secret) % P
    return str(session_key)


def generate_private_public_key():
    """
    generate a private key and an associated public key
    :return: a tuple of both keys
    """
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
        backend=default_backend()
    )
    public_key = private_key.public_key()
    keys = (private_key, public_key)
    return keys


def read_server_public_key(server_public_key_file_path):
    """
    read a public key from a file
    :param server_public_key_file_path: the path of the file
    :return: the key
    """
    with open(server_public_key_file_path, "rb") as key_file:
        public_key = serialization.load_der_public_key(
            key_file.read(),
            backend=default_backend()
        )

    return public_key


def read_server_private_key(server_private_key_file_path):
    """
    read a private key from a file
    :param server_private_key_file_path: the path to the file
    :return:
    """
    with open(server_private_key_file_path, "rb") as key_file:
        private_key = serialization.load_der_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )

    return private_key


def pickle_helper(input, operation):
    """
    use pickle to serialize or deserialize the input
    :param input: the input
    :param operation: whether to serialize or deserialize
    :return: the output
    """
    if operation == constants.PICKLE_SERIALIZE:
        output = pickle.dumps(input, 2)
    if operation == constants.PICKLE_DESERIALIZE:
        output = pickle.loads(input)
    return output