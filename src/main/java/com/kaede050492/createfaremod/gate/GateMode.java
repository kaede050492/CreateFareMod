package com.kaede050492.createfaremod.gate;

import java.util.Locale;

public enum GateMode {
    ENTRY,
    EXIT,
    BIDIRECTIONAL;

    public static GateMode parse(String value) {
        if (value == null) {
            return BIDIRECTIONAL;
        }
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BIDIRECTIONAL;
        }
    }
}
