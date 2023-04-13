import axios from 'axios';
import {Pixel} from './types/Pixel';

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
    const params = response.split('&');
    const pixel = Object.fromEntries(params.map(el => el.split('=')));

    return {color: pixel.color, position: {x: pixel.x, y: pixel.y}}
};

export const moveBot = async (id: string, dir: string): Promise<Pixel> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${id}&move=${dir}`);

        return parsePixelResponse(response.data);
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to move bot: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const setColor = async (id: string, color: string): Promise<Pixel> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${id}&move=${color}`);

        return parsePixelResponse(response.data);
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to set color: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const paintPixel = async (id: string): Promise<Pixel> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${id}&paint`);

        return parsePixelResponse(response.data);
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to paint: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const clearPixel = async (id: string): Promise<Pixel> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${id}`);

        return parsePixelResponse(response.data);
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to clear a pixel: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const say = async (id: string, message: string): Promise<Pixel> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${id}&msg=${message}`);

        return parsePixelResponse(response.data);
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to say a message: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};

export const look = async (id: string): Promise<Pixel> => {
    try {
        const response = await axios.post(`${API_URL}`, `id=${id}`);

        return parsePixelResponse(response.data);
    } catch (e) {
        if (axios.isAxiosError(e)) {
            const errResp = e.response;
            throw Error(`Failed to say look: ${errResp?.data}`)
        } else {
            throw e;
        }
    }
};