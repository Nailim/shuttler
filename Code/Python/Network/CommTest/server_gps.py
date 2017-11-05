#!/usr/bin/env python

import socket

host = ''
port = 3001
backlog = 5
size = 1024

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((host,port))
s.listen(backlog)

client, address = s.accept()
print "We have one"

while 1:
    
    data = client.recv(size)
    print 'data: ' + data

client.close() 
