package paintbots;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

/**
 * HTML interface helper to call paintbots server.
 */
public class BotClient {
    static String LOCAL_URL = "http://localhost:31173";

    static final String url;
    static {
        String envUrl = System.getenv("PAINTBOTS_URL");
        url = envUrl == null ? LOCAL_URL : envUrl;
    }

    private static String enc(Object s) {
        if(s == null) return "";
        try {
            return URLEncoder.encode(s.toString(), "UTF-8");
        } catch(UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }

    private static String post(Object... data) {
        var b = new StringBuilder();
        for(int i=0; i<data.length/2; i++) {
            if(i>0) b.append("&");
            b.append(enc(data[i*2+0])).append("=").append(enc(data[i*2+1]));
        }
        var req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(b.toString()))
            .build();
        try {
            HttpResponse<String> resp = HttpClient.newHttpClient().send(req, BodyHandlers.ofString());
            if(resp.statusCode() >= 400) {
                throw new RuntimeException("Got unexpected status: "+resp.statusCode()+", with body: "+resp.body());
            } else {
                return resp.body();
            }
        } catch(IOException ioe) {
            throw new RuntimeException("IO Exception in POST", ioe);
        } catch(InterruptedException ie) {
            throw new RuntimeException("Interrupted in POST", ie);
        }
    }

    public static class BotResponse {
        public int x;
        public int y;
        public String color;
        BotResponse(String s) {
            System.out.println("GOT: "+s);
            x = 0; y = 0;
            for(String fd : s.split("&")) {
                String[] kv = fd.split("=");
                switch(kv[0]) {
                case "x": x = Integer.parseInt(kv[1]); break;
                case "y": y = Integer.parseInt(kv[1]); break;
                case "color": color = kv[1]; break;
                }
            }
        }
    }

    public static BotResponse cmd(Object... args) {
        return new BotResponse(post(args));
    }

    /**
     * Register name with server, returns id.
     */
    public static String register(String name) {
        return post("register", name);
    }


}
