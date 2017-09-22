import cPickle
import socket
import struct

marshall = cPickle.dumps
unmarshall = cPickle.loads


def send(channel, *args):
    """
    send data to the channel

    :param channel: the socket
    :param args: the data; not serialized
    :return: void
    """
    buf = marshall(args)
    value = socket.htonl(len(buf))
    size = struct.pack("L", value)
    channel.send(size)
    channel.send(buf)


def receive(channel):
    """
    receive data from the channel
    :param channel: the socket to receive from
    :return: the data, deserialized
    """
    size = struct.calcsize("L")
    size = channel.recv(size)
    try:
        size = socket.ntohl(struct.unpack("L", size)[0])
    except struct.error, e:
        return ''

    buf = ""

    while len(buf) < size:
        buf = channel.recv(size - len(buf))

    return unmarshall(buf)[0]