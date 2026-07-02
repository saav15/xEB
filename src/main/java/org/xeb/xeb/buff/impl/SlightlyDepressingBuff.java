package org.xeb.xeb.buff.impl;

import org.xeb.xeb.buff.BuffType;
import org.xeb.xeb.effect.ModEffects;

public class SlightlyDepressingBuff extends AuraDebuffBuff {
    public SlightlyDepressingBuff() {
        super("slightly_depressing", "Slightly Depressing", BuffType.UNIVERSAL, 0x708090, 5.0D,
                2.0D, () -> ModEffects.ALL_STATS_DOWN.get(), 220, 10);
    }
}
