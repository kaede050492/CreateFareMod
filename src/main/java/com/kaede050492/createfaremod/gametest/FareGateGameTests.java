package com.kaede050492.createfaremod.gametest;

import com.mojang.authlib.GameProfile;
import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.block.entity.FareGateBlockEntity;
import com.kaede050492.createfaremod.data.CardLedgerSavedData;
import com.kaede050492.createfaremod.gate.GateConfiguration;
import com.kaede050492.createfaremod.gate.GateMode;
import com.kaede050492.createfaremod.registry.ModBlocks;
import com.kaede050492.createfaremod.registry.ModItems;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateFareMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FareGateGameTests {
    private static final String TEMPLATE = "fare_gate_empty";

    private FareGateGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void copiesCompleteConfiguration(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 0);
        BlockPos targetPos = new BlockPos(1, 1, 0);
        helper.setBlock(sourcePos, ModBlocks.FARE_GATE.get());
        helper.setBlock(targetPos, ModBlocks.FARE_GATE.get());
        FareGateBlockEntity source = helper.getBlockEntity(sourcePos);
        FareGateBlockEntity target = helper.getBlockEntity(targetPos);
        GateConfiguration expected = sampleConfiguration(UUID.randomUUID());

        source.applyConfiguration(expected);
        target.applyConfiguration(source.getConfiguration());

        helper.assertValueEqual(target.getConfiguration(), expected, "Copied gate configuration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void preservesConfigurationWhenBrokenAndPlaced(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 0);
        BlockPos targetPos = new BlockPos(1, 1, 0);
        helper.setBlock(sourcePos, ModBlocks.FARE_GATE.get());
        FareGateBlockEntity source = helper.getBlockEntity(sourcePos);
        GateConfiguration expected = sampleConfiguration(UUID.randomUUID());
        source.applyConfiguration(expected);

        ItemStack pickedGate = Block.getDrops(
                source.getBlockState(),
                helper.getLevel(),
                source.getBlockPos(),
                source
        ).stream().findFirst().orElseThrow();
        helper.setBlock(targetPos, ModBlocks.FARE_GATE.get());
        FareGateBlockEntity placed = helper.getBlockEntity(targetPos);
        placed.applyComponentsFromItemStack(pickedGate);

        helper.assertValueEqual(placed.getConfiguration(), expected, "Block item configuration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void restoresConfigurationAfterReload(GameTestHelper helper) {
        BlockPos gatePos = new BlockPos(0, 1, 0);
        helper.setBlock(gatePos, ModBlocks.FARE_GATE.get());
        FareGateBlockEntity source = helper.getBlockEntity(gatePos);
        GateConfiguration expected = sampleConfiguration(UUID.randomUUID());
        source.applyConfiguration(expected);
        CompoundTag saved = source.saveWithFullMetadata(helper.getLevel().registryAccess());

        helper.setBlock(gatePos, ModBlocks.FARE_GATE.get());
        FareGateBlockEntity reloaded = helper.getBlockEntity(gatePos);
        reloaded.loadWithComponents(saved, helper.getLevel().registryAccess());

        helper.assertValueEqual(reloaded.getConfiguration(), expected, "Reloaded gate configuration");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectsCopiedIcCard(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        CardLedgerSavedData ledger = CardLedgerSavedData.get(helper.getLevel().getServer());
        ItemStack original = new ItemStack(ModItems.IC_CARD.get());
        CardLedgerSavedData.Authentication registration = ledger.authenticateAndAdvance(original, player);
        helper.assertTrue(registration.valid(), "New card should register");

        ItemStack copied = original.copy();
        CardLedgerSavedData.Authentication originalUse = ledger.authenticateAndAdvance(original, player);
        CardLedgerSavedData.Authentication copiedUse = ledger.authenticateAndAdvance(copied, player);

        helper.assertTrue(originalUse.valid(), "Original card should remain valid");
        helper.assertFalse(copiedUse.valid(), "Copied card should be rejected after counter rotation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectsCardUsedByAnotherPlayer(GameTestHelper helper) {
        ServerPlayer owner = FakePlayerFactory.get(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "FareOwner")
        );
        ServerPlayer otherPlayer = FakePlayerFactory.get(
                helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "FareOther")
        );
        CardLedgerSavedData ledger = CardLedgerSavedData.get(helper.getLevel().getServer());
        ItemStack card = new ItemStack(ModItems.IC_CARD.get());

        helper.assertTrue(ledger.authenticateAndAdvance(card, owner).valid(), "Owner should register card");
        helper.assertFalse(
                ledger.authenticateAndAdvance(card, otherPlayer).valid(),
                "Another player should not use the card"
        );
        helper.succeed();
    }

    private static GateConfiguration sampleConfiguration(UUID owner) {
        return new GateConfiguration(
                "station_a",
                "Central Station",
                "line_1",
                "player:" + owner,
                "Test Account",
                GateMode.EXIT,
                Map.of("station_b", 240L),
                owner
        );
    }
}
