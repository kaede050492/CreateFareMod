package com.kaede050492.createfaremod.currency;

import io.github.lightman314.lightmanscurrency.api.capability.money.IMoneyHandler;
import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.bank.BankAPI;
import io.github.lightman314.lightmanscurrency.api.money.bank.IBankAccount;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.BankReference;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.builtin.PlayerBankReference;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.builtin.TeamBankReference;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.builtin.CoinValue;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class LightmansCurrencyAdapter implements CurrencyAdapter {
    public static final LightmansCurrencyAdapter INSTANCE = new LightmansCurrencyAdapter();
    private static final String DEFAULT_COIN_CHAIN = "main";

    private LightmansCurrencyAdapter() {
    }

    @Override
    public Optional<Account> resolveAccount(String accountId) {
        if (accountId == null) {
            return Optional.empty();
        }
        String normalized = accountId.trim().toLowerCase(Locale.ROOT);
        BankReference reference;
        try {
            if (normalized.startsWith("player:")) {
                reference = PlayerBankReference.of(UUID.fromString(normalized.substring("player:".length())));
            } else if (normalized.startsWith("team:")) {
                reference = TeamBankReference.of(Long.parseLong(normalized.substring("team:".length())));
            } else {
                return Optional.empty();
            }
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }

        IBankAccount bankAccount = reference.get();
        if (bankAccount == null) {
            return Optional.empty();
        }
        return Optional.of(new Account(normalized, bankAccount.getName().getString(), bankAccount));
    }

    @Override
    public PaymentResult charge(ServerPlayer player, Account account, long fare) {
        if (fare < 0 || !(account.handle() instanceof IBankAccount destination)) {
            return PaymentResult.failure("Invalid fare or destination account.");
        }
        if (fare == 0) {
            return PaymentResult.approved();
        }

        MoneyValue value = CoinValue.fromNumber(DEFAULT_COIN_CHAIN, fare);
        if (value.isEmpty() || value.isInvalid()) {
            return PaymentResult.failure("The LC main coin chain cannot represent this fare.");
        }

        IMoneyHandler payer = MoneyAPI.getApi().GetPlayersMoneyHandler(player);
        MoneyValue simulatedRemainder = payer.extractMoney(value, true);
        if (!simulatedRemainder.isEmpty()) {
            return PaymentResult.failure("Insufficient balance.");
        }

        MoneyValue remainder = payer.extractMoney(value, false);
        if (!remainder.isEmpty()) {
            return PaymentResult.failure("Balance changed before payment completed.");
        }

        if (!BankAPI.getApi().BankDepositFromServer(destination, value)) {
            payer.insertMoney(value, false);
            return PaymentResult.failure("The destination account rejected the payment.");
        }
        return PaymentResult.approved();
    }
}
