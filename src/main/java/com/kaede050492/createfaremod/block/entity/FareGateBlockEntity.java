package com.kaede050492.createfaremod.block.entity;

import com.kaede050492.createfaremod.block.FareGateBlock;
import com.kaede050492.createfaremod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class FareGateBlockEntity extends BlockEntity {
    private static final String GATE_OPEN_TAG = "gateOpen";
    private static final String LAST_ACCESS_TIME_TAG = "lastAccessTime";
    private static final String STATION_ID_TAG = "stationId";

    private boolean gateOpen;
    private long lastAccessTime;
    private String stationId = "";

    public FareGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARE_GATE.get(), pos, state);
        gateOpen = state.getValue(FareGateBlock.OPEN);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FareGateBlockEntity blockEntity) {
        boolean stateOpen = state.getValue(FareGateBlock.OPEN);
        if (blockEntity.gateOpen != stateOpen) {
            blockEntity.gateOpen = stateOpen;
            blockEntity.setChanged();
        }
    }

    public void toggleGate(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState previousState = getBlockState();
        gateOpen = !gateOpen;
        lastAccessTime = level.getGameTime();
        BlockState updatedState = previousState.setValue(FareGateBlock.OPEN, gateOpen);

        level.setBlock(worldPosition, updatedState, Block.UPDATE_ALL);
        setChanged();
        level.sendBlockUpdated(worldPosition, previousState, updatedState, Block.UPDATE_CLIENTS);
        level.playSound(
                null,
                worldPosition,
                gateOpen ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS,
                0.8F,
                1.0F
        );
        level.gameEvent(player, gateOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, worldPosition);
    }

    public boolean isGateOpen() {
        return gateOpen;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId == null ? "" : stationId;
        setChangedAndSync();
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
        stationId = tag.getString(STATION_ID_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean(GATE_OPEN_TAG, gateOpen);
        tag.putLong(LAST_ACCESS_TIME_TAG, lastAccessTime);
        tag.putString(STATION_ID_TAG, stationId);
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
