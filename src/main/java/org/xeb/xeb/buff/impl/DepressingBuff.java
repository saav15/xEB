package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.effect.ModEffects;

public class DepressingBuff extends AuraDebuffBuff {
    public DepressingBuff() {
        super("depressing", "Depressing", BuffType.UNIVERSAL, 0x2F4F4F, 2.0D,
                10.0D, () -> ModEffects.ALL_STATS_DOWN.get(), 220, 10);
    }
}
