package org.xeb.xeb.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class DeathMessageHelperTest {

    private String invokeGetVagueDescription(String id) throws Exception {
        Method method = DeathMessageHelper.class.getDeclaredMethod("getVagueDescription", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, id);
    }

    @Test
    public void testKnownBuffDescriptions() throws Exception {
        assertEquals("Deals damage back to attackers.", invokeGetVagueDescription("spiky"));
        assertEquals("Emits a sonic shockwave when damaged.", invokeGetVagueDescription("reactive"));
        assertEquals("Grants increased attack damage.", invokeGetVagueDescription("damaging"));
        assertEquals("Grants increased armor.", invokeGetVagueDescription("tough"));
        assertEquals("Moves significantly faster.", invokeGetVagueDescription("speedy"));
        assertEquals("Immune to fire, leaves a trail of fire.", invokeGetVagueDescription("flaming"));
        assertEquals("Reflects projectiles back to shooter.", invokeGetVagueDescription("mirror"));
        assertEquals("Revives with 50% health once upon death.", invokeGetVagueDescription("undying"));
        assertEquals("Spawns hostile flies when killed.", invokeGetVagueDescription("infested"));
        assertEquals("Spawns with an identical twin with the same buffs.", invokeGetVagueDescription("twin"));
        assertEquals("Immune to all forms of crowd control.", invokeGetVagueDescription("hardy"));
        assertEquals("Tarres attackers, slowing their movement.", invokeGetVagueDescription("sticky"));
    }

    @Test
    public void testUnknownBuffReturnsDefault() throws Exception {
        assertEquals("Grants mysterious powers.", invokeGetVagueDescription("nonexistent_buff"));
        assertEquals("Grants mysterious powers.", invokeGetVagueDescription(""));
        assertEquals("Grants mysterious powers.", invokeGetVagueDescription("totally_made_up"));
    }

    @Test
    public void testAllDefinedBuffIdsHaveDescriptions() throws Exception {
        String[] knownIds = {
            "spiky", "reactive", "damaging", "tough", "shielded", "protected",
            "speedy", "flaming", "creepy", "lucky", "static", "bouncy",
            "mirror", "resonant", "undying", "healthy", "sandy", "infested",
            "absorbent", "depressing", "slightly_depressing", "evolving",
            "plow", "mega", "mad", "twin", "hardy", "sticky"
        };

        for (String id : knownIds) {
            String desc = invokeGetVagueDescription(id);
            assertNotNull(desc, "Description should not be null for " + id);
            assertNotEquals("Grants mysterious powers.", desc,
                "Buff '" + id + "' should have a specific description, not the default");
        }
    }
}
