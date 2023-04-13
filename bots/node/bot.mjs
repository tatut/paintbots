import fetch from 'node-fetch';
import http from 'http';
import https from 'https';

const httpAgent = new http.Agent({ keepAlive: false });
const httpsAgent = new https.Agent({ keepAlive: false });
const agent = (_parsedURL) => _parsedURL.protocol == 'http:' ? httpAgent : httpsAgent;


const URL = process.argv[2] || "http://localhost:31173";
console.log("Using URL: "+ URL);

function params(p) {
    let param = new URLSearchParams();
    for(let k in p) param.append(k, p[k]);
    return param;
}

function post(data) {
    return fetch(URL, {method: "POST", body: params(data), agent});
}

async function register(bot) {
    const r = await post({register: bot.name});
    const id = await r.text();
    console.log("ID: ", id);
    return {id: id, ...bot};
}

// Issue a bot command, returns new bot (updates x/y/color)
async function command(bot, params) {
    //console.log("COMMAND, bot: ", bot, " params: ", params);
    const r = await post({id: bot.id, ...params});
    const fd = await r.formData();
    return {x: parseInt(fd.get("x")), y: parseInt(fd.get("y")), color: fd.get("color"), ...bot};
}

async function move(bot, dir) {
    return await command(bot, {move: dir});
}
async function paint(bot) {
    return await command(bot, {paint: "1"});
}
async function color(bot, col) {
    return await command(bot, {color: col});
}

async function line(bot, dir, count) {
    let b = bot;
    for(let i=0; i < count; i++) {
        b = await paint(b);
        b = await move(b, dir);
    }
    return b;
}

// Main entrypoint here, registers a new bot, draws a "maze"
async function main(name) {
    let bot = await register({name: name})
    bot = await line(bot, "LEFT", 10);
    bot = await line(bot, "DOWN", 9);
    bot = await line(bot, "RIGHT", 8);
    bot = await line(bot, "UP", 7);
    bot = await line(bot, "LEFT", 6);
    bot = await line(bot, "DOWN", 5);
    bot = await line(bot, "RIGHT", 4);
    bot = await line(bot, "UP", 3);
    bot = await line(bot, "LEFT", 2);
    bot = await line(bot, "DOWN", 1);
}

main("Mazer");
