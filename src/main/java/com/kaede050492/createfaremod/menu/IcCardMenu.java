package com.kaede050492.createfaremod.menu;

import com.kaede050492.createfaremod.currency.CurrencyAdapter;
import com.kaede050492.createfaremod.currency.LightmansCurrencyAdapter;
import com.kaede050492.createfaremod.data.CardLedgerSavedData;
import com.kaede050492.createfaremod.data.TransactionLogSavedData;
import com.kaede050492.createfaremod.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public final class IcCardMenu extends AbstractContainerMenu {
    private static final int HISTORY_LIMIT = 6;

    private final CardView cardView;

    public IcCardMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, readCardView(buffer));
    }

    private IcCardMenu(int containerId, CardView cardView) {
        super(ModMenus.IC_CARD.get(), containerId);
        this.cardView = cardView;
    }

    public static boolean open(ServerPlayer player, ItemStack stack) {
        CardLedgerSavedData ledger = CardLedgerSavedData.get(player.getServer());
        CardLedgerSavedData.Authentication authentication = ledger.inspect(stack, player);
        if (!authentication.valid()) {
            player.displayClientMessage(Component.literal(authentication.message()), true);
            return false;
        }
        CurrencyAdapter.Balance balance = LightmansCurrencyAdapter.INSTANCE.getBalance(player);
        List<HistoryEntry> history = TransactionLogSavedData.get(player.getServer())
                .newestForCard(authentication.cardId(), HISTORY_LIMIT)
                .stream()
                .map(HistoryEntry::from)
                .toList();
        CardLedgerSavedData.CardRecord record = authentication.record();
        CardView view = new CardView(
                authentication.cardId(),
                balance.amount(),
                balance.displayText(),
                record.entered(),
                record.entryStationName(),
                record.entryTimestamp(),
                record.lineId(),
                history
        );
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.createfaremod.ic_card.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player menuPlayer) {
                return new IcCardMenu(id, view);
            }
        }, buffer -> writeCardView(buffer, view));
        return true;
    }

    public CardView getCardView() {
        return cardView;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }

    private static void writeCardView(RegistryFriendlyByteBuf buffer, CardView view) {
        buffer.writeUUID(view.cardId());
        buffer.writeVarLong(view.balance());
        buffer.writeUtf(view.balanceText(), 128);
        buffer.writeBoolean(view.entered());
        buffer.writeUtf(view.entryStation(), 64);
        buffer.writeVarLong(view.entryTimestamp());
        buffer.writeUtf(view.lineId(), 32);
        buffer.writeVarInt(view.history().size());
        for (HistoryEntry entry : view.history()) {
            buffer.writeVarLong(entry.timestamp());
            buffer.writeUtf(entry.entryStation(), 64);
            buffer.writeUtf(entry.exitStation(), 64);
            buffer.writeVarLong(entry.fare());
            buffer.writeUtf(entry.result(), 64);
        }
    }

    private static CardView readCardView(RegistryFriendlyByteBuf buffer) {
        UUID cardId = buffer.readUUID();
        long balance = buffer.readVarLong();
        String balanceText = buffer.readUtf(128);
        boolean entered = buffer.readBoolean();
        String entryStation = buffer.readUtf(64);
        long entryTimestamp = buffer.readVarLong();
        String lineId = buffer.readUtf(32);
        int count = Math.min(buffer.readVarInt(), HISTORY_LIMIT);
        List<HistoryEntry> history = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            history.add(new HistoryEntry(
                    buffer.readVarLong(),
                    buffer.readUtf(64),
                    buffer.readUtf(64),
                    buffer.readVarLong(),
                    buffer.readUtf(64)
            ));
        }
        return new CardView(
                cardId,
                balance,
                balanceText,
                entered,
                entryStation,
                entryTimestamp,
                lineId,
                List.copyOf(history)
        );
    }

    public record CardView(
            UUID cardId,
            long balance,
            String balanceText,
            boolean entered,
            String entryStation,
            long entryTimestamp,
            String lineId,
            List<HistoryEntry> history
    ) {
    }

    public record HistoryEntry(
            long timestamp,
            String entryStation,
            String exitStation,
            long fare,
            String result
    ) {
        private static HistoryEntry from(TransactionLogSavedData.Transaction transaction) {
            return new HistoryEntry(
                    transaction.time(),
                    transaction.stationIn(),
                    transaction.stationOut(),
                    transaction.fare(),
                    transaction.result()
            );
        }
    }
}
