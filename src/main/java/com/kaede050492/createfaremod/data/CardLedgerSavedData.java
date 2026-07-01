package com.kaede050492.createfaremod.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.saveddata.SavedData;

public final class CardLedgerSavedData extends SavedData {
    private static final String DATA_NAME = "createfaremod_ic_cards";
    private static final String CARD_ID_TAG = "createfaremod.cardId";
    private static final String COUNTER_TAG = "createfaremod.counter";
    private static final SavedData.Factory<CardLedgerSavedData> FACTORY =
            new SavedData.Factory<>(CardLedgerSavedData::new, CardLedgerSavedData::load);

    private final Map<UUID, CardRecord> cards = new HashMap<>();

    public static CardLedgerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Authentication authenticateAndAdvance(ItemStack stack, ServerPlayer player) {
        CompoundTag itemTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        UUID cardId;
        CardRecord record;

        if (!itemTag.hasUUID(CARD_ID_TAG)) {
            cardId = UUID.randomUUID();
            record = new CardRecord(player.getUUID(), 0L, "", "", "");
            cards.put(cardId, record);
        } else {
            cardId = itemTag.getUUID(CARD_ID_TAG);
            record = cards.get(cardId);
            if (record == null) {
                return Authentication.failure("This IC card is not registered.");
            }
            if (!record.owner().equals(player.getUUID())) {
                return Authentication.failure("This IC card belongs to another player.");
            }
            if (itemTag.getLong(COUNTER_TAG) != record.counter()) {
                return Authentication.failure("A copied or modified IC card was rejected.");
            }
        }

        CardRecord advanced = record.withCounter(record.counter() + 1);
        cards.put(cardId, advanced);
        itemTag.putUUID(CARD_ID_TAG, cardId);
        itemTag.putLong(COUNTER_TAG, advanced.counter());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemTag));
        setDirty();
        return Authentication.success(cardId, advanced);
    }

    public void setEntry(UUID cardId, String stationId, String stationName, String lineId) {
        CardRecord current = cards.get(cardId);
        if (current != null) {
            cards.put(cardId, new CardRecord(
                    current.owner(), current.counter(), stationId, stationName, lineId
            ));
            setDirty();
        }
    }

    public void clearEntry(UUID cardId) {
        CardRecord current = cards.get(cardId);
        if (current != null) {
            cards.put(cardId, new CardRecord(current.owner(), current.counter(), "", "", ""));
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        cards.forEach((cardId, record) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("cardId", cardId);
            entry.putUUID("owner", record.owner());
            entry.putLong("counter", record.counter());
            entry.putString("entryStationId", record.entryStationId());
            entry.putString("entryStationName", record.entryStationName());
            entry.putString("lineId", record.lineId());
            entries.add(entry);
        });
        tag.put("cards", entries);
        return tag;
    }

    private static CardLedgerSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CardLedgerSavedData data = new CardLedgerSavedData();
        ListTag entries = tag.getList("cards", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (entry.hasUUID("cardId") && entry.hasUUID("owner")) {
                data.cards.put(entry.getUUID("cardId"), new CardRecord(
                        entry.getUUID("owner"),
                        entry.getLong("counter"),
                        entry.getString("entryStationId"),
                        entry.getString("entryStationName"),
                        entry.getString("lineId")
                ));
            }
        }
        return data;
    }

    public record CardRecord(
            UUID owner,
            long counter,
            String entryStationId,
            String entryStationName,
            String lineId
    ) {
        public boolean hasEntry() {
            return !entryStationId.isBlank();
        }

        private CardRecord withCounter(long updatedCounter) {
            return new CardRecord(owner, updatedCounter, entryStationId, entryStationName, lineId);
        }
    }

    public record Authentication(boolean valid, String message, UUID cardId, CardRecord record) {
        private static Authentication success(UUID cardId, CardRecord record) {
            return new Authentication(true, "", cardId, record);
        }

        private static Authentication failure(String message) {
            return new Authentication(false, message, null, null);
        }
    }
}
