package com.kaede050492.createfaremod.fare;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kaede050492.createfaremod.CreateFareMod;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;

public final class FareManager {
    private static final Gson GSON = new Gson();
    private static final int MAX_STATIONS = 2_048;

    private FareManager() {
    }

    public static FareResult calculate(
            MinecraftServer server,
            String tableId,
            String lineId,
            String entryStationId,
            String exitStationId
    ) {
        ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(
                CreateFareMod.MOD_ID,
                "fare_tables/" + tableId + ".json"
        );
        Resource resource = server.getResourceManager().getResource(resourceId).orElse(null);
        if (resource == null) {
            return FareResult.failure("Fare table '" + tableId + "' was not found.");
        }
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return FareResult.failure("Fare table '" + tableId + "' is empty.");
            }
            return calculate(root, lineId, entryStationId, exitStationId);
        } catch (IOException | JsonParseException | ArithmeticException exception) {
            return FareResult.failure("Fare table '" + tableId + "' is invalid.");
        }
    }

    private static FareResult calculate(
            JsonObject root,
            String lineId,
            String entryStationId,
            String exitStationId
    ) {
        String configuredLine = requiredString(root, "line");
        if (!configuredLine.equals("*") && !configuredLine.equals(lineId)) {
            return FareResult.failure("The fare table does not support line '" + lineId + "'.");
        }
        String type = requiredString(root, "type").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "distance" -> distanceFare(root, entryStationId, exitStationId);
            case "fixed" -> fixedFare(root, entryStationId, exitStationId);
            default -> FareResult.failure("Unsupported fare table type '" + type + "'.");
        };
    }

    private static FareResult distanceFare(
            JsonObject root,
            String entryStationId,
            String exitStationId
    ) {
        long baseFare = requiredNonNegativeLong(root, "baseFare");
        long minimumFare = requiredNonNegativeLong(root, "minimumFare");
        long perDistance = requiredNonNegativeLong(root, "perDistance");
        JsonObject stationObject = requiredObject(root, "stations");
        if (stationObject.size() > MAX_STATIONS) {
            throw new JsonParseException("Too many stations");
        }
        Map<String, Long> distances = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : stationObject.entrySet()) {
            long distance = entry.getValue().getAsLong();
            if (distance < 0L) {
                throw new JsonParseException("Station distance cannot be negative");
            }
            distances.put(entry.getKey(), distance);
        }
        Long entryDistance = distances.get(entryStationId);
        Long exitDistance = distances.get(exitStationId);
        if (entryDistance == null || exitDistance == null) {
            return FareResult.failure("The route does not contain both stations.");
        }
        long distance = Math.abs(Math.subtractExact(exitDistance, entryDistance));
        long fare = Math.addExact(baseFare, Math.multiplyExact(distance, perDistance));
        return FareResult.success(Math.max(minimumFare, fare), distance);
    }

    private static FareResult fixedFare(
            JsonObject root,
            String entryStationId,
            String exitStationId
    ) {
        JsonObject fares = requiredObject(root, "fares");
        Long fare = lookupFixedFare(fares, entryStationId, exitStationId);
        if (fare == null) {
            fare = lookupFixedFare(fares, exitStationId, entryStationId);
        }
        if (fare == null) {
            return FareResult.failure("No fixed fare is defined for this journey.");
        }
        if (fare < 0L) {
            throw new JsonParseException("Fare cannot be negative");
        }
        return FareResult.success(fare, 0L);
    }

    private static Long lookupFixedFare(JsonObject fares, String from, String to) {
        JsonElement fromElement = fares.get(from);
        if (fromElement == null || !fromElement.isJsonObject()) {
            return null;
        }
        JsonElement fareElement = fromElement.getAsJsonObject().get(to);
        return fareElement == null ? null : fareElement.getAsLong();
    }

    private static String requiredString(JsonObject root, String name) {
        JsonElement element = root.get(name);
        if (element == null || !element.isJsonPrimitive() || element.getAsString().isBlank()) {
            throw new JsonParseException("Missing " + name);
        }
        return element.getAsString().trim();
    }

    private static long requiredNonNegativeLong(JsonObject root, String name) {
        JsonElement element = root.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            throw new JsonParseException("Missing " + name);
        }
        long value = element.getAsLong();
        if (value < 0L) {
            throw new JsonParseException(name + " cannot be negative");
        }
        return value;
    }

    private static JsonObject requiredObject(JsonObject root, String name) {
        JsonElement element = root.get(name);
        if (element == null || !element.isJsonObject()) {
            throw new JsonParseException("Missing " + name);
        }
        return element.getAsJsonObject();
    }

    public record FareResult(boolean success, long fare, long distance, String message) {
        private static FareResult success(long fare, long distance) {
            return new FareResult(true, fare, distance, "");
        }

        private static FareResult failure(String message) {
            return new FareResult(false, 0L, 0L, message);
        }
    }
}
