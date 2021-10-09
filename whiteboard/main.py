
from browser import document, html, DOMEvent, websocket
from javascript import JSON

WIDTH = 600
HEIGHT = 600

SERVER_PORT = 8001


my_lastx = None
my_lasty = None
ws = None
color_choice = 'black'      # default value

# Get the URL host:port, split on ':', and use the host part
# as the machine on which the websockets server is running.
server_ip = document.location.host.split(':')[0]


# TODO: store last_x and last_y values *for each client* in some data structure
# defined here.


def handle_mousemove(ev: DOMEvent):
    '''On behalf of all that is good, I apologize for using global
    variables in this code. It is difficult to avoid them when you
    have callbacks like we do here, unless you start creating classes, etc.
    That seemed like overkill for this relatively simple application.'''

    global ctx
    global my_lastx, my_lasty
    global ws

    # This is the first event or the mouse is being moved without a button
    # being pushed -- don't draw anything, but record where the mouse is.
    if my_lastx is None or ev.buttons == 0:
        my_lastx = ev.x
        my_lasty = ev.y
        ctx.beginPath()
        ctx.moveTo(my_lastx, my_lasty)
        # TODO: send data to server.
    else:
        ctx.lineTo(ev.x, ev.y)
        ctx.strokeStyle = color_choice
        ctx.stroke()
        # TODO: send data to server.
        # Store new (x, y) as the last point.
        my_lastx = ev.x
        my_lasty = ev.y


def handle_other_client_data(data):
    # TODO: you, gentle student, need to provide the code here. It is
    # very similar in structure to handle_mousemove() above -- but there
    # are some logical differences.
    pass


def on_mesg_recv(evt):
    '''message received from server'''
    # Replace next line if you decide to send data not using JSON formatting.
    data = JSON.parse(evt.data)
    handle_other_client_data(data)


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

canvas.bind('mousemove', handle_mousemove)

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

ws = websocket.WebSocket(f"ws://{server_ip}:{SERVER_PORT}/")
ws.bind('message', on_mesg_recv)
