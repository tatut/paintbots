namespace PaintBots;

public class Bot {
    private readonly BotClient _botClient;
    public bool Registered { get; set; }
    public string Name { get; }
    public Guid Id { get; set; }

    public int X { get; set; }
    public int Y { get; set; }
    public string Colour { get; set; } = "";

    public Bot(string name, BotClient botClient) {
        Name = name;
        _botClient = botClient;
    }

    public async Task Register()
    {
        Id = await _botClient.Register(Name);
        Registered = true;
    }
    
    private void CheckRegistered() {
        if(!Registered) throw new InvalidOperationException("Not registered!");
    }

    private void Update(BotResponse botResponse) {
        Console.WriteLine(botResponse);
        
        X = botResponse.X;
        Y = botResponse.Y;
        Colour = botResponse.Color;
    }

    public enum Dir { Left, Right, Up, Down };

    public async Task Move(Dir d)
        => await Do("move", d.ToString().ToUpper());

    public async Task Paint()
        => await Do("paint", "1");

    public async Task Color(string color) 
        => await Do("color", color);

    public async Task Bye(bool checkRegistration = true) 
        => await Do("bye", "1", checkRegistration);

    private async Task Do(string argument, string value, bool checkRegistration = true)
    {
        if (checkRegistration)
        {
            CheckRegistered();
        }
        var args = new Dictionary<string, string>()
        {
            {"id", Id.ToString()},
            {argument, value}
        };
        var updatedBot = await _botClient.PostCommand(args);
        if(updatedBot != null) Update(updatedBot);
    }
}