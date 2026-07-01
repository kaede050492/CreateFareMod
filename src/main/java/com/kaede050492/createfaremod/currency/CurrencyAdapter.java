package com.kaede050492.createfaremod.currency;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public interface CurrencyAdapter {
    Optional<Account> resolveAccount(String accountId);

    PaymentResult charge(ServerPlayer player, Account account, long fare);

    record Account(String id, String name, Object handle) {
    }

    record PaymentResult(boolean success, String message) {
        public static PaymentResult approved() {
            return new PaymentResult(true, "");
        }

        public static PaymentResult failure(String message) {
            return new PaymentResult(false, message);
        }
    }
}
