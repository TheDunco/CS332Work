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
        'color': 'yellow'
    }
    
    def __init__(self, x, y, color):
        self.struct.x = x
        self.struct.y = y
        self.struct.color = color
        
    def get_dict(self):
        return self.struct
        
    def update_dict_x(self, new_x):
        self.struct['x'] = new_x
    
    def update_dict_y(self, new_y):
        self.struct['y'] = new_y
        
    def update_dict_color(self, new_color):
        self.struct['color'] = new_color
        
    def set_none_color(self):
        self.struct['color'] = None
        

# TODO: store last_x and last_y values *for each client* in some data structure
# defined here.
client_mouse_data = [ Mousedata(0, 0, "black") ]


def send_data_to_server(penIsDown):
    global client_mouse_data
    # if the pen is up, we set the color to none
    if not penIsDown:
        client_mouse_data[0].set_none_color()
        
    # stringify (serialize) the dictionary to json
    serial_data = JSON.stringify(client_mouse_data[0].get_dict())
    
    if DEBUG:
        print(serial_data)
    if serial_data:
        # send the data to the server
        ws.send(serial_data)
    else:
        if DEBUG:
            print('error sending data')
    
    
def update_mouse_data(cid, x, y):
    global client_mouse_data
    global color_choice
    client_mouse_data[cid].update_dict_x(x)
    client_mouse_data[cid].update_dict_y(y)
    client_mouse_data[cid].update_dict_color(color_choice)


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
        update_mouse_data(0, my_lastx, my_lasty)
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
        update_mouse_data(0, my_lastx, my_lasty)


def on_mesg_recv(evt):
    '''message received from server'''
    # Replace next line if you decide to send data not using JSON formatting.
    data = JSON.parse(evt.data)
    handle_other_client_data(data)


def handle_other_client_data(data):
    # TODO: you, gentle student, need to provide the code here. It is
    # very similar in structure to handle_mousemove() above -- but there
    # are some logical differences.
    print('got other client data')


def set_color(evt):
    global color_choice
    # Get the value of the input box:
    color_choice = document['color_input'].value
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
