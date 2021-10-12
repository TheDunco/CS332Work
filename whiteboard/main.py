# Main brython code for running a website client whiteboard
#
# Original author: Victor Norman at Calvin University
# Modified by Duncan Van Keulen for CS332 Advanced Networking at Calvin University
# 
# To run: python3 -m http.server
# 

from browser import document, html, DOMEvent, websocket
from javascript import JSON

WIDTH = 600
HEIGHT = 600

SERVER_PORT = 8001

DEBUG = True


my_lastx = None
my_lasty = None
ws = None
color_choice = 'black'      # default value

# Get the URL host:port, split on ':', and use the host part
# as the machine on which the websockets server is running.
server_ip = document.location.host.split(':')[0]

class Mousedata:
    '''A class to hold and access data about a 
    'mouse'/client drawing on the whiteboard'''
    struct = {
        'x': 0,
        'y': 0,
        'color': 'yellow',
        'id': 0
    }
    
    def __init__(self, iD, x, y, color):
        self.struct['id'] = iD
        self.struct['x'] = x
        self.struct['y'] = y
        self.struct['color'] = color
        
    def get_id(self):
        return self.struct['id']
        
    def get_mouse_dict(self):
        return self.struct
        
    def get_x(self):
        return self.struct['x']
        
    def get_y(self):
        return self.struct['y']
        
    def get_color(self):
        return self.struct['color']
        
    def update_dict_x(self, new_x):
        self.struct['x'] = new_x
    
    def update_dict_y(self, new_y):
        self.struct['y'] = new_y
        
    def update_dict_color(self, new_color):
        self.struct['color'] = new_color
        
    def update_full_dict(self, newdict):
        self.struct = newdict
        
    def set_none_color(self):
        self.struct['color'] = None
        

# Store last_x and last_y values *for each client* in some data structure
#                              id  x  y  color
client_mouse_data = [ Mousedata(0, 0, 0, "orange") ]


def send_data_to_server(penIsDown):
    global client_mouse_data
    # if the pen is up, we set the color to none
    if not penIsDown:
        client_mouse_data[0].set_none_color()
        
    # stringify (serialize) the dictionary to json
    serial_data = JSON.stringify(client_mouse_data[0].get_mouse_dict())
    
    if DEBUG:
        print(serial_data)
    if serial_data:
        # send the data to the server
        ws.send(serial_data)
    else:
        if DEBUG:
            print('error sending data')
    
    
def update_mouse_data(cid, x, y, color):
    global client_mouse_data
    
    client_mouse_data[cid].update_dict_x(x)
    client_mouse_data[cid].update_dict_y(y)
    client_mouse_data[cid].update_dict_color(color)


def handle_mousemove(ev: DOMEvent):
    '''On behalf of all that is good, I apologize for using global
    variables in this code. It is difficult to avoid them when you
    have callbacks like we do here, unless you start creating classes, etc.
    That seemed like overkill for this relatively simple application.'''

    global ctx
    global my_lastx, my_lasty
    global ws
    global client_mouse_data

    # This is the first event or the mouse is being moved without a button
    # being pushed -- don't draw anything, but record where the mouse is.
    if my_lastx is None or ev.buttons == 0:
        my_lastx = ev.x
        my_lasty = ev.y
        ctx.beginPath()
        ctx.moveTo(my_lastx, my_lasty)
        # update our dictionary
        update_mouse_data(0, my_lastx, my_lasty, color_choice)
        # send data to server (pen up)
        send_data_to_server(False)
    else:
        ctx.lineTo(ev.x, ev.y)
        ctx.strokeStyle = color_choice
        ctx.stroke()
        # send data to server (pen down)
        send_data_to_server(True)
        # Store new (x, y) as the last point.
        my_lastx = ev.x
        my_lasty = ev.y
        update_mouse_data(0, my_lastx, my_lasty, color_choice)


def on_mesg_recv(evt):
    '''message received from server'''
    # Replace next line if you decide to send data not using JSON formatting.
    data = JSON.parse(evt.data)
    handle_other_client_data(data)


# This was just a hopeless cause. I don't have enough time to debug this further so I abandoned this. Leaving it here
#   to display my intent and effort. I decided that for the purposes of this application, it doesn't matter if you unregister
#   the other clients from your list or not. Sure if there are bonkers amounts of other clients it will take up a lot of memory
#   but that's just the cost of simplicity.
# def register_or_unregister_client(data):
#     '''If the data includes an unregister field, remove that client from our list
#     as we don't have to keep track of it anymore. Otherwise, if there is a new client, start keeping track of it.
#     Returns None if we unregistered a client and no further action is to be taken, returns the client we'll be
#     drawing with (whether that be the new one or the one with selected id) otherwise'''
    
#     print('checking if data has unregister key')
#     try:
#         # if there is something to unregister, remove that client from our midst
#         if data['unregister'] > 0:
#             print('data has unregister key')
#             for c in client_mouse_data:
#                 print('checking client ' + c)
#                 if c.get_id() == data['unregister']:
#                     print('removing clinet ' + c)
#                     client_mouse_data.remove(c)
#                     return None
#     except Exception as err:
#         print("Data does not have that property!" + err)
        
                
#     print('seeing if there is a new client')
#     # if there is a new client, register it. Loop based off of this example...
#     # https://thispointer.com/python-how-to-check-if-an-item-exists-in-list-search-by-value-or-condition/
#     if not any(mouse_data.get_id() == data['id'] for mouse_data in client_mouse_data):
#         new_client = Mousedata(data['id'], data['x'], data['y'], data['color'])
#         client_mouse_data.append(new_client)
#         return new_client
        
