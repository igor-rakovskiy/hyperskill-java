package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        String address = "127.0.0.1";
        int port = 23456;

        String type = null;
        String key = null;
        String value = null;
        String inputFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-t":
                    type = args[++i];
                    break;

                case "-k":
                    key = args[++i];
                    break;

                case "-v":
                    value = args[++i];
                    break;

                case "-in":
                    inputFile = args[++i];
                    break;

                default:
                    break;
            }
        }

        String requestJson;

        if (inputFile != null) {
            Path path = Path.of(
                    System.getProperty("user.dir"),
                    "src",
                    "client",
                    "data",
                    inputFile
            );

            requestJson = Files.readString(
                    path,
                    StandardCharsets.UTF_8
            );
        } else {
            JsonObject request = new JsonObject();
            request.addProperty("type", type);

            if (!"exit".equals(type)) {
                request.addProperty("key", key);
            }

            if ("set".equals(type)) {
                request.addProperty("value", value);
            }

            requestJson = new Gson().toJson(request);
        }

        try (
                Socket socket = new Socket(address, port);
                DataInputStream input =
                        new DataInputStream(
                                socket.getInputStream()
                        );
                DataOutputStream output =
                        new DataOutputStream(
                                socket.getOutputStream()
                        )
        ) {
            System.out.println("Client started!");

            output.writeUTF(requestJson);
            output.flush();

            System.out.println("Sent: " + requestJson);

            String response = input.readUTF();

            System.out.println("Received: " + response);
        }
    }
}