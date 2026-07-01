package com.kaede050492.createfaremod.gametest;

import com.kaede050492.createfaremod.CreateFareMod;
import com.kaede050492.createfaremod.currency.CurrencyAdapter;
import com.kaede050492.createfaremod.currency.LightmansCurrencyAdapter;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateFareMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CurrencyAdapterGameTests {
    private CurrencyAdapterGameTests() {
    }

    @GameTest(template = "fare_gate_empty")
    public static void handlesPaymentSuccessAndFailure(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        CurrencyAdapter adapter = LightmansCurrencyAdapter.INSTANCE;
        Optional<CurrencyAdapter.Account> account =
                adapter.resolveAccount("player:" + player.getUUID());
        helper.assertTrue(account.isPresent(), "Player LC account should resolve");

        CurrencyAdapter.PaymentResult freeFare = adapter.charge(player, account.orElseThrow(), 0L);
        CurrencyAdapter.PaymentResult unaffordableFare = adapter.charge(player, account.orElseThrow(), 1L);

        helper.assertTrue(freeFare.success(), "Zero fare should succeed");
        helper.assertFalse(unaffordableFare.success(), "Fare without an LC wallet should fail");
        helper.succeed();
    }
}
