package com.kaede050492.createfaremod.gate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.nbt.CompoundTag;

public record GateConfiguration(
        String stationId,
        String stationName,
        String lineId,
        String accountId,
        String accountName,
        GateMode gateMode,
        Map<String, Long> fareTable,
        UUID ownerUuid
) {
    public static final int MAX_ID_LENGTH = 32;
    public static final int MAX_NAME_LENGTH = 64;
    public static final int MAX_ACCOUNT_LENGTH = 80;
    public static final int MAX_FARE_ENTRIES = 128;
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");

    public GateConfiguration {
        stationId = clean(stationId);
        stationName = clean(stationName);
        lineId = clean(lineId);
        accountId = clean(accountId);
        accountName = clean(accountName);
        gateMode = gateMode == null ? GateMode.BIDIRECTIONAL : gateMode;
        fareTable = Map.copyOf(fareTable == null ? Map.of() : new TreeMap<>(fareTable));
    }

    public static GateConfiguration empty() {
        return new GateConfiguration("", "", "", "", "", GateMode.BIDIRECTIONAL, Map.of(), null);
    }

    public Optional<String> validate() {
        if (!validId(stationId)) {
            return Optional.of("Station ID must contain 1-32 letters, numbers, '.', '_' or '-'.");
        }
        if (stationName.isBlank() || stationName.length() > MAX_NAME_LENGTH) {
            return Optional.of("Station name must contain 1-64 characters.");
        }
        if (!validId(lineId)) {
            return Optional.of("Line ID must contain 1-32 letters, numbers, '.', '_' or '-'.");
        }
        if (accountId.isBlank() || accountId.length() > MAX_ACCOUNT_LENGTH) {
            return Optional.of("LC account must use player:<UUID> or team:<ID>.");
        }
        if (fareTable.size() > MAX_FARE_ENTRIES) {
            return Optional.of("Fare table has too many entries.");
        }
        for (Map.Entry<String, Long> entry : fareTable.entrySet()) {
            if (!validId(entry.getKey()) || entry.getValue() == null || entry.getValue() < 0) {
                return Optional.of("Fare entries must use stationId=non-negativeAmount.");
            }
        }
        return Optional.empty();
    }

    public long fareFrom(String entryStationId) {
        return fareTable.getOrDefault(entryStationId, -1L);
    }

    public GateConfiguration withOwner(UUID owner) {
        return new GateConfiguration(
                stationId, stationName, lineId, accountId, accountName, gateMode, fareTable, owner
        );
    }

    public GateConfiguration withAccountName(String resolvedName) {
        return new GateConfiguration(
                stationId, stationName, lineId, accountId, resolvedName, gateMode, fareTable, ownerUuid
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("stationId", stationId);
        tag.putString("stationName", stationName);
        tag.putString("lineId", lineId);
        tag.putString("accountId", accountId);
        tag.putString("accountName", accountName);
        tag.putString("gateMode", gateMode.name());
        CompoundTag fares = new CompoundTag();
        fareTable.forEach(fares::putLong);
        tag.put("fareTable", fares);
        if (ownerUuid != null) {
            tag.putUUID("ownerUUID", ownerUuid);
        }
        return tag;
    }

    public static GateConfiguration load(CompoundTag tag) {
        Map<String, Long> fares = new LinkedHashMap<>();
        CompoundTag fareTag = tag.getCompound("fareTable");
        for (String key : fareTag.getAllKeys()) {
            fares.put(key, fareTag.getLong(key));
        }
        UUID owner = tag.hasUUID("ownerUUID") ? tag.getUUID("ownerUUID") : null;
        return new GateConfiguration(
                tag.getString("stationId"),
                tag.getString("stationName"),
                tag.getString("lineId"),
                tag.getString("accountId"),
                tag.getString("accountName"),
                GateMode.parse(tag.getString("gateMode")),
                fares,
                owner
        );
    }

    public static Map<String, Long> parseFareTable(String value) {
        Map<String, Long> fares = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return fares;
        }
        String[] entries = value.split("[;,]");
        if (entries.length > MAX_FARE_ENTRIES) {
            throw new IllegalArgumentException("Fare table has too many entries.");
        }
        for (String rawEntry : entries) {
            String[] parts = rawEntry.trim().split("=", 2);
            if (parts.length != 2 || !validId(parts[0].trim())) {
                throw new IllegalArgumentException("Use stationId=amount entries separated by semicolons.");
            }
            long amount;
            try {
                amount = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Fare amounts must be whole numbers.", exception);
            }
            if (amount < 0) {
                throw new IllegalArgumentException("Fare amounts cannot be negative.");
            }
            fares.put(parts[0].trim(), amount);
        }
        return fares;
    }

    public String formatFareTable() {
        return fareTable.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
    }

    private static boolean validId(String value) {
        return value != null
                && !value.isBlank()
                && value.length() <= MAX_ID_LENGTH
                && ID_PATTERN.matcher(value).matches();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
