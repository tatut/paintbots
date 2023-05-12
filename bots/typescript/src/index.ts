import * as api from "./Api";
import { Bot } from "./types/Bot";
import { Color, colors } from "./types/Color";
import { moveBot, registerBot } from "./util";

// Name to be registered. Must be unique in the drawing board.
const botName = "MyBot";
// See color palette documentation in Color.ts
const botColor: Color = colors.RED;

const sayings = [
  "Kylän kohoralla komiasti, vaikka mettällä vähän kompuroottooki.",
  "Kyllä maailma opettaa, jonsei muuta niin hilijaa kävelemähän.",
  "Olokaa klopit hilijaa siälä porstuas, nyt tuloo runua!",
  "Hyviä neuvoja sateloo niinku rakehia.",
  "Minen palijo mitää tee, jos mä jotaki teen, niin mä makaan.",
  "Nii on jano, notta sylyki pöläjää. 🍺",
  "Kyllä aika piisaa, kun vain järki kestää.",
  "Me ei teherä virheitä, vaa ilosii pikku vahinkoi.",
];

/**
 * Example helper functions for drawing a simple rectangle using the api calls
 * Here we are moving the bot first to a certain direction and then painting a
 * pixel with a color that was previously set in the main function.
 * @param bot
 * @param width
 */
const drawRectangle = async (bot: Bot, width: number): Promise<Bot> => {
  const dirs = ["RIGHT", "DOWN", "LEFT", "UP"];

  for (const dir of dirs) {
    for (let i = 1; i < width; i++) {
      bot = await api.moveBot(bot, dir);
      bot = await api.paintPixel(bot);
    }
  }

  return bot;
};

export async function main() {
  let bot = await registerBot(botName);
  bot = await api.setColor(bot, botColor);
  bot = await api.say(bot, sayings[Math.floor(Math.random() * sayings.length)]);

  // Draw some simple rectangles for example (make your own helper functions for more complex shapes!)
  bot = await drawRectangle(bot, 6);
  bot = await moveBot(bot, "RIGHT", 3);
  bot = await drawRectangle(bot, 3);
  bot = await moveBot(bot, "RIGHT", 6);
  bot = await drawRectangle(bot, 6);
  bot = await moveBot(bot, "RIGHT", 3);
  bot = await drawRectangle(bot, 3);
  bot = await moveBot(bot, "RIGHT", 8);

  console.log(
    `Current bot position: ${bot.position?.x},${bot.position?.y} and current bot color: ${bot.color}`
  );

  // Print the current state of canvas in ASCII
  // console.log(await api.look(bot))

  // Get the current state of all registered bots (json)
  // Useful i.e. for bots that want to utilize some swarming behaviour
  // console.log(await api.bots(bot))

  // Call 'deregisterBot' if you want to remove your bot from the server and, for example, change your bot name.
  // Your bot key is stored in botConfig.cfg after registration, and it is reused when you run this script again.
  // The deregister_bot command will remove the botConfig.cfg file automatically.
  // await deregisterBot(bot)
}

if (require.main === module) {
  main();
}
