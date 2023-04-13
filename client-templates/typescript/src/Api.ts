import axios from 'axios';
import {Bot} from "./types/Bot";
import {Pixel} from "./types/Pixel";

const API_URL = 'http://localhost:31173/';

export const registerBot = async (name: string): Promise<string> => {
    try {
        const response = await axios.post(`${API_URL}`, `register=${name}`);
        return response.data;
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to register bot: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

const parsePixelResponse = (response: string): Pixel => {
    const params = new URLSearchParams(response);
    let color, x, y;

    console.log(response, params)

    if (params.get('color')) {
        color = parseInt(<string>params.get('color'));
    }

    if (params.get('x')) {
        x = parseInt(<string>params.get('x'));
    }

    if (params.get('color')) {
        y = parseInt(<string>params.get('color'));
    }

    if (!color || !x || !y) {
        throw Error('Unable to parse pixel response!');
    }

    return {color, position: {x, y}}
};

export const moveBot = async (bot: Bot, dir: string): Promise<Bot> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${bot.id}&move=${dir}`);

        return {...bot, ...parsePixelResponse(response.data)};
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to move bot: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
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
    try {
        const response = await axios.post(`${API_URL}`, `id=${bot.id}&color=${color}`);

        return {...bot, ...parsePixelResponse(response.data)};
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to set color: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const paintPixel = async (bot: Bot): Promise<Bot> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${bot.id}&paint`);

        return {...bot, ...parsePixelResponse(response.data)};
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to paint: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const clearPixel = async (bot: Bot): Promise<Bot> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${bot.id}`);

        return {...bot, ...parsePixelResponse(response.data)};
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to clear a pixel: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const say = async (bot: Bot, message: string): Promise<Bot> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${bot.id}&msg=${message}`);

        return {...bot, ...parsePixelResponse(response.data)};
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to say a message: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const look = async (bot: Bot): Promise<Bot> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${bot.id}`);

        return {...bot, ...parsePixelResponse(response.data)};
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to say look: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};