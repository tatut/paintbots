import axios, { AxiosRequestConfig, RawAxiosRequestHeaders } from "axios";
import { Bot } from "./types/Bot";
import { Pixel } from "./types/Pixel";
import { BotCommand } from "./types/BotCommand";

const API_URL = "http://localhost:31173/";

const config: AxiosRequestConfig = {
  headers: {
    "content-type": "application/x-www-form-urlencoded",
  } as RawAxiosRequestHeaders,
};

export const registerBot = async (name: string): Promise<string> => {
  try {
    const response = await axios.post(API_URL, { register: name }, config);
    console.log(`Got bot id from server: ${response.data}`);

    return response.data;
  } catch (e) {
    if (axios.isAxiosError(e)) {
      const errResp = e.response;
      throw Error(`Failed to register bot: ${errResp?.data}`);
    } else {
      throw e;
    }
  }
};

/**
 * Returns an ascii representation of the current canvas.
 */
export const look = async (bot: Bot): Promise<Bot> => {
  try {
    const response = await axios.post(
      API_URL,
      { id: bot.id, look: "" },
      config
    );
    return response.data;
  } catch (e) {
    if (axios.isAxiosError(e)) {
      const errResp = e.response;
      throw Error(`Failed to look: ${errResp?.data}`);
    } else {
      throw e;
    }
  }
};

const parsePixelResponse = (response: string): Pixel => {
  const params = new URLSearchParams(response);
  let color, x, y;

  if (params.get("color")) {
    color = parseInt(<string>params.get("color"));
  }

  if (params.get("x")) {
    x = parseInt(<string>params.get("x"));
  }

  if (params.get("color")) {
    y = parseInt(<string>params.get("color"));
  }

  if (!color || !x || !y) {
    throw Error("Unable to parse pixel response!");
  }

  return { color, position: { x, y } };
};

const apiCommand = async (bot: Bot, command: BotCommand, errorMsg: string) => {
  try {
    const response = await axios.post(API_URL, command, config);

    return { ...bot, ...parsePixelResponse(response.data) };
  } catch (e) {
    if (axios.isAxiosError(e)) {
      const errResp = e.response;
      throw Error(`${errorMsg}: ${errResp?.data}`);
    } else {
      throw e;
    }
  }
};

export const moveBot = async (bot: Bot, dir: string): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, move: dir }, "Failed to move bot");
};

/**
 *
 * ;; pico-8 16 color palette from https://www.pixilart.com/palettes/pico-8-51001
 * 0 "#000000"
 * 1 "#1D2B53"
 * 2 "#7E2553"
 * 3 "#008751"
 * 4 "#AB5236"
 * 5 "#5F574F"
 * 6 "#C2C3C7"
 * 7 "#FFF1E8"
 * 8 "#FF004D"
 * 9 "#FFA300"
 * 10 "#FFEC27"
 * 11 "#00E436"
 * 12 "#29ADFF"
 * 13 "#83769C"
 * 14 "#FF77A8"
 * 15 "#FFCCAA"
 *
 * @param bot
 * @param color
 *
 */
export const setColor = async (bot: Bot, color: number): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, color }, "Failed to set color");
};

export const paintPixel = async (bot: Bot): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, paint: "" }, "Failed to paint");
};

export const clearPixel = async (bot: Bot): Promise<Bot> => {
  return await apiCommand(
    bot,
    { id: bot.id, clear: "" },
    "Failed to clear a pixel"
  );
};

export const say = async (bot: Bot, msg: string): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, msg }, "Failed to say a message");
};
