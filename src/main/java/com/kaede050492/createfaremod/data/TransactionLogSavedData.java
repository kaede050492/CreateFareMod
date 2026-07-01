package com.kaede050492.createfaremod.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class TransactionLogSavedData extends SavedData {
    private static final String DATA_NAME = "createfaremod_transactions";
    private static final int MAX_TRANSACTIONS = 10_000;
    private static final SavedData.Factory<TransactionLogSavedData> FACTORY =
            new SavedData.Factory<>(TransactionLogSavedData::new, TransactionLogSavedData::load);

    private final List<Transaction> transactions = new ArrayList<>();

    public static TransactionLogSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void append(Transaction transaction) {
        transactions.add(transaction);
        if (transactions.size() > MAX_TRANSACTIONS) {
            transactions.remove(0);
        }
        setDirty();
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public List<Transaction> findByPlayer(UUID playerUuid) {
        return transactions.stream()
                .filter(transaction -> transaction.playerUuid().equals(playerUuid))
                .toList();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Transaction transaction : transactions) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("time", transaction.time());
            entry.putUUID("player", transaction.playerUuid());
            entry.putString("stationIn", transaction.stationIn());
            entry.putString("stationOut", transaction.stationOut());
            entry.putLong("fare", transaction.fare());
            entry.putString("account", transaction.account());
            entry.putString("result", transaction.result());
            entries.add(entry);
        }
        tag.put("transactions", entries);
        return tag;
    }

    private static TransactionLogSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TransactionLogSavedData data = new TransactionLogSavedData();
        ListTag entries = tag.getList("transactions", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (entry.hasUUID("player")) {
                data.transactions.add(new Transaction(
                        entry.getLong("time"),
                        entry.getUUID("player"),
                        entry.getString("stationIn"),
                        entry.getString("stationOut"),
                        entry.getLong("fare"),
                        entry.getString("account"),
                        entry.getString("result")
                ));
            }
        }
        return data;
    }

    public record Transaction(
            long time,
            UUID playerUuid,
            String stationIn,
            String stationOut,
            long fare,
            String account,
            String result
    ) {
    }
}
