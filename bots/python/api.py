import aiohttp

API_URL = "http://localhost:31173/"
headers = {"content-type": "application/x-www-form-urlencoded"}


async def register_bot(session, name):
    try:
        async with session.post(API_URL,
                                data={"register": name},
                                headers=headers) as resp:
            response = await resp.text()
            print(f"Got bot id from server: {response}")

            return response
    except Exception as e:
        raise Exception(f"Failed to register bot: {e}")
