import asyncio
import aiohttp
import random
from util import register_bot

# Name to be registered. Must be unique in the drawing board.
bot_name = "MyBot"
# See color palette documentation in api.set_color
bot_color = 6
sayings = [
    "Leoka pystyyn kun tulloo kova paekka, pyssyypähän aenae suu kiinni.",
    "Ne tekköö jotka ossoo, jotka ee ossoo ne arvostelloo.",
    "Joka ihteesä luottaa, se kykysä tuploo.",
    "Anna kaekkes vuan elä periks.",
    "Naara itelles ennen ku muut kerkijää.",
    "Jos et tiijjä, niin kysy.",
    "Jos ymmärrät kaeken, oot varmasti käsittännä viärin.",
    "Misteepä sen tietää, mihin pystyy, ennen kun kokkeeloo."
]


async def main():
    async with aiohttp.ClientSession() as session:
        bot = await register_bot(session, bot_name)
        await bot.set_color(bot_color)
        await bot.say(sayings[random.randint(0, len(sayings) - 1)])

        # Draw some simple rectangles
        # Add your own drawing helper functions into bot.py
        await bot.draw_rectangle(6)
        await bot.move_bot("RIGHT", 4)
        await bot.draw_rectangle(2)
        await bot.move_bot("RIGHT", 6)
        await bot.draw_rectangle(6)
        await bot.move_bot("RIGHT", 4)
        await bot.draw_rectangle(2)
        await bot.move_bot("RIGHT", 8)

        print(f"Current bot position: {bot.x},{bot.y} and current bot color: {bot.color}")

        # Print the current state of the canvas
        # print(await bot.look())

        # Call 'deregister_bot' if you want to remove your bot from the server and, for example, change your bot name.
        # Your bot key is stored in botConfig.cfg after registration, and it is reused when you run this script again.
        # The deregister_bot command will remove the botConfig.cfg file automatically.
        # await bot.deregister_bot()


if __name__ == '__main__':
    asyncio.run(main())
