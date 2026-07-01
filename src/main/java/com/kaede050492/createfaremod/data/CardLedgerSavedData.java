package com.kaede050492.createfaremod.data;

import com.kaede050492.createfaremod.registry.ModItems;
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
    private static final String OWNER_TAG = "createfaremod.owner";
    private static final String ENTERED_TAG = "createfaremod.entered";
    private static final String ENTRY_STATION_ID_TAG = "createfaremod.entryStationId";
    private static final String ENTRY_STATION_NAME_TAG = "createfaremod.entryStationName";
    private static final String ENTRY_GATE_ID_TAG = "createfaremod.entryGateId";
    private static final String ENTRY_TIMESTAMP_TAG = "createfaremod.entryTimestamp";
    private static final String LINE_ID_TAG = "createfaremod.lineId";
    private static final SavedData.Factory<CardLedgerSavedData> FACTORY =
            new SavedData.Factory<>(CardLedgerSavedData::new, CardLedgerSavedData::load);

    private final Map<UUID, CardRecord> cards = new HashMap<>();

    public static CardLedgerSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public ItemStack issueCard(ServerPlayer owner) {
        UUID cardId;
        do {
            cardId = UUID.randomUUID();
        } while (cards.containsKey(cardId));
        CardRecord record = CardRecord.empty(owner.getUUID(), System.currentTimeMillis());
        cards.put(cardId, record);
        ItemStack stack = new ItemStack(ModItems.IC_CARD.get());
        writeAuthoritativeData(stack, cardId, record);
        setDirty();
        return stack;
    }

    public Authentication authenticateAndAdvance(ItemStack stack, ServerPlayer player) {
        Authentication authentication = inspect(stack, player);
        if (!authentication.valid()) {
            return authentication;
        }
        CardRecord advanced = authentication.record().withCounter(authentication.record().counter() + 1L);
        cards.put(authentication.cardId(), advanced);
        writeAuthoritativeData(stack, authentication.cardId(), advanced);
        setDirty();
        return Authentication.success(authentication.cardId(), advanced);
    }

    public Authentication inspect(ItemStack stack, ServerPlayer player) {
        if (!stack.is(ModItems.IC_CARD.get())) {
            return Authentication.failure("Please use an IC Card.");
        }
        CompoundTag itemTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!itemTag.hasUUID(CARD_ID_TAG)) {
            return Authentication.failure("This IC card was not issued by an IC Card Issuer.");
        }
        UUID cardId = itemTag.getUUID(CARD_ID_TAG);
        CardRecord record = cards.get(cardId);
        if (record == null) {
            return Authentication.failure("This IC card is not registered.");
        }
        if (!record.owner().equals(player.getUUID())) {
            return Authentication.failure("This IC card belongs to another player.");
        }
        if (itemTag.getLong(COUNTER_TAG) != record.counter()) {
            return Authentication.failure("A copied or modified IC card was rejected.");
        }
        writeAuthoritativeData(stack, cardId, record);
        return Authentication.success(cardId, record);
    }

    public CardRecord setEntry(
            UUID cardId,
            ItemStack stack,
            String stationId,
            String stationName,
            UUID gateId,
            long entryTimestamp,
            String lineId
    ) {
        CardRecord current = cards.get(cardId);
        if (current == null) {
            return null;
        }
        CardRecord updated = new CardRecord(
                current.owner(),
                current.counter(),
                current.issuedAt(),
                true,
                stationId,
                stationName,
                gateId,
                entryTimestamp,
                lineId
        );
        cards.put(cardId, updated);
        writeAuthoritativeData(stack, cardId, updated);
        setDirty();
        return updated;
    }

    public CardRecord clearEntry(UUID cardId, ItemStack stack) {
        CardRecord current = cards.get(cardId);
        if (current == null) {
            return null;
        }
        CardRecord updated = CardRecord.empty(current.owner(), current.issuedAt())
                .withCounter(current.counter());
        cards.put(cardId, updated);
        writeAuthoritativeData(stack, cardId, updated);
        setDirty();
        return updated;
    }

    public CardRecord getRecord(UUID cardId) {
        return cards.get(cardId);
    }

    public static DisplayData readDisplayData(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return new DisplayData(
                tag.hasUUID(CARD_ID_TAG) ? tag.getUUID(CARD_ID_TAG) : null,
                tag.getBoolean(ENTERED_TAG),
                tag.getString(ENTRY_STATION_ID_TAG),
                tag.getString(ENTRY_STATION_NAME_TAG),
                tag.getLong(ENTRY_TIMESTAMP_TAG),
                tag.getString(LINE_ID_TAG)
        );
    }

    private static void writeAuthoritativeData(ItemStack stack, UUID cardId, CardRecord record) {
        CompoundTag itemTag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        itemTag.putUUID(CARD_ID_TAG, cardId);
        itemTag.putLong(COUNTER_TAG, record.counter());
        itemTag.putUUID(OWNER_TAG, record.owner());
        itemTag.putBoolean(ENTERED_TAG, record.entered());
        itemTag.putString(ENTRY_STATION_ID_TAG, record.entryStationId());
        itemTag.putString(ENTRY_STATION_NAME_TAG, record.entryStationName());
        itemTag.putLong(ENTRY_TIMESTAMP_TAG, record.entryTimestamp());
        itemTag.putString(LINE_ID_TAG, record.lineId());
        if (record.entryGateId() != null) {
            itemTag.putUUID(ENTRY_GATE_ID_TAG, record.entryGateId());
        } else {
            itemTag.remove(ENTRY_GATE_ID_TAG);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemTag));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        cards.forEach((cardId, record) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("cardId", cardId);
            entry.putUUID("owner", record.owner());
            entry.putLong("counter", record.counter());
            entry.putLong("issuedAt", record.issuedAt());
            entry.putBoolean("entered", record.entered());
            entry.putString("entryStationId", record.entryStationId());
            entry.putString("entryStationName", record.entryStationName());
            if (record.entryGateId() != null) {
                entry.putUUID("entryGateId", record.entryGateId());
            }
            entry.putLong("entryTimestamp", record.entryTimestamp());
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
            if (!entry.hasUUID("cardId") || !entry.hasUUID("owner")) {
                continue;
            }
            String stationId = entry.getString("entryStationId");
            boolean entered = entry.contains("entered", Tag.TAG_BYTE)
                    ? entry.getBoolean("entered")
                    : !stationId.isBlank();
            data.cards.put(entry.getUUID("cardId"), new CardRecord(
                    entry.getUUID("owner"),
                    entry.getLong("counter"),
                    entry.getLong("issuedAt"),
                    entered,
                    stationId,
                    entry.getString("entryStationName"),
                    entry.hasUUID("entryGateId") ? entry.getUUID("entryGateId") : null,
                    entry.getLong("entryTimestamp"),
                    entry.getString("lineId")
            ));
        }
        return data;
    }

    public record CardRecord(
            UUID owner,
            long counter,
            long issuedAt,
            boolean entered,
            String entryStationId,
            String entryStationName,
            UUID entryGateId,
            long entryTimestamp,
            String lineId
    ) {
        public static CardRecord empty(UUID owner, long issuedAt) {
            return new CardRecord(owner, 0L, issuedAt, false, "", "", null, 0L, "");
        }

        public boolean hasEntry() {
            return entered;
        }

        private CardRecord withCounter(long updatedCounter) {
            return new CardRecord(
                    owner,
                    updatedCounter,
                    issuedAt,
                    entered,
                    entryStationId,
                    entryStationName,
                    entryGateId,
                    entryTimestamp,
                    lineId
            );
        }
    }

    public record DisplayData(
            UUID cardId,
            boolean entered,
            String entryStationId,
            String entryStationName,
            long entryTimestamp,
            String lineId
    ) {
        public boolean issued() {
            return cardId != null;
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
