# Node.js bot in TypeScript

Tested with Node.js version 18.14.0

### Install deps

1. Install Nodejs 18+ (use for example NVM for this).
2. Run `$ npm install`

## Set up the API url and bot name
1. If you need to change the API base URL, edit the `API_URL` variable in `types/Api.ts` before running the bot
2. Change the bot name in `index.ts`

## Running the bot
Run the script with `$ npm start`  
There are other helpful commands you can use while developing the bot. Check them out in `package.json`.

## Problems with registering the bot?
If you restart the local paintbots server (or if the cloud server is restarted), you will need to re-register the bot.  
In that case, remove the `botConfig.cfg` file you created from your client directory or change the name of the bot in the index.ts file.
to re-register the next time you run the bot script.