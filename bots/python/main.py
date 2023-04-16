import asyncio
import aiohttp
from util import register_bot

# Name to be registered. Must be unique in the drawing board.
bot_name = "MyBot"
# See color palette documentation in api.setColor
bot_color = 6


async def main():
    async with aiohttp.ClientSession() as session:
        bot = await register_bot(session, bot_name)

    print(bot)


if __name__ == '__main__':
    asyncio.run(main())
