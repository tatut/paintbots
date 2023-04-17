# Python bot client

Tested with python 3.8.10.

## Set up the API url and bot name

1. If you need to change the API base URL, edit the `API_URL` variable in `api.py` before running the bot
   * Or, use API_URL environment variable when running the python script
2. Change the bot name in `main.py`

## Install deps
1. Setup venv
2. Activate venv `$ source <project root>/venv/bin/activate`
3. Install requirements.txt deps in venv: `$ pip install -r requirements.txt`

## Running the bot

Run the script with `$ python -u main.py`  
Or, with `$ API_URL="http://your-url.com" python -u main.py`

## Problems with registering the bot?

If you restart the local paintbots server (or if the cloud server is restarted), you will need to re-register the bot.  
In that case, remove the `botConfig.cfg` file you created from your client directory or change the name of the bot in the index.ts file.
to re-register the next time you run the bot script.
