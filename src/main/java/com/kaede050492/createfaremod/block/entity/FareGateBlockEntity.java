package com.kaede050492.createfaremod.block.entity;

import com.kaede050492.createfaremod.block.FareGateBlock;
import com.kaede050492.createfaremod.currency.CurrencyAdapter;
import com.kaede050492.createfaremod.currency.LightmansCurrencyAdapter;
import com.kaede050492.createfaremod.data.CardLedgerSavedData;
import com.kaede050492.createfaremod.data.TransactionLogSavedData;
import com.kaede050492.createfaremod.fare.FareManager;
import com.kaede050492.createfaremod.gate.GateConfiguration;
import com.kaede050492.createfaremod.gate.GateMode;
import com.kaede050492.createfaremod.gate.GateStatus;
import com.kaede050492.createfaremod.menu.FareGateMenu;
import com.kaede050492.createfaremod.network.PaymentFailurePayload;
import com.kaede050492.createfaremod.network.TransactionResultPayload;
import com.kaede050492.createfaremod.registry.ModBlockEntities;
import com.kaede050492.createfaremod.registry.ModItems;
import com.kaede050492.createfaremod.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class FareGateBlockEntity extends BlockEntity {
    private static final String GATE_OPEN_TAG = "gateOpen";
    private static final String LAST_ACCESS_TIME_TAG = "lastAccessTime";
    private static final String CONFIGURATION_TAG = "configuration";
    private static final String GATE_ID_TAG = "gateId";
    private static final String ITEM_CONFIGURATION_TAG = "createfaremod.gateConfiguration";
    private static final int PROCESSING_TICKS = 10;
    private static final int OPEN_TICKS = 30;
    private static final int FAILURE_TICKS = 40;

    private boolean gateOpen;
    private long lastAccessTime;
    private GateConfiguration configuration = GateConfiguration.empty();
    private UUID gateId = UUID.randomUUID();
    private UUID pendingPlayer;
    private InteractionHand pendingHand;
    private int processingTicks;
    private int resetTicks;
    private int secondFailureSoundTicks;

    public FareGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARE_GATE.get(), pos, state);
        gateOpen = state.getValue(FareGateBlock.OPEN);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FareGateBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (blockEntity.gateOpen != state.getValue(FareGateBlock.OPEN)) {
            blockEntity.gateOpen = state.getValue(FareGateBlock.OPEN);
            blockEntity.setChanged();
        }
        if (blockEntity.processingTicks > 0 && --blockEntity.processingTicks == 0) {
            blockEntity.completePendingUse(serverLevel);
        }
        if (blockEntity.secondFailureSoundTicks > 0 && --blockEntity.secondFailureSoundTicks == 0) {
            blockEntity.playSound(ModSounds.GATE_FAILURE.get(), 0.85F, 0.9F);
        }
        if (blockEntity.resetTicks > 0 && --blockEntity.resetTicks == 0) {
            if (blockEntity.gateOpen) {
                blockEntity.setGateOpen(false, null);
            }
            blockEntity.setStatus(blockEntity.idleStatus());
        }
        if (blockEntity.processingTicks == 0
                && blockEntity.resetTicks == 0
                && blockEntity.pendingPlayer == null
                && state.getValue(FareGateBlock.STATUS) != blockEntity.idleStatus()) {
            blockEntity.setStatus(blockEntity.idleStatus());
        }
    }

    public void queueCardUse(ServerPlayer player, InteractionHand hand) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (processingTicks > 0 || pendingPlayer != null) {
            rejectCard(player, "Gate is already processing another card.", true);
            return;
        }
        Optional<String> validation = configuration.validate();
        if (validation.isPresent()) {
            rejectMaintenance(player, "Gate under maintenance: " + validation.get());
            return;
        }
        if (configuration.maintenanceMode() && !canConfigure(player)) {
            rejectMaintenance(player, "Gate under maintenance");
            return;
        }
        pendingPlayer = player.getUUID();
        pendingHand = hand;
        processingTicks = PROCESSING_TICKS;
        resetTicks = 0;
        setGateOpen(false, null);
        setStatus(GateStatus.PROCESSING);
    }

    private void completePendingUse(ServerLevel serverLevel) {
        UUID playerId = pendingPlayer;
        InteractionHand hand = pendingHand;
        pendingPlayer = null;
        pendingHand = null;
        if (playerId == null || hand == null) {
            setStatus(GateStatus.NORMAL);
            return;
        }

        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
        if (player == null || player.distanceToSqr(worldPosition.getCenter()) > 64.0D) {
            setStatus(GateStatus.FAILURE);
            resetTicks = FAILURE_TICKS;
            return;
        }
        ItemStack cardStack = player.getItemInHand(hand);
        if (!cardStack.is(ModItems.IC_CARD.get())) {
            rejectCard(player, "Keep the IC card on the reader until processing finishes.", true);
            return;
        }

        CardLedgerSavedData ledger = CardLedgerSavedData.get(serverLevel.getServer());
        CardLedgerSavedData.Authentication authentication = ledger.authenticateAndAdvance(cardStack, player);
        if (!authentication.valid()) {
            rejectCard(player, authentication.message(), true);
            return;
        }

        boolean exit = configuration.gateMode() == GateMode.EXIT
                || (configuration.gateMode() == GateMode.BIDIRECTIONAL && authentication.record().hasEntry());
        if (exit) {
            processExit(player, cardStack, ledger, authentication);
        } else {
            processEntry(player, cardStack, ledger, authentication);
        }
    }

    private void processEntry(
            ServerPlayer player,
            ItemStack cardStack,
            CardLedgerSavedData ledger,
            CardLedgerSavedData.Authentication authentication
    ) {
        if (authentication.record().hasEntry()) {
            rejectCard(player, "This IC card already has an active journey.", true);
            log(
                    player,
                    authentication.record().entryStationName(),
                    configuration.stationName(),
                    0L,
                    "ENTRY_ALREADY_ACTIVE",
                    authentication.cardId()
            );
            return;
        }
        ledger.setEntry(
                authentication.cardId(),
                cardStack,
                configuration.stationId(),
                configuration.stationName(),
                gateId,
                System.currentTimeMillis(),
                configuration.lineId()
        );
        log(player, configuration.stationName(), "", 0L, "ENTRY_SUCCESS", authentication.cardId());
        succeed(player, "Entry recorded: " + configuration.stationName(), 0L);
    }

    private void processExit(
            ServerPlayer player,
            ItemStack cardStack,
            CardLedgerSavedData ledger,
            CardLedgerSavedData.Authentication authentication
    ) {
        CardLedgerSavedData.CardRecord card = authentication.record();
        if (!card.hasEntry()) {
            rejectCard(player, "No entry station is recorded on this IC card.", true);
            log(player, "", configuration.stationName(), 0L, "NO_ENTRY", authentication.cardId());
            return;
        }
        if (!card.lineId().equals(configuration.lineId())) {
            rejectCard(player, "The entry and exit lines do not match.", true);
            log(
                    player,
                    card.entryStationName(),
                    configuration.stationName(),
                    0L,
                    "LINE_MISMATCH",
                    authentication.cardId()
            );
            return;
        }

        FareManager.FareResult fareResult = FareManager.calculate(
                player.getServer(),
                configuration.fareTableId(),
                configuration.lineId(),
                card.entryStationId(),
                configuration.stationId()
        );
        if (!fareResult.success()) {
            paymentFailed(player, fareResult.message(), 0L);
            log(
                    player,
                    card.entryStationName(),
                    configuration.stationName(),
                    0L,
                    "FARE_MISSING",
                    authentication.cardId()
            );
            return;
        }
        long fare = fareResult.fare();

        CurrencyAdapter adapter = LightmansCurrencyAdapter.INSTANCE;
        Optional<CurrencyAdapter.Account> account = adapter.resolveAccount(configuration.accountId());
        if (account.isEmpty()) {
            paymentFailed(player, "The configured LC account is unavailable.", fare);
            log(
                    player,
                    card.entryStationName(),
                    configuration.stationName(),
                    fare,
                    "ACCOUNT_UNAVAILABLE",
                    authentication.cardId()
            );
            return;
        }
        CurrencyAdapter.PaymentResult payment = adapter.charge(player, account.get(), fare);
        if (!payment.success()) {
            paymentFailed(player, payment.message(), fare);
            log(
                    player,
                    card.entryStationName(),
                    configuration.stationName(),
                    fare,
                    "PAYMENT_FAILED",
                    authentication.cardId()
            );
            return;
        }

        ledger.clearEntry(authentication.cardId(), cardStack);
        log(
                player,
                card.entryStationName(),
                configuration.stationName(),
                fare,
                "PAYMENT_SUCCESS",
                authentication.cardId()
        );
        succeed(player, "Fare paid: " + fare + " LC", fare);
    }

    private void succeed(ServerPlayer player, String message, long fare) {
        lastAccessTime = level == null ? 0L : level.getGameTime();
        setStatus(GateStatus.SUCCESS);
        playSound(ModSounds.GATE_SUCCESS.get(), 0.9F, 1.15F);
        setGateOpen(true, player);
        resetTicks = OPEN_TICKS;
        PacketDistributor.sendToPlayer(player, new TransactionResultPayload(true, message, fare));
    }

    private void paymentFailed(ServerPlayer player, String reason, long fare) {
        setGateOpen(false, null);
        setStatus(GateStatus.FAILURE);
        resetTicks = FAILURE_TICKS;
        playFailureSound();
        PacketDistributor.sendToPlayer(player, new PaymentFailurePayload(reason, fare));
    }

    public void rejectNonCard(ServerPlayer player) {
        rejectCard(player, "Please use an IC Card.", true);
    }

    private void rejectCard(ServerPlayer player, String reason, boolean sound) {
        setGateOpen(false, null);
        setStatus(GateStatus.FAILURE);
        resetTicks = FAILURE_TICKS;
        if (sound) {
            playFailureSound();
        }
        player.displayClientMessage(Component.literal(reason), true);
    }

    private void rejectMaintenance(ServerPlayer player, String reason) {
        setGateOpen(false, null);
        setStatus(GateStatus.MAINTENANCE);
        resetTicks = 0;
        playFailureSound();
        player.displayClientMessage(Component.literal(reason), true);
    }

    private void playFailureSound() {
        playSound(ModSounds.GATE_FAILURE.get(), 0.85F, 0.9F);
        secondFailureSoundTicks = 4;
    }

    private void log(
            ServerPlayer player,
            String stationIn,
            String stationOut,
            long fare,
            String result,
            UUID cardId
    ) {
        TransactionLogSavedData.get(player.getServer()).append(new TransactionLogSavedData.Transaction(
                System.currentTimeMillis(),
                player.getUUID(),
                player.getGameProfile().getName(),
                stationIn,
                stationOut,
                fare,
                configuration.accountId(),
                result,
                gateId,
                cardId
        ));
    }

    public void toggleGate(Player player) {
        if (level == null || level.isClientSide || !canConfigure(player)) {
            return;
        }
        setGateOpen(!gateOpen, player);
        setStatus(gateOpen ? GateStatus.SUCCESS : GateStatus.NORMAL);
        resetTicks = gateOpen ? OPEN_TICKS : 0;
    }

    public void openOperatorMenu(ServerPlayer player) {
        if (canConfigure(player)) {
            FareGateMenu.open(player, this);
        } else {
            player.displayClientMessage(Component.translatable("message.createfaremod.not_gate_owner"), true);
        }
    }

    public void updateConfiguration(ServerPlayer player, GateConfiguration requested) {
        if (!canConfigure(player)
                || player.distanceToSqr(worldPosition.getCenter()) > 64.0D) {
            player.displayClientMessage(Component.translatable("message.createfaremod.not_gate_owner"), true);
            return;
        }
        GateConfiguration secured = requested.withOwner(configuration.ownerUuid());
        Optional<String> validation = secured.validate();
        if (validation.isPresent()) {
            player.displayClientMessage(Component.literal(validation.get()), false);
            return;
        }
        Optional<CurrencyAdapter.Account> account =
                LightmansCurrencyAdapter.INSTANCE.resolveAccount(secured.accountId());
        if (account.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.createfaremod.invalid_lc_account"), false);
            return;
        }
        applyConfiguration(secured.withAccountName(account.get().name()));
        player.displayClientMessage(Component.translatable("message.createfaremod.configuration_saved"), true);
    }

    public boolean canConfigure(Player player) {
        return player instanceof ServerPlayer serverPlayer
                && (configuration.ownerUuid() == null
                || configuration.ownerUuid().equals(player.getUUID())
                || serverPlayer.hasPermissions(2));
    }

    public GateConfiguration getConfiguration() {
        return configuration;
    }

    public void applyConfiguration(GateConfiguration configuration) {
        this.configuration = configuration == null ? GateConfiguration.empty() : configuration;
        setStatus(idleStatus());
        setChangedAndSync();
    }

    public void setOwnerIfAbsent(UUID owner) {
        if (configuration.ownerUuid() == null && owner != null) {
            configuration = configuration.withOwner(owner);
            setChangedAndSync();
        }
    }

    public boolean isGateOpen() {
        return gateOpen;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public UUID getGateId() {
        return gateId;
    }

    public String getStationId() {
        return configuration.stationId();
    }

    public void setStationId(String stationId) {
        configuration = new GateConfiguration(
                stationId,
                configuration.stationName(),
                configuration.lineId(),
                configuration.accountId(),
                configuration.accountName(),
                configuration.gateMode(),
                configuration.fareTableId(),
                configuration.fareTable(),
                configuration.ownerUuid(),
                configuration.maintenanceMode()
        );
        setChangedAndSync();
    }

    public void saveConfigurationToItem(ItemStack stack, HolderLookup.Provider registries) {
        saveToItem(stack, registries);
    }

    private void setGateOpen(boolean open, Player player) {
        if (level == null || level.isClientSide || gateOpen == open) {
            return;
        }
        BlockState previousState = getBlockState();
        gateOpen = open;
        BlockState updatedState = previousState.setValue(FareGateBlock.OPEN, open);
        level.setBlock(worldPosition, updatedState, Block.UPDATE_ALL);
        setChanged();
        level.sendBlockUpdated(worldPosition, previousState, updatedState, Block.UPDATE_CLIENTS);
        playSound(open ? ModSounds.GATE_OPEN.get() : ModSounds.GATE_CLOSE.get(), 0.8F, 1.0F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, worldPosition);
    }

    private void setStatus(GateStatus status) {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (state.getValue(FareGateBlock.STATUS) != status) {
            level.setBlock(worldPosition, state.setValue(FareGateBlock.STATUS, status), Block.UPDATE_ALL);
        }
    }

    private GateStatus idleStatus() {
        return configuration.maintenanceMode() || configuration.validate().isPresent()
                ? GateStatus.MAINTENANCE
                : GateStatus.NORMAL;
    }

    private void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        gateOpen = tag.getBoolean(GATE_OPEN_TAG);
        lastAccessTime = tag.getLong(LAST_ACCESS_TIME_TAG);
        gateId = tag.hasUUID(GATE_ID_TAG) ? tag.getUUID(GATE_ID_TAG) : UUID.randomUUID();
        configuration = tag.contains(CONFIGURATION_TAG, Tag.TAG_COMPOUND)
                ? GateConfiguration.load(tag.getCompound(CONFIGURATION_TAG))
                : GateConfiguration.empty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(GATE_OPEN_TAG, gateOpen);
        tag.putLong(LAST_ACCESS_TIME_TAG, lastAccessTime);
        tag.putUUID(GATE_ID_TAG, gateId);
        tag.put(CONFIGURATION_TAG, configuration.save());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        CompoundTag customData = new CompoundTag();
        customData.put(ITEM_CONFIGURATION_TAG, configuration.save());
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        CustomData customData = input.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(ITEM_CONFIGURATION_TAG, Tag.TAG_COMPOUND)) {
                configuration = GateConfiguration.load(tag.getCompound(ITEM_CONFIGURATION_TAG));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
