import {clearPixel, lookAround, moveBot, paintPixel, registerBot, say, setColor} from './Api'

const botName = "MyBot";
const botColor = "#FF004D";

export async function main() {
    const botId = await registerBot(botName);
    await moveBot(botId, "RIGHT");
    const pixel = await paintPixel(botId);

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