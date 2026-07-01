package com.kaede050492.createfaremod.gate;

import net.minecraft.util.StringRepresentable;

public enum GateStatus implements StringRepresentable {
    NORMAL("normal"),
    PROCESSING("processing"),
    SUCCESS("success"),
    FAILURE("failure"),
    MAINTENANCE("maintenance");

    private final String serializedName;

    GateStatus(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
