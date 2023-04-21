import urllib.parse
import os

API_URL = os.getenv('PAINTBOTS_URL', 'http://localhost:31173/')
headers = {'content-type': 'application/x-www-form-urlencoded'}


async def register_bot(session, name):
    try:
        async with session.post(API_URL,
                                data={'register': name},
                                headers=headers) as resp:
            response = await resp.text()
            print(f"Got bot id from server: {response}")

            return response
    except Exception as e:
        raise Exception(f"Failed to register bot: {e}")


async def look(session, bot_id):
    """
    Note: Returns an ascii representation of the current canvas.
    """
    try:
        async with session.post(API_URL,
                                data={'id': bot_id, 'look': ''},
                                headers=headers) as resp:
            return await resp.text()
    except Exception as e:
        raise Exception(f"Failed to look: {e}")


def parse_position_response(response):
    params = dict(urllib.parse.parse_qsl(response))
    color = params.get('color')
    x = params.get('x')
    y = params.get('y')

    color = int(color) if color else None
    x = int(params.get('x')) if x else None
    y = int(params.get('y')) if y else None

    return {'color': color, 'x': x, 'y': y}


async def api_command(session, command, error_msg):
    try:
        async with session.post(API_URL, data=command, headers=headers) as resp:
            response = await resp.text()
            # print(command, response)

            return parse_position_response(response)
    except Exception as e:
        raise Exception(f"{error_msg}: {e}")


async def move_bot(session, bot_id, direction):
    return await api_command(session, {'id': bot_id, 'move': direction}, "Failed to move bot")


# pico-8 16 color palette from https://www.pixilart.com/palettes/pico-8-51001
# 0 "#000000"
# 1 "#1D2B53"
# 2 "#7E2553"
# 3 "#008751"
# 4 "#AB5236"
# 5 "#5F574F"
# 6 "#C2C3C7"
# 7 "#FFF1E8"
# 8 "#FF004D"
# 9 "#FFA300"
# 10 "#FFEC27"
# 11 "#00E436"
# 12 "#29ADFF"
# 13 "#83769C"
# 14 "#FF77A8"
# 15 "#FFCCAA"

async def set_color(session, bot_id, color):
    return await api_command(session, {'id': bot_id, 'color': color}, "Failed to set color")


async def paint_pixel(session, bot_id):
    return await api_command(session, {'id': bot_id, 'paint': ''}, "Failed to paint")


async def clear_pixel(session, bot_id):
    return await api_command(session, {'id': bot_id, 'clear': ''}, "Failed to clear a pixel")


async def say(session, bot_id, msg):
    return await api_command(session, {'id': bot_id, 'msg': msg}, "Failed to say a message")


async def bye(session, bot_id):
    return await api_command(session, {'id': bot_id, 'bye': ''}, "Failed to deregister the bot")
