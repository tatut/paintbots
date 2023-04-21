import aiofiles.os
import api
from bot import Bot

config_file_path = "botConfig.cfg"


async def file_exists(path: str) -> bool:
    return await aiofiles.os.path.exists(path)


async def store_bot_config(bot_name: str, bot_id: str) -> dict:
    async with aiofiles.open(config_file_path, 'w') as f:
        await f.write(f"{bot_name}:{bot_id}")

    return {'name': bot_name, 'bot_id': bot_id}


async def remove_bot_config():
    exists = await file_exists(config_file_path)

    if exists:
        await aiofiles.os.remove(config_file_path)

    return None


async def load_bot_config() -> dict or None:
    exists = await file_exists(config_file_path)

    if exists:
        try:
            async with aiofiles.open(config_file_path, 'r') as f:
                result = await f.read()
                splat = result.split(":")
                return {'name': splat[0], 'bot_id': splat[1]}
        except Exception as e:
            print(e)

    return None


async def register_bot(session, bot_name: str) -> Bot:
    config = await load_bot_config()

    if config and config['name'] == bot_name:
        bot_id = config['bot_id']
    else:
        bot_id = await api.register_bot(session, bot_name)
        await store_bot_config(bot_name, bot_id)

    print(f"Registered bot: {bot_name} with id: {bot_id}")

    return Bot(session, name=bot_name, bot_id=bot_id)


async def deregister_bot(session, bot_id):
    await api.bye(session, bot_id)

    # Remove bot_config.cfg if present
    await remove_bot_config()
