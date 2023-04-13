import * as api from './Api'
import {registerBot} from './util'
import {Bot} from "./types/Bot";

const botName = "MyBot";
const botColor = "#FF004D";



const drawRectangle = async (bot: Bot, width: number): Promise<Bot> => {
    const dirs = ['RIGHT', 'DOWN', 'LEFT', 'UP'];

    for (const dir of dirs) {
        for (let i = 0; i < width; i++) {
            await api.moveBot(bot.id, dir);
            await api.paintPixel(bot.id);
        }
    }

    return bot;
};

export async function main() {
    let bot = await registerBot(botName);
    bot = await drawRectangle(bot, 5);
    console.log(bot);
}

if (require.main === module) {
    main();
}