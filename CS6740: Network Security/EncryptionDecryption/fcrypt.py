import os
import sys
import argparse
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives import padding as aes_padding


BLOCK_LENGTH = 128
KEY_SIZE = 32
IV_SIZE = 16
SIGNATURE_LEN = 256
AES_ENCRYPTION_ESSENTIAL_LEN = 256
SEPARATOR = "JANHAVI"



# function to encrypt a message
def encrypt(message, destination_public_key, sender_private_key):
    # encrypt message using AES algorithm and CBC mode.

    # format string generated across encryption block to send the packet across

    backend = default_backend()

    # AES allows only blocks of 128 bits to pass hence padding required
    padder = aes_padding.PKCS7(BLOCK_LENGTH).padder()
    message = padder.update(message)
    message += padder.finalize()

    message_length = str(len(message))


    # AES encryption with CBC mode begins :
    # requires a key - 256 bits , iv - 128 bits
    key = os.urandom(KEY_SIZE)
    iv = os.urandom(IV_SIZE)
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=backend)
    encryptor = cipher.encryptor()
    cipher_text = encryptor.update(message) + encryptor.finalize()
    # encryption ends

    # pass the key , iv for decryption securely.
    # encrypt key , iv using destination public key.

    # open the public key of the destination
    with open(destination_public_key, "rb") as key_file:
        public_key = serialization.load_der_public_key(
        key_file.read(),
        backend=default_backend()
        )
    # begin encryption of key and iv using the public key
    # OAEP is standard suggested padding for RSA encryption
    encrypted_sym_components = public_key.encrypt(
        key + SEPARATOR + iv,
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None
        )
    )
    cipher_text = cipher_text + SEPARATOR + encrypted_sym_components



    # Begin digital signature:
    # open sender private key
    with open(sender_private_key, "rb") as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )


    # pad and hashing required for RSA key
    signer = private_key.signer(
        padding.PSS(
            mgf=padding.MGF1(hashes.SHA256()),
            salt_length=padding.PSS.MAX_LENGTH
        ),
        hashes.SHA256()
        )

    signer.update(cipher_text)
    signature = signer.finalize()
    # Signing ends

    #print ("signature",len(signature),signature)

    message_block = cipher_text + SEPARATOR + signature
    return message_block


# fu nction to decrypt a message
def decrypt(cipher_text,destination_private_key,sender_public_key):



    components = cipher_text.split(SEPARATOR)


    # get signature
    signature = components[2]
    #signature = signature.split()[0]
    #print("decrpyt sign len", len(signature), signature)

    # open sender's public key
    with open(sender_public_key, "rb") as key_file:
        public_key = serialization.load_der_public_key(
            key_file.read(),
            backend=default_backend()
        )
    # Begin Verification
    # verify signature of the sender using it's public key
    verifier = public_key.verifier(
        signature,
        padding.PSS(
            mgf=padding.MGF1(hashes.SHA256()),
            salt_length=padding.PSS.MAX_LENGTH
        ),
        hashes.SHA256()
    )

    # verify the cipher text as well whether that is also a sent by the expected sender
    verifier.update(components[0] + SEPARATOR + components[1])
    verifier.verify()
    # Verification ends

    # Use destination private key to decrypt AES components
    with open(destination_private_key, "rb") as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=None,
            backend=default_backend()
        )

    # Decrypt using private key
    decrypted_symmetric_key_components = private_key.decrypt(
        components[1],
        padding.OAEP(
            mgf=padding.MGF1(algorithm=hashes.SHA256()),
            algorithm=hashes.SHA256(),
            label=None
        )
    )

    # Take key and iv from the decrypted text and use it for symmetric decryption
    key = decrypted_symmetric_key_components.split(SEPARATOR)[0]
    iv = decrypted_symmetric_key_components.split(SEPARATOR)[1]


    # AES decryption
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()

    # remove padding from the decrypted message
    unpadder = aes_padding.PKCS7(BLOCK_LENGTH).unpadder()
    decrypted_data = decryptor.update(components[0]) + decryptor.finalize()

    return unpadder.update(decrypted_data) + unpadder.finalize()


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('-e', '-d', nargs=4, help='Provide -e or -d parameter', required=True)
    args = vars(ap.parse_args())

    # args expected in this format
    # python fcrypt.py -e receiver_public_key.pub sender_private_key input_text_file ciphertext_file
    if sys.argv[1] == '-e':
        try:
            destination_public_key = sys.argv[2]
            sender_private_key = sys.argv[3]
            #print ("Sender length in main", len(sender_private_key))
            plain_text = sys.argv[4]
            plain_text = open(sys.argv[4], 'r').read()
            cipher_text_file = sys.argv[5]
            cipher_text = encrypt(plain_text, destination_public_key, sender_private_key)
            open(cipher_text_file, 'w').write(cipher_text)
        except IOError, e:
            print("Could not find file ", e.filename)

    # args expected in this format
    # python fcrypt.py -d receiver_private_key sender_public_key.pub ciphertext_file output_plaintext_file
    elif sys.argv[1] == '-d':
        try:
            destination_private_key = sys.argv[2]
            sender_public_key = sys.argv[3]

            cipher_text_file = sys.argv[4]
            plain_text = sys.argv[5]
            cipher_text = open(cipher_text_file, 'r').read()
            output = decrypt(cipher_text, destination_private_key, sender_public_key)
            open(plain_text, 'w').write(output)
        except IOError, e:
            print("Could not find file ", e.filename)

    else:
        print "Invalid arguments"

    sys.exit(1)
