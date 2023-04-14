package paintbots;

public class Main {

    public static void main(String[] args) {
        String name = args[0];
        Bot b = new Bot(name);
        for (int i = 0; i < 10; i++) {
            b.move(Bot.Dir.LEFT);
            b.paint();
        }
    }
}