#     print('getting out of here and drawing from the client')
#     # if we didn't do anything, all is normal, so we'll be drawing from the client 
#     for c in client_mouse_data:
#         if c.get_id() == data['id']:
#             return c
#     else:
#         return None

def handle_other_client_data(data):
    # TODO: you, gentle student, need to provide the code here. It is
    # very similar in structure to handle_mousemove() above -- but there
    # are some logical differences.
    if DEBUG:
        print(data)
    
    global ctx
    # init our_client
    our_client = client_mouse_data[0]
    
    if DEBUG:
        print('looking for client')
    found_client = False
    c_index = 0
    # find the client we're dealing with
    for c in client_mouse_data:
        if c.get_id() == data['id']:
            our_client = c
            found_client = True
            break
        c_index += 1
    
    if DEBUG:
        print('adding new client')
    # we didn't have this client before
    if not found_client:
        our_client = Mousedata(data['id'], data['x'], data['y'], data['color'])
        client_mouse_data.append(our_client)
    else:
        # we did find a client, update it's info
        our_client.update_full_dict(data)
        client_mouse_data[c_index] = our_client
        
    if DEBUG:
        print('drawing from client ' + our_client.get_id())
    # draw what the client has/move to where it should be
    if not our_client == None: # make sure we have a valid client to draw with
        if DEBUG:
            print('we have a valid client', type(our_client))
        if our_client.get_color() == None: # pen is up
            if DEBUG:
                print('none color')
            ctx.beginPath()
            ctx.moveTo(our_client.get_x(), our_client.get_y())
        else: # pen is down
            if DEBUG:
                print(our_client.get_color())
            ctx.lineTo(our_client.get_x(), our_client.get_y())
            ctx.strokeStyle = our_client.get_color()
            ctx.stroke()
        

def set_color(evt):
    global color_choice
    # Get the value of the input box:
    color_choice = document['color_input'].value
    # update the local clients data with color choice
    client_mouse_data[0].update_dict_color(color_choice)
    # print('color_choice is now', color_choice)


def set_server_ip(evt):
    global server_ip
    global ws
    server_ip = document['server_input'].value
    ws = websocket.WebSocket(f"ws://{server_ip}:{SERVER_PORT}/")
    ws.bind('message', on_mesg_recv)

# ----------------------- Main -----------------------------

canvas = html.CANVAS(width=WIDTH, height=HEIGHT, id="myCanvas")
document <= canvas
ctx = document.getElementById("myCanvas").getContext("2d")

if DEBUG:
    print("binding mousemove")
canvas.bind('mousemove', handle_mousemove)
if DEBUG:
    print("bound mousemove")
    
document <= html.P()
color_btn = html.BUTTON("Set color: ", Class="button")
color_btn.bind("click", set_color)
document <= color_btn
color_input = html.INPUT(type="text", id="color_input", value=color_choice)
document <= color_input

document <= html.P()
server_btn = html.BUTTON("Server IP address: ", Class="button")
server_btn.bind("click", set_server_ip)
document <= server_btn
server_txt_input = html.INPUT(type="text", id="server_input", value=server_ip)
document <= server_txt_input

if DEBUG:
    print("binding websocket")
ws = websocket.WebSocket(f"ws://{server_ip}:{SERVER_PORT}/")
ws.bind('message', on_mesg_recv)
if DEBUG:
    print("bound websocket")



################################################################################
'''Question answers!'''
# What would you have to change in the protocol if you wanted to allow a user to:
# - erase areas
'''In order to be able to erase areas from the whiteboard, you would most likely
want to include a brush size attribute in the JSON/dicionaries. This would be set
by a UI element such as a slider. You would also have to include a flag (boolean) 
of some sort in order to indicate to the other clients that you are erasing that
area instead of just moving the cursor (assuming that in order to erase an area 
you'd actually want the color to be  None.)'''
# - write text
'''In order to write text to the whiteboard, I feel like you could do it without
adding anything if you had the time. All it would consist of is figuring out how
to draw said text with individual x,y coordinates that could be macro recorded get
and idea of how to make each letter. This is basically making your own font, which
is not ideal. Therefore, if you wanted to do it without doing this, assuming there 
is a library that allows you to actually write text to the whiteboard efficiently, 
all you would need to do is add a "text" attribute/key that replaces the "x" and "y"
attributes for each message. The text could be entered in a text box on the sending
client and once the use hits enter it could be sent to all other clients. You could 
combine this with the erasing scheme in order to be able to backspace and/or send the 
messages live as the sending client types and not have to worry about writing over 
letters the sending client has backspaced/deleted'''
# - take (and release) control of the whiteboard
'''In order to pull this off, you would have to add an id field for the client in 
control who is in control that would be authoritatively set by the server. This 
meaning that the clients would have to negotiate with the server in order to determine
who has control of the whiteboard. If one client releases control of the whiteboard 
(i.e. sets the id field to -1 or something), the server could see that and accept the next 
or queued request for control from the other clients (most likely the server would ignore
other requests  or queue only one request for control instead of queuing them all so the 
clients would simply have to ask the server again or the latest one to ask for control would
get it). The clients could request control by clicking on a button that sends a message to 
the server with their id as the "in control" id. The ACK from the server would come in the 
form of the next message that gets sent to all the clients: if it has the requesting client's 
id as the "in control" field, the request was accepted, otherwise it was not'''
################################################################################
