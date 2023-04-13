import * as api from './Api'
import {moveBot, registerBot} from './util'
import {Bot} from "./types/Bot";

// Name to be registered. Must be unique in the drawing board.
const botName = "MyBot";
// See color palette documentation in api.setColor
const botColor = 6;

/**
 * Example helper functions for drawing a simple rectangle using the api calls
 * Here we are moving the bot first to a certain direction and then painting a
 * pixel with a color that was previously set in the main function.
 * @param bot
 * @param width
 */
const drawRectangle = async (bot: Bot, width: number): Promise<Bot> => {
    const dirs = ['RIGHT', 'DOWN', 'LEFT', 'UP'];

    for (const dir of dirs) {
        for (let i = 0; i < width; i++) {
            await api.moveBot(bot, dir);
            await api.paintPixel(bot);
        }
    }

    return bot;
};

export async function main() {
    let bot = await registerBot(botName);
    bot = await api.setColor(bot, botColor)

    // Draw some simple rectangles for example (make your own helper functions for more complex shapes!)
    bot = await drawRectangle(bot, 6);
    bot = await moveBot(bot, 'RIGHT', 4);
    bot = await drawRectangle(bot, 2);
    bot = await moveBot(bot, 'RIGHT', 6);
    bot = await drawRectangle(bot, 6);
    bot = await moveBot(bot, 'RIGHT', 4);
    bot = await drawRectangle(bot, 2);
    bot = await moveBot(bot, 'RIGHT', 8);

    console.log(bot);
}

if (require.main === module) {
    main();
}