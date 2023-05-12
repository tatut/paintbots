using System.Web;

namespace PaintBots;

public class BotClient
{
    private readonly HttpClient _httpClient;
    private string Url { get; }
    
    public BotClient(string? url = null)
    {
        Url = url ?? "http://localhost:31173";
        
        _httpClient = new HttpClient
        {
            BaseAddress = new Uri(Url)
        };
    }

    /// <summary>
    /// Register name with server
    /// </summary>
    /// <param name="name"></param>
    /// <returns>Id of the registered bot</returns>
    public async Task<Guid> Register(string name)
    {
        var formContent = new FormUrlEncodedContent(new Dictionary<string, string> {{"register", name}});
        var result = await _httpClient.PostAsync("", formContent);
        return Guid.Parse(await result.Content.ReadAsStringAsync());
    }

    /// <summary>
    /// POST against the API with any collection of bot arguments: https://github.com/tatut/paintbots#bot-commands
    /// </summary>
    /// <param name="args">Dictionary of key value pairs to be used as arguments for the POST</param>
    /// <returns>Returns the received state of the <see cref="BotResponse"/></returns>
    public async Task<BotResponse?> PostCommand(IDictionary<string, string> args)
    {
        var formContent = new FormUrlEncodedContent(args);
        var response = await _httpClient.PostAsync("", formContent);
        var responseQueryString = await response.Content.ReadAsStringAsync();
        BotResponse? botResponse = null;
        if (!string.IsNullOrEmpty(responseQueryString))
        {
            var parsed = HttpUtility.ParseQueryString(responseQueryString);
            botResponse = new BotResponse(int.Parse(parsed["x"]!), int.Parse(parsed["y"]!), parsed["color"]!);
        }
        return botResponse;
    }
}

/// <summary>
/// Simply record matching the expected response for API requests related to a Bot
/// </summary>
/// <param name="X"></param>
/// <param name="Y"></param>
/// <param name="Color"></param>
public record BotResponse(int X, int Y, string Color);