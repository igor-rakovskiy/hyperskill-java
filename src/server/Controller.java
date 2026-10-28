package server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Controller {
    private final Bd database;

    public Controller(Bd database) {
        this.database = database;
    }

    public JsonObject processCommand(JsonObject request) {
        String type = request
                .get("type")
                .getAsString();

        switch (type) {
            case "set": return set(request);

            case "get": return get(request);

            case "delete": return delete(request);

            case "exit": return createOkResponse();

            default: return createErrorResponse("Unknown request");
        }
    }

    private JsonObject set(JsonObject request) {
        JsonElement key = request.get("key");
        JsonElement value = request.get("value");

        database.set(key, value);

        return createOkResponse();
    }

    private JsonObject get(JsonObject request) {
        JsonElement key = request.get("key");
        JsonElement value = database.get(key);

        if (value == null) {
            return createErrorResponse("No such key");
        }

        JsonObject response = createOkResponse();
        response.add("value", value);

        return response;
    }

    private JsonObject delete(JsonObject request) {
        JsonElement key = request.get("key");

        if (!database.delete(key)) {
            return createErrorResponse("No such key");
        }

        return createOkResponse();
    }

    private JsonObject createOkResponse() {
        JsonObject response = new JsonObject();
        response.addProperty("response", "OK");
        return response;
    }

    private JsonObject createErrorResponse(String reason) {
        JsonObject response = new JsonObject();
        response.addProperty("response", "ERROR");
        response.addProperty("reason", reason);
        return response;
    }
}