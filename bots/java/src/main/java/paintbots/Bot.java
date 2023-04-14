package paintbots;

public class Bot {
    private String name;
    private String id;

    private int x;
    private int y;
    private String color;

    public String getName() { return name; }
    public int getX() { return x; }
    public int getY() { return y; }

    public Bot(String name) {
        this.name = name;
        id = BotClient.register(name);
        /* get these with info command */
        x = 0;
        y = 0;
        color = "";
    }

    private void update(BotClient.BotResponse r) {
        this.x = r.x;
        this.y = r.y;
        this.color = r.color;
    }

    public enum Dir { LEFT, RIGHT, UP, DOWN };
    
    public void move(Dir d) {
        update(BotClient.cmd("id", id, "move", d));
    }

    public void paint() {
        update(BotClient.cmd("id", id, "paint", "1"));
    }

    public void color(String col) {
        update(BotClient.cmd("id", id, "color", col));
    }
}
