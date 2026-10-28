package server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 23456;

        ExecutorService executor =
                Executors.newFixedThreadPool(10);

        AtomicBoolean running = new AtomicBoolean(true);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started!");
            System.out.flush();

            Bd database = new Bd();
            Controller controller = new Controller(database);
            Gson gson = new Gson();

            while (running.get()) {
                try {
                    Socket socket = server.accept();

                    executor.submit(() -> processClient(
                            socket,
                            server,
                            controller,
                            gson,
                            running
                    ));
                } catch (IOException e) {
                    if (running.get()) {
                        throw e;
                    }
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    private static void processClient(
            Socket socket,
            ServerSocket server,
            Controller controller,
            Gson gson,
            AtomicBoolean running
    ) {
        try (
                Socket currentSocket = socket;
                DataInputStream input =
                        new DataInputStream(
                                currentSocket.getInputStream()
                        );
                DataOutputStream output =
                        new DataOutputStream(
                                currentSocket.getOutputStream()
                        )
        ) {
            String requestString = input.readUTF();

            JsonObject request = JsonParser
                    .parseString(requestString)
                    .getAsJsonObject();

            JsonObject response =
                    controller.processCommand(request);

            output.writeUTF(gson.toJson(response));
            output.flush();

            String type = request
                    .get("type")
                    .getAsString();

            if ("exit".equals(type)) {
                running.set(false);
                server.close();
            }
        } catch (IOException ignored) {
        }
    }
}