#
# Websocket-based server code. This code sends to other clients whatever
# each client sends to it. Thus, this is essentially a broadcasting service.
#
# Date: 24 June 2021
# Original author: Victor Norman at Calvin University
#
# Due Date: 13 October 2021
# Modified by Duncan Van Keulen for CS332 Advanced Networking at Calvin University
#

import asyncio
import websockets
import json
import socket

# Change to False when you are done debugging.
DEBUG = True
# Port the server listens on to receive connections.
PORT = 8001

next_id = 0

# TODO: need a variable here to hold all connected clients' websocket objects.
my_clients = []

class Client:
    id = 0
    ws = None
    
    def __init__(self, sock, iD):
        self.ws = sock
        self.id = iD
    
    def get_id(self):
        return self.id
        
    def get_socket(self):
        return self.ws


def register_new_client(client):
    '''Add a client ot my list of clients'''
    global next_id
    
    my_clients.append(client)
    
    '''client id keeps incrementing as there is no max in Python3+,
    so every new client will have a truly unique id. This solves 
    the problem of data not being sent to other clients because 
    client 0 disconnected.'''
    next_id += 1
    
    if DEBUG:
        print('+ registered new client! ' + str(client.ws.host))


async def unregister_client(client):
    '''Remove a client from my list of clients'''
    
    tmpid = client.get_id()
    
    my_clients.remove(client)
    
    # send message to other clients that this client was disconnected
    for c in my_clients:
        await c.get_socket().send(json.dumps({"unregister": tmpid}))
    
    if DEBUG:
        print('- removed old client!')

async def per_client_handler(client_ws, path):
    '''This function is called whenever a client connects to the server. It
    does not exit until the client ends the connection. Thus, an instance of
    this function runs for each connected client.'''
    global next_id
    me = Client(client_ws, next_id)
    register_new_client(me)
    try:
        async for message in me.ws:
            # This next line assumes that the message we received is a stringify-ed
            # JSON object.  data will be a dictionary.
            rcvd_data = json.loads(message)
            if DEBUG:
                print('rcvd data: ', rcvd_data, type(rcvd_data))
            
            # Add the client's unique id to the message before
            # sending to everyone.
            rcvd_data["id"] = me.get_id()

            # Send received message to all *other* clients.
            for client in my_clients:
                if not client.get_id() == me.get_id():
                    
                    await client.get_socket().send(json.dumps(rcvd_data))
                    
                    if DEBUG: 
                        print('sent ' + json.dumps(rcvd_data) + ' to client ' + str(client.id) + '\n')
    except websocket.exceptions.ConnectionClosedOK:
        pass
    finally:
        await unregister_client(me)


# Adapted from https://stackoverflow.com/questions/166506/finding-local-ip-addresses-using-pythons-stdlib
def getNetworkIp():
    '''This is just a way to get the IP address of interface this program is
    communicating over.'''
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    s.connect(('8.8.8.8', 80))
    return s.getsockname()[0]


# Run websocket server on port PORT on the local loopback interface while you are
# still debugging your own code.
start_server = websockets.serve(per_client_handler, "localhost", PORT)
# TODO: use this next line when you are ready to deploy and test your code with others.
# (And comment out the line above.)
# start_server = websockets.serve(per_client_handler, getNetworkIp(), PORT)

asyncio.get_event_loop().run_until_complete(start_server)
asyncio.get_event_loop().run_forever()
