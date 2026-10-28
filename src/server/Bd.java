package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Bd {
    private final Path databasePath;
    private final Gson gson;
    private final JsonObject database;
    private final ReadWriteLock lock;

    public Bd() throws IOException {
        databasePath = Path.of(
                System.getProperty("user.dir"),
                "src",
                "server",
                "data",
                "db.json"
        );

        gson = new Gson();
        lock = new ReentrantReadWriteLock();

        Files.createDirectories(databasePath.getParent());

        if (Files.exists(databasePath)) {
            String json = Files.readString(
                    databasePath,
                    StandardCharsets.UTF_8
            ).strip();

            database = json.isEmpty()
                    ? new JsonObject()
                    : JsonParser.parseString(json).getAsJsonObject();
        } else {
            database = new JsonObject();
            saveDatabase();
        }
    }

    public JsonElement get(JsonElement key) {
        lock.readLock().lock();

        try {
            List<String> path = getPath(key);
            JsonElement current = database;

            for (String part : path) {
                if (!current.isJsonObject()) {
                    return null;
                }

                JsonObject object = current.getAsJsonObject();

                if (!object.has(part)) {
                    return null;
                }

                current = object.get(part);
            }

            return current.deepCopy();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void set(JsonElement key, JsonElement value) {
        lock.writeLock().lock();

        try {
            List<String> path = getPath(key);
            JsonObject current = database;

            for (int i = 0; i < path.size() - 1; i++) {
                String part = path.get(i);
                JsonElement child = current.get(part);

                if (child == null || !child.isJsonObject()) {
                    JsonObject newObject = new JsonObject();
                    current.add(part, newObject);
                    current = newObject;
                } else {
                    current = child.getAsJsonObject();
                }
            }

            String lastKey = path.get(path.size() - 1);
            current.add(lastKey, value.deepCopy());

            saveDatabase();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean delete(JsonElement key) {
        lock.writeLock().lock();

        try {
            List<String> path = getPath(key);
            JsonObject current = database;

            for (int i = 0; i < path.size() - 1; i++) {
                String part = path.get(i);

                if (!current.has(part)
                        || !current.get(part).isJsonObject()) {
                    return false;
                }

                current = current.getAsJsonObject(part);
            }

            String lastKey = path.get(path.size() - 1);

            if (!current.has(lastKey)) {
                return false;
            }

            current.remove(lastKey);
            saveDatabase();

            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<String> getPath(JsonElement key) {
        List<String> path = new ArrayList<>();

        if (key != null && key.isJsonPrimitive()) {
            path.add(key.getAsString());
            return path;
        }

        if (key != null && key.isJsonArray()) {
            JsonArray array = key.getAsJsonArray();

            for (JsonElement element : array) {
                path.add(element.getAsString());
            }

            if (!path.isEmpty()) {
                return path;
            }
        }

        throw new IllegalArgumentException();
    }

    private void saveDatabase() throws IOException {
        Files.writeString(
                databasePath,
                gson.toJson(database),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }
}