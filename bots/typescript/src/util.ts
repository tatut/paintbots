import { promises as fs } from "fs";
import { Bot } from "./types/Bot";
import * as api from "./Api";

const configFilePath = "botConfig.cfg";
const fileExists = async (path: string) =>
  !!(await fs.stat(path).catch(() => false));

export const storeBotConfig = async (
  botName: string,
  id: string
): Promise<Bot> => {
  await fs.writeFile("botConfig.cfg", `${botName}:${id}`);

  return {
    name: botName,
    id,
  };
};

export const loadBotConfig = async (): Promise<Bot | undefined> => {
  const exists = await fileExists(configFilePath);

  if (exists) {
    try {
      const result = await fs.readFile(configFilePath, "utf8");
      const splat = result.split(":");
      return {
        name: splat[0],
        id: splat[1],
      };
    } catch (e) {
      console.error(e);
    }
  }

  return;
};

export const registerBot = async (botName: string): Promise<Bot> => {
  const config = await loadBotConfig();
  let id;

  // If there is an existing config matching the chosen bot name, return the registered id,
  // otherwise register a new bot.
  if (config && config.name === botName) {
    id = config.id;
  } else {
    id = await api.registerBot(botName);
    await storeBotConfig(botName, id);
  }

  console.log(`Registered bot: ${botName} with id: ${id}`);

  return {
    name: botName,
    id,
  };
};

export const moveBot = async (
  bot: Bot,
  dir: string,
  dist: number
): Promise<Bot> => {
  let result = bot;

  for (let i = 0; i < dist; i++) {
    result = await api.moveBot(bot, dir);
  }

  return result;
};
