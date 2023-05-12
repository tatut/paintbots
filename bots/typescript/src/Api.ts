import axios, { AxiosRequestConfig, RawAxiosRequestHeaders } from "axios";
import { Bot } from "./types/Bot";
import { BotCommand } from "./types/BotCommand";
import { Color } from "./types/Color";
import { Pixel } from "./types/Pixel";

const API_URL = process.env.PAINTBOTS_URL || "http://localhost:31173/";

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
      const msg = errResp?.data || e.message

      throw Error(`Failed to register bot: ${msg}`);
    } else {
      throw e;
    }
  }
};


export const degisterBot = async (bot: Bot) => {
  try {
    const response = await axios.post(API_URL, {id: bot.id, bye: ''}, config);

    return response.data;
  } catch (e) {
    if (axios.isAxiosError(e)) {
      const errResp = e.response;
      const msg = errResp?.data || e.message

      throw Error(`Failed to deregister bot: ${msg}`);
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
      const msg = errResp?.data || e.message

      throw Error(`Failed to look: ${msg}`);
    } else {
      throw e;
    }
  }
};


/**
 * Returns (JSON) information about all registered bots
 */
export const bots = async (bot: Bot): Promise<Bot> => {
  try {
    const response = await axios.post(
      API_URL,
      { id: bot.id, bots: "" },
      config
    );
    return response.data;
  } catch (e) {
    if (axios.isAxiosError(e)) {
      const errResp = e.response;
      const msg = errResp?.data || e.message

      throw Error(`Failed to fetch bots state: ${msg}`);
    } else {
      throw e;
    }
  }
};

const parsePixelResponse = (response: string): Pixel => {
  const params = new URLSearchParams(response);
  
  let color: Color | undefined = undefined;
  let x: number | undefined = undefined;
  let y: number | undefined = undefined;

  if (params.get("color")) {
    color = params.get("color") as Color;
  }

  if (params.get("x")) {
    x = parseInt(<string>params.get("x"));
  }

  if (params.get("y")) {
    y = parseInt(<string>params.get("y"));
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
      const msg = errResp?.data || e.message

      throw Error(`${errorMsg}: ${msg}`);
    } else {
      throw e;
    }
  }
};

export const moveBot = async (bot: Bot, dir: string): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, move: dir }, "Failed to move bot");
};

export const setColor = async (bot: Bot, color: Color): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, color }, "Failed to set color");
};

export const paintPixel = async (bot: Bot): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, paint: "" }, "Failed to paint");
};

export const clearPixel = async (bot: Bot): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, clear: "" }, "Failed to clear a pixel"
  );
};

export const say = async (bot: Bot, msg: string): Promise<Bot> => {
  return await apiCommand(bot, { id: bot.id, msg }, "Failed to say a message");
};
