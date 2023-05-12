using PaintBots;

Console.WriteLine("Hello Bots!");
const string name = "BotName2"; // TODO From config or command line args
var bot = new Bot(name, new BotClient()); // TODO Pass server url from config or command line
Console.WriteLine($"Initializing bot: {bot.Name}");
try
{
    await bot.Register();
    Console.WriteLine($"Bot registered with ID: {bot.Id}");
    await bot.Color("8");
    Console.WriteLine("Drawing...");
    for (var i = 0; i < 10; i++)
    {
        await bot.Move(Bot.Dir.Left);
        await bot.Paint();
    }
}
finally
{
    Console.WriteLine("Bye bye!");
    await bot.Bye(false);
}
