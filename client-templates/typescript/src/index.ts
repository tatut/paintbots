import * as api from './Api'
import {registerBot} from './util'

const botName = "MyBot";
const botColor = "#FF004D";

export async function main() {
    const bot = await registerBot(botName);
    console.log(bot);
    await api.moveBot(bot.id, "RIGHT");
    const pixel = await api.paintPixel(bot.id);

    /*const bot = {
        id: botId,
        name: botName,
        color: botColor,
        position: {
            x: pixel.position.x,
            y: pixel.position.y
        }
    }
    console.log(bot);
    */
}

if (require.main === module) {
    main();
}