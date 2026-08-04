package org.xeb.xeb.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import org.xeb.xeb.item.ModItems;
import org.xeb.xeb.medallion.MedallionType;
import org.xeb.xeb.buff.EliteBuff;
import org.xeb.xeb.buff.EliteBuffRegistry;
import org.xeb.xeb.render.MedallionRenderLayer;
import org.xeb.xeb.extremeburst.ExtremeBurstRegistry;
import org.xeb.xeb.event.ModTooltipHandler;

import java.util.ArrayList;
import java.util.List;

public class EnigmaBiosScreen extends Screen {
    private final int guiWidth = 360;
    private final int guiHeight = 260;
    private int leftPos;
    private int topPos;

    private int activeTab = 0; // 0: Analyzer, 1: Bestiary, 2: Logs, 3: Mastery, 4: Scanner
    private ItemStack analyzedStack = ItemStack.EMPTY;
    private int selectedAbilityIndex = 0;

    private int selectedBestiaryIndex = 0;
    private int selectedBestiaryTierIndex = 0; // 0: BRONZE, 1: SILVER, 2: GOLD
    private int selectedLogIndex = 0;

    private final List<LogEntry> logs = new ArrayList<>();

    // Scroll Amounts
    private float tabScrollAmount = 0.0F;
    private float contentScrollAmount = 0.0F;
    private float analyzerScrollAmount = 0.0F;
    private float headerLoreScrollAmount = 0.0F;
    private float bestiaryListScrollAmount = 0.0F;
    private float bestiaryDetailsScrollAmount = 0.0F;
    private float logListScrollAmount = 0.0F;
    private float logDetailsScrollAmount = 0.0F;
    private float masteryScrollAmount = 0.0F;
    private float scannerScrollAmount = 0.0F;

    // Last Scroll Times for Auto-Hiding Scrollbars
    private long lastTabScrollTime = 0L;
    private long lastAnalyzerScrollTime = 0L;
    private long lastHeaderLoreScrollTime = 0L;
    private long lastBestiaryListScrollTime = 0L;
    private long lastBestiaryDetailsScrollTime = 0L;
    private long lastLogListScrollTime = 0L;
    private long lastLogDetailsScrollTime = 0L;
    private long lastLogScrollTime = 0L;
    private long lastMasteryScrollTime = 0L;
    private long lastScannerScrollTime = 0L;

    // Mouse Dragging States for ALL Scrollbars
    private boolean isDraggingTabScroll = false;
    private boolean isDraggingAnalyzerScroll = false;
    private boolean isDraggingHeaderLoreScroll = false;
    private boolean isDraggingBestiaryListScroll = false;
    private boolean isDraggingBestiaryDetailsScroll = false;
    private boolean isDraggingLogListScroll = false;
    private boolean isDraggingLogDetailsScroll = false;
    private boolean isDraggingLogScroll = false;
    private boolean isDraggingMasteryScroll = false;
    private boolean isDraggingScannerScroll = false;
    private double dragStartY = 0.0;
    private float dragStartScroll = 0.0F;

    // Unknown item warning flash states
    private boolean lastAnalyzedUnknown = false;
    private long lastAnalyzedTime = 0L;
    private int unknownTextIndex = 0;

    public EnigmaBiosScreen() {
        super(Component.literal("Enigma Bios"));
        initLogs();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void initLogs() {
        logs.add(new LogEntry("gui.xeb.enigma_bios.log1.title", "gui.xeb.enigma_bios.log1.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log2.title", "gui.xeb.enigma_bios.log2.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log3.title", "gui.xeb.enigma_bios.log3.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log4.title", "gui.xeb.enigma_bios.log4.content"));
        logs.add(new LogEntry("gui.xeb.enigma_bios.log5.title", "gui.xeb.enigma_bios.log5.content"));
    }

    private String translate(String key) {
        if (key == null) return "";
        String str = Component.translatable(key).getString();
        if (str.equals(key)) {
            return switch (key) {
                case "gui.xeb.enigma_bios.title" -> "ENIGMA BIOS v1.0";
                case "gui.xeb.enigma_bios.status" -> "SISTEMA ACTIVO";
                case "gui.xeb.enigma_bios.tab.analyzer" -> "Analizador";
                case "gui.xeb.enigma_bios.tab.bestiary" -> "Bestiario";
                case "gui.xeb.enigma_bios.tab.logs" -> "Bitácoras";
                case "gui.xeb.enigma_bios.tab.mastery" -> "Maestría";
                case "gui.xeb.enigma_bios.tab.scanner" -> "Escáner";
                case "gui.xeb.enigma_bios.tab.log" -> "Bitácora";
                case "gui.xeb.enigma_bios.logs.title" -> "BITÁCORAS DE INVESTIGACIÓN";
                case "gui.xeb.enigma_bios.mastery.title_panel" -> "SISTEMA DE MAESTRÍA ÉLITE";
                case "gui.xeb.enigma_bios.scanner.title_panel" -> "SISTEMA DE ESCÁNER DE DAÑO";
                case "gui.xeb.enigma_bios.analyzer.empty" -> "No hay ningún objeto colocado en el analizador.";
                case "gui.xeb.enigma_bios.hud_pos" -> "Ajustar HUD";
                case "gui.xeb.enigma_bios.bestiary.kills" -> "Bajas: ";
                case "gui.xeb.enigma_bios.log.locked.title" -> "Bitácora [BLOQUEADA]";
                case "gui.xeb.enigma_bios.log.locked.desc" -> "Esta bitácora de investigación requiere desbloqueo previa exploración de reliquias élite.";
                case "item.xeb.golden_flower.enigma_lore" -> "Una extraña flor dorada hallada en suelo subterráneo, susurrando promesas de amistad y control absoluto. Se alimenta de determinación, proyectando ráfagas de pétalos engañosos, campos florales teledirigidos e ilusiones espectrales siniestras en una danza ineludible.";
                case "item.xeb.doomfist.enigma_lore" -> "Un guantelete legendario heredado entre señores de la guerra, convencidos de que solo a través del conflicto evoluciona la humanidad. Canaliza cargas cinéticas que estrellan enemigos contra estructuras y ganchos ascendentes que ejecutan titanes masivos.";
                case "item.xeb.doomfist_v2.enigma_lore" -> "Un exopuño cibernético mejorado para el dominio imparable en el frente de batalla. Reforzado con condensadores de bloqueo cinético, cañones de palma de plasma y ganchos que fracturan la tierra y ralentizan las fuerzas enemigas.";
                case "item.xeb.optic_blast.enigma_lore" -> "Un visor de Rubí-Cuarzo diseñado para contener y redirigir la incontrolable energía dimensional de la mirada de su portador. Canaliza rayos láser cinéticos continuos, ráfagas desintegradoras de empalme genético y vientos ciclónicos.";
                case "item.xeb.holy_duality_blade.enigma_lore" -> "Un antiguo mandoble forjado en el límite donde la luz divina encuentra la sombra umbría. Blandido por campeones del equilibrio cósmico, manifiesta barreras impenetrables de luz, estocadas sagradas que rompen armaduras y tajos duales de juicio.";
                case "item.xeb.mecha_overdrive.enigma_lore" -> "El núcleo táctico de sobrecarga de una forma de vida mecánica de alta velocidad construida para rivalizar con leyendas celestes. Equipado con taladros propulsores de altas revoluciones, cañones Vulcan Gatling y misiles Spindash teledirigidos.";
                case "item.xeb.broken_diamond.enigma_lore" -> "Una reliquia fragmentada que contiene el espíritu de un guerrero feroz y compasivo que se niega a dejar algo roto. Manifiesta un Stand espectral que desata ráfagas de 60 puñetazos DORA DORA, patadas cinéticas y la capacidad de fusionar enemigos en piedra sólida.";
                case "item.xeb.the_tears.enigma_lore" -> "Una esfera de cristal nacida del llanto incesante de un niño atrapado en oscuros sótanos. Convierte la tristeza en lágrimas espaciales explosivas, imbuyendo proyectiles con afinidades elementales y otorgando invisibilidad en las sombras.";
                case "item.xeb.smart_halberd.enigma_lore" -> "Una alabarda táctica autónoma equipada con ópticas avanzadas de adquisición de objetivos. Guiada por un núcleo de IA interno, realiza tajos de precisión y embestidas teledirigidas que rastrean sin fallo a los objetivos enemistados.";
                case "item.xeb.omega_flowery.enigma_lore" -> "Reliquia Definitiva de Curios: Otorga la capacidad de activar el Extreme Burst de Omega Flowey al estar equipada junto a The Golden Flower.";
                case "item.xeb.dogma.enigma_lore" -> "Reliquia Definitiva de Curios: Otorga la capacidad de activar el Extreme Burst de Dogma al estar equipada junto a The Tears.";
                case "item.xeb.quantum_cat_barrage.enigma_lore" -> "Reliquia Definitiva Universal de Curios: Otorga la capacidad de desatar un Bombardeo Felino Cuántico devastador.";
                case "item.xeb.meteor_strike.enigma_lore" -> "Reliquia Definitiva de Curios: Otorga la capacidad de activar el Extreme Burst de Meteor Strike al estar equipada junto a Doomfist v1 o Doomfist v2.";
                case "item.xeb.full_aperture_supernova.enigma_lore" -> "Reliquia Definitiva de Curios: Otorga la capacidad de activar el Extreme Burst de Full-Aperture Supernova al estar equipada junto a Optic Blast.";
                case "item.xeb.judgement_cut.enigma_lore" -> "Reliquia Definitiva Universal de Curios: Otorga la capacidad de desatar Judgement Cut End, ralentizando el espacio-tiempo y cortando dimensionalmente el área.";
                case "item.xeb.sovereign_arsenal.enigma_lore" -> "Reliquia Definitiva Universal de Curios: Otorga la capacidad de abrir los portales del Arsenal Soberano e invocar espadas espaciales empaladoras.";
                default -> key;
            };
        }
        return str;
    }

    private void renderAutoHidingScrollbar(GuiGraphics g, int scrollX, int scrollY, int scrollW, int scrollH, float scrollAmount, float maxScroll, float totalHeight, long lastScrollTime, boolean isDragging) {
        if (maxScroll <= 0) return;
        long elapsed = System.currentTimeMillis() - lastScrollTime;
        if (!isDragging && elapsed > 1000L) return; // Hide completely after 1 second of inactivity!

        float fade = 1.0F;
        if (!isDragging && elapsed > 700L) {
            fade = 1.0F - (elapsed - 700L) / 300.0F;
        }
        fade = Mth.clamp(fade, 0.0F, 1.0F);

        int alpha = (int) (255 * fade);
        if (alpha <= 0) return;

        int trackCol = (alpha / 4 << 24) | 0x00FFCC;
        int thumbCol = (alpha << 24) | (isDragging ? 0x00FFFF : 0x00FFCC);

        g.fill(scrollX, scrollY, scrollX + scrollW, scrollY + scrollH, trackCol);
        int thumbH = Math.max(8, (int) ((float) scrollH * scrollH / totalHeight));
        int thumbY = scrollY + (int) ((float) scrollAmount * (scrollH - thumbH) / maxScroll);
        thumbY = Mth.clamp(thumbY, scrollY, scrollY + scrollH - thumbH);
        g.fill(scrollX, thumbY, scrollX + scrollW, thumbY + thumbH, thumbCol);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.leftPos = (this.width - this.guiWidth) / 2;
        this.topPos = (this.height - this.guiHeight) / 2;

        this.renderBackground(g);

        int borderColor = 0xFF00FFCC;
        boolean permanight = org.xeb.xeb.client.PermanightClientHandler.isPermanightActive();

        long elapsed = System.currentTimeMillis() - this.lastAnalyzedTime;
        boolean flashing = this.lastAnalyzedUnknown && (elapsed < 800L);
        if (flashing) {
            float flash = (float) Math.sin(elapsed * 0.02D);
            if (flash > 0) {
                borderColor = 0xFFFF3333;
            }
        }

        // Fondo Principal
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, 0xEE08111E);

        // Permanight: gradiente morado difuminado desde abajo y rojo sangre desde arriba hacia el centro
        if (permanight) {
            // Gradiente morado (desde abajo)
            g.fill(this.leftPos + 2, this.topPos + this.guiHeight - 100, this.leftPos + this.guiWidth - 2, this.topPos + this.guiHeight - 70, 0x0F3B005A);
            g.fill(this.leftPos + 2, this.topPos + this.guiHeight - 70,  this.leftPos + this.guiWidth - 2, this.topPos + this.guiHeight - 40, 0x1F2B0072);
            g.fill(this.leftPos + 2, this.topPos + this.guiHeight - 40,  this.leftPos + this.guiWidth - 2, this.topPos + this.guiHeight - 2,  0x3A3B0099);

            // Gradiente rojo sangre (desde arriba hacia enmedio)
            g.fill(this.leftPos + 2, this.topPos + 2,  this.leftPos + this.guiWidth - 2, this.topPos + 35, 0x3A7A000D);
            g.fill(this.leftPos + 2, this.topPos + 35, this.leftPos + this.guiWidth - 2, this.topPos + 65, 0x1F5C000B);
            g.fill(this.leftPos + 2, this.topPos + 65, this.leftPos + this.guiWidth - 2, this.topPos + 95, 0x0F3A0007);
        }

        // Scanlines Sci-Fi
        renderFuturisticBackgroundScanlines(g, borderColor, permanight);

        // Marco Exterior
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 2, borderColor);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 2, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos + this.guiWidth - 2, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);

        // Header Title Bar
        g.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.guiWidth - 4, this.topPos + 16, 0x3300FFCC);
        g.drawString(this.font, "ENIGMA BIOS v1.0", this.leftPos + 8, this.topPos + 6, borderColor, false);

        String statusText = translate("gui.xeb.enigma_bios.status");
        int statusColor = borderColor;

        if (permanight) {
            int ticksLeft = org.xeb.xeb.client.PermanightClientHandler.getTicksRemaining();
            int totalSecs = Math.max(0, ticksLeft / 20);
            int mins = totalSecs / 60;
            int secs = totalSecs % 60;
            String timerStr = String.format("%02d:%02d", mins, secs);

            long cycle = System.currentTimeMillis() % 20000L; // Glitch cycle cada 20 segundos
            if (cycle >= 15000L) { // Durante los últimos 5s de cada ciclo de 20s
                long glitchTime = cycle - 15000L;
                if (glitchTime < 250L || (glitchTime > 2300L && glitchTime < 2550L)) {
                    char glitchChar1 = (char) ('A' + (int)(System.currentTimeMillis() % 26));
                    char glitchChar2 = (char) ('0' + (int)(System.currentTimeMillis() % 10));
                    statusText = "PERMANIGHT " + glitchChar1 + ":" + glitchChar2 + (secs % 10);
                    statusColor = 0xFFFF1E40; // Glitch rojo carmesí
                } else {
                    statusText = "PERMANIGHT " + timerStr;
                    statusColor = 0xFFFF3355; // Rojo carmesí
                }
            }
        }

        g.drawString(this.font, statusText, this.leftPos + this.guiWidth - 8 - this.font.width(statusText), this.topPos + 6, statusColor, false);

        renderTabs(g, mouseX, mouseY);
        renderContent(g, mouseX, mouseY, borderColor, flashing, elapsed);
        renderInventory(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderFuturisticBackgroundScanlines(GuiGraphics g, int borderColor, boolean permanight) {
        long time = System.currentTimeMillis();
        // Permanight: scanlines move at half speed
        long period = permanight ? 16000L : 8000L;
        int scanY = (int) ((time % period) / (period / 100.0F) * (this.guiHeight / 100.0F));

        g.enableScissor(this.leftPos + 2, this.topPos + 2, this.leftPos + this.guiWidth - 2, this.topPos + this.guiHeight - 2);

        for (int offset = 0; offset < this.guiHeight; offset += 36) {
            int lineY = this.topPos + ((scanY + offset) % this.guiHeight);
            g.fill(this.leftPos + 2, lineY, this.leftPos + this.guiWidth - 2, lineY + 1, 0x0E00FFCC);
        }

        for (int x = this.leftPos + 36; x < this.leftPos + this.guiWidth; x += 36) {
            g.fill(x, this.topPos + 2, x + 1, this.topPos + this.guiHeight - 2, 0x0600FFCC);
        }

        g.disableScissor();
    }

    private void renderTabs(GuiGraphics g, int mouseX, int mouseY) {
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;
        int totalTabH = 5 * 22;
        int maxTabScroll = Math.max(0, totalTabH - viewportH);
        this.tabScrollAmount = Mth.clamp(this.tabScrollAmount, 0.0F, maxTabScroll);

        // Barra de scroll de pestañas auto-ocultable en 1s
        renderAutoHidingScrollbar(g, startX + 60, viewportY, 3, viewportH, this.tabScrollAmount, maxTabScroll, totalTabH, this.lastTabScrollTime, this.isDraggingTabScroll);

        g.enableScissor(startX, viewportY, startX + 58, viewportY + viewportH);

        for (int i = 0; i < 5; i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (y + 20 < viewportY || y > viewportY + viewportH) continue;

            boolean isActive = (this.activeTab == i);
            boolean isHovered = mouseX >= startX && mouseX < startX + 58 && mouseY >= y && mouseY < y + 20;

            int bgColor = isActive ? 0xCC00FFCC : (isHovered ? 0x4400FFCC : 0x2200FFCC);
            int textColor = isActive ? 0xFF08111E : (isHovered ? 0xFFFFFFFF : 0xFF00FFCC);

            g.fill(startX, y, startX + 58, y + 20, bgColor);

            String label = switch (i) {
                case 0 -> translate("gui.xeb.enigma_bios.tab.analyzer");
                case 1 -> translate("gui.xeb.enigma_bios.tab.bestiary");
                case 2 -> translate("gui.xeb.enigma_bios.tab.logs");
                case 3 -> translate("gui.xeb.enigma_bios.tab.mastery");
                case 4 -> translate("gui.xeb.enigma_bios.tab.scanner");
                default -> "";
            };

            g.drawString(this.font, label, startX + 4, y + 6, textColor, false);
        }

        g.disableScissor();
    }

    private static int getItemRarityColor(ItemStack stack, int defaultColor) {
        if (stack == null || stack.isEmpty()) return defaultColor;
        Item item = stack.getItem();
        long time = System.currentTimeMillis();
        double phase = (time % 3000L) / 3000.0 * 2.0 * Math.PI; // 50% más lento

        if (ModTooltipHandler.isModWeapon(stack)) {
            if (item == ModItems.SMART_HALBERD.get()) {
                // Aqua Legendario (Pulsante)
                int r = (int) (10 + 10 * Math.sin(phase));
                int g = (int) (225 + 30 * Math.sin(phase));
                int b = (int) (195 + 30 * Math.sin(phase));
                return 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            // Rojo Mítico (Pulsante)
            int r = (int) (210 + 45 * Math.sin(phase));
            int g = (int) (35 + 25 * Math.sin(phase));
            int b = (int) (40 + 20 * Math.sin(phase));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        } else if (ModTooltipHandler.isExtremeBurstCurio(stack)) {
            // Morado Épico (Pulsante)
            int r = (int) (190 + 50 * Math.sin(phase));
            int g = (int) (40 + 30 * Math.sin(phase));
            int b = (int) (215 + 35 * Math.sin(phase));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        } else if (item == ModItems.ENIGMA_BIOS.get()) {
            // Dorado Enigma (Pulsante)
            int r = 255;
            int g = (int) (200 + 45 * Math.sin(phase));
            int b = (int) (30 + 30 * Math.sin(phase));
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return defaultColor; // Standard Cyan
    }

    private static void drawEdgeShine(GuiGraphics g, int x, int y, int w, int h, float d1, float d2, int color) {
        float P = 2.0F * w + 2.0F * h;
        d1 = (d1 % P + P) % P;
        d2 = (d2 % P + P) % P;
        if (Math.abs(d1 - d2) > P / 2.0F) return;

        float minD = Math.min(d1, d2);
        float maxD = Math.max(d1, d2);

        // Top edge: [0, w]
        if (minD < w) {
            int x1 = x + (int) minD;
            int x2 = x + (int) Math.min(w, maxD);
            if (x2 > x1) g.fill(x1, y, x2, y + 1, color);
        }
        // Right edge: [w, w + h]
        if (maxD > w && minD < w + h) {
            int y1 = y + (int) (Math.max(w, minD) - w);
            int y2 = y + (int) (Math.min(w + h, maxD) - w);
            if (y2 > y1) g.fill(x + w - 1, y1, x + w, y2, color);
        }
        // Bottom edge: [w + h, 2*w + h]
        if (maxD > w + h && minD < 2 * w + h) {
            int sub1 = (int) (Math.max(w + h, minD) - (w + h));
            int sub2 = (int) (Math.min(2 * w + h, maxD) - (w + h));
            int x1 = x + w - sub2;
            int x2 = x + w - sub1;
            if (x2 > x1) g.fill(x1, y + h - 1, x2, y + h, color);
        }
        // Left edge: [2*w + h, 2*w + 2*h]
        if (maxD > 2 * w + h) {
            int sub1 = (int) (Math.max(2 * w + h, minD) - (2 * w + h));
            int sub2 = (int) (Math.min(2 * w + 2 * h, maxD) - (2 * w + h));
            int y1 = y + h - sub2;
            int y2 = y + h - sub1;
            if (y2 > y1) g.fill(x, y1, x + 1, y2, color);
        }
    }

    private static void drawPerimeterShineSegment(GuiGraphics g, int x, int y, int w, int h, float startD, float endD) {
        float P = 2.0F * w + 2.0F * h;
        float step = 2.5F;
        float segmentLen = endD - startD;
        if (segmentLen <= 0) return;

        for (float d = startD; d < endD; d += step) {
            float dNext = d + step;
            float frac = (d - startD) / segmentLen;
            int alpha = (int) (240 * Math.sin(frac * Math.PI));
            int glowColor = (alpha << 24) | 0xFFFFFF;

            drawEdgeShine(g, x, y, w, h, d, dNext, glowColor);
        }
    }

    private static int getItemRarityBgColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0x11000000;
        Item item = stack.getItem();
        if (ModTooltipHandler.isModWeapon(stack)) {
            if (item == ModItems.SMART_HALBERD.get()) {
                return 0x22001510; // Dark Aqua tint
            }
            return 0x221C0202; // Dark Red tint
        } else if (ModTooltipHandler.isExtremeBurstCurio(stack)) {
            return 0x22120215; // Dark Purple tint
        } else if (item == ModItems.ENIGMA_BIOS.get()) {
            return 0x221C1602; // Dark Gold tint
        }
        return 0x11000000;
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY, int borderColor, boolean flashing, long elapsed) {
        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        int activeBorderColor = borderColor;
        int activeBgColor = 0x11000000;
        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            activeBorderColor = getItemRarityColor(this.analyzedStack, borderColor);
            activeBgColor = getItemRarityBgColor(this.analyzedStack);
        }

        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, activeBgColor);
        g.fill(areaX, areaY, areaX + areaW, areaY + 1, activeBorderColor);
        g.fill(areaX, areaY + areaH - 1, areaX + areaW, areaY + areaH, activeBorderColor);
        g.fill(areaX, areaY, areaX + 1, areaY + areaH, activeBorderColor);
        g.fill(areaX + areaW - 1, areaY, areaX + areaW, areaY + areaH, activeBorderColor);

        // Brillo Reluciente Recorriendo Todo el Perímetro (50% más lento: 3.5s recorrido + 2.5s pausa)
        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            long time = System.currentTimeMillis();
            long cycleTime = time % 6000L; // 6s ciclo total

            if (cycleTime < 3500L) { // 3.5s recorriendo, 2.5s invisible
                float progress = cycleTime / 3500.0F;
                float totalPerimeter = 2.0F * areaW + 2.0F * areaH;
                float headDist = progress * totalPerimeter;
                float shineLen = 65.0F;

                drawPerimeterShineSegment(g, areaX, areaY, areaW, areaH, headDist - shineLen, headDist);
            }
        }

        if (this.activeTab == 0) {
            // ANALYZER TAB
            if (this.analyzedStack.isEmpty()) {
                g.drawString(this.font, translate("gui.xeb.enigma_bios.analyzer.empty"), areaX + 12, areaY + 12, 0xFF888888, false);
            } else {
                AnalyzedInfo info = analyzeItem(this.analyzedStack);

                g.renderFakeItem(this.analyzedStack, areaX + 12, areaY + 8);
                g.renderItemDecorations(this.font, this.analyzedStack, areaX + 12, areaY + 8);

                int nameColor = flashing ? 0xFFFF3333 : activeBorderColor;
                g.drawString(this.font, info.name, areaX + 34, areaY + 6, nameColor, false);

                String itemLoreText = translate(info.translationKey + ".enigma_lore");
                if (itemLoreText.equals(info.translationKey + ".enigma_lore") || itemLoreText.isEmpty()) {
                    itemLoreText = translate(info.translationKey + ".enigma_effect");
                }

                if (!itemLoreText.isEmpty() && !itemLoreText.startsWith("item.xeb")) {
                    List<FormattedText> headerLoreLines = this.font.getSplitter().splitLines("§o" + itemLoreText, areaW - 48, net.minecraft.network.chat.Style.EMPTY);
                    int totalHeaderLoreH = headerLoreLines.size() * 9;
                    int maxHeaderLoreScroll = Math.max(0, totalHeaderLoreH - 18);

                    if (maxHeaderLoreScroll > 0) {
                        // Passive Auto-Scroll when player is not manually interacting
                        if (!this.isDraggingHeaderLoreScroll && (System.currentTimeMillis() - this.lastHeaderLoreScrollTime > 3000L)) {
                            long cycleTime = System.currentTimeMillis() % 12000L;
                            float progress;
                            if (cycleTime < 1500L) {
                                progress = 0.0F;
                            } else if (cycleTime < 6000L) {
                                progress = (cycleTime - 1500L) / 4500.0F;
                            } else if (cycleTime < 7500L) {
                                progress = 1.0F;
                            } else {
                                progress = 1.0F - (cycleTime - 7500L) / 4500.0F;
                            }
                            progress = progress * progress * (3.0F - 2.0F * progress);
                            this.headerLoreScrollAmount = progress * maxHeaderLoreScroll;
                        } else {
                            this.headerLoreScrollAmount = Mth.clamp(this.headerLoreScrollAmount, 0.0F, maxHeaderLoreScroll);
                        }

                        // Smart Auto-Hiding Scrollbar
                        renderAutoHidingScrollbar(g, areaX + areaW - 6, areaY + 16, 3, 18, this.headerLoreScrollAmount, maxHeaderLoreScroll, totalHeaderLoreH, this.lastHeaderLoreScrollTime, this.isDraggingHeaderLoreScroll);
                    } else {
                        this.headerLoreScrollAmount = 0.0F;
                    }

                    g.enableScissor(areaX + 34, areaY + 16, areaX + areaW - 10, areaY + 34);
                    int ly = areaY + 17 - (int) this.headerLoreScrollAmount;
                    for (FormattedText line : headerLoreLines) {
                        g.drawString(this.font, line.getString(), areaX + 34, ly, 0xFFBBBBBB, false);
                        ly += 9;
                    }
                    g.disableScissor();
                }

                if (info.hasCustomHUD) {
                    int hudBtnX = areaX + areaW - 84;
                    int hudBtnY = areaY + 4;
                    int hudBtnW = 78;
                    int hudBtnH = 11;
                    boolean btnHov = mouseX >= hudBtnX && mouseX < hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY < hudBtnY + hudBtnH;

                    int hudBg = btnHov ? ((activeBorderColor & 0x00FFFFFF) | 0xCC000000) : ((activeBorderColor & 0x00FFFFFF) | 0x44000000);
                    g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, hudBg);
                    g.drawString(this.font, translate("gui.xeb.enigma_bios.hud_pos"), hudBtnX + 4, hudBtnY + 2, btnHov ? 0xFF08111E : activeBorderColor, false);
                }

                g.fill(areaX + 12, areaY + 38, areaX + areaW - 12, areaY + 39, activeBorderColor);

                if (info.hasAbilities) {
                    int btnW = 50;
                    int btnH = 14;
                    int btnY = areaY + 42;

                    String[] labels = new String[]{"L-Click", "R-Click", "Act 1", "Act 2", "Burst"};

                    int bgSelected = (activeBorderColor & 0x00FFFFFF) | 0xCC000000;
                    int bgHover = (activeBorderColor & 0x00FFFFFF) | 0x44000000;
                    int bgNormal = (activeBorderColor & 0x00FFFFFF) | 0x22000000;

                    for (int b = 0; b < 5; b++) {
                        int bx = areaX + 12 + b * 52;
                        boolean isSelected = (this.selectedAbilityIndex == b);
                        boolean isDisabled = info.isAbilityDisabled(b);
                        boolean isHov = !isDisabled && mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH;

                        int btnBg = isDisabled ? 0x22444444 : (isSelected ? bgSelected : (isHov ? bgHover : bgNormal));
                        int btnTxt = isDisabled ? 0xFF666666 : (isSelected ? 0xFF08111E : (isHov ? 0xFFFFFFFF : activeBorderColor));

                        g.fill(bx, btnY, bx + btnW, btnY + btnH, btnBg);
                        g.drawString(this.font, labels[b], bx + (btnW - this.font.width(labels[b])) / 2, btnY + 3, btnTxt, false);
                    }

                    g.fill(areaX + 12, areaY + 58, areaX + areaW - 12, areaY + 59, (activeBorderColor & 0x00FFFFFF) | 0x44000000);

                    int detailY = areaY + 62;
                    int detailH = areaY + areaH - 4 - detailY;
                    int idx = this.selectedAbilityIndex;

                    if (idx == 4) {
                        ExtremeBurstRegistry.ExtremeBurstEntry burst = ExtremeBurstRegistry.getEntry(this.analyzedStack.getItem());
                        if (burst == null) {
                            Item item = this.analyzedStack.getItem();
                            if (item == ModItems.GOLDEN_FLOWER.get()) {
                                burst = ExtremeBurstRegistry.getEntry(ModItems.OMEGA_FLOWERY.get());
                            } else if (item == ModItems.THE_TEARS.get()) {
                                burst = ExtremeBurstRegistry.getEntry(ModItems.DOGMA.get());
                            } else if (item == ModItems.DOOMFIST.get() || item == ModItems.DOOMFIST_V2.get()) {
                                burst = ExtremeBurstRegistry.getEntry(ModItems.METEOR_STRIKE.get());
                            } else if (item == ModItems.OPTIC_BLAST.get()) {
                                burst = ExtremeBurstRegistry.getEntry(ModItems.FULL_APERTURE_SUPERNOVA.get());
                            }
                        }

                        if (burst != null) {
                            String curioName = new ItemStack(burst.curioItem).getHoverName().getString();
                            String burstName = curioName + " Burst";
                            String cdStr = (burst.cooldownTicks / 20) + "s (" + String.format(java.util.Locale.US, "%.1f", (burst.cooldownTicks / 1200.0F)) + " min)";

                            String verStr = burst.version == ExtremeBurstRegistry.BurstVersion.INSTANCE
                                    ? "Instancia (20s Duración)"
                                    : (burst.durationTicks > 0 ? "Activa (10s Efecto)" : "Instantánea");

                            String reqName = burst.requiredWeaponName != null ? info.name : "";
                            String typeStr = burst.type == ExtremeBurstRegistry.BurstType.LIMITED
                                    ? ("Limitado (Requiere: " + reqName + ")")
                                    : "Universal";

                            String burstDescKey = "item.xeb." + burst.curioItem.getDescriptionId().replace("item.xeb.", "").replace("item.", "") + ".extreme_burst.desc";
                            String burstDesc = translate(burstDescKey);
                            if (burstDesc.startsWith("item.xeb") || burstDesc.isEmpty()) {
                                burstDesc = "Desata una habilidad definitiva destructiva al activar la tecla de Extreme Burst.";
                            }

                            List<FormattedText> descLines = this.font.getSplitter().splitLines(burstDesc, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                            int totalH = 46 + descLines.size() * 10;
                            int maxScroll = Math.max(0, totalH - detailH);
                            this.analyzerScrollAmount = Mth.clamp(this.analyzerScrollAmount, 0.0F, maxScroll);

                            renderAutoHidingScrollbar(g, areaX + areaW - 6, detailY, 3, detailH, this.analyzerScrollAmount, maxScroll, totalH, this.lastAnalyzerScrollTime, this.isDraggingAnalyzerScroll);

                            g.enableScissor(areaX + 12, detailY, areaX + areaW - 12, areaY + areaH - 4);
                            int dy = detailY - (int) this.analyzerScrollAmount;

                            g.drawString(this.font, "Extreme Burst: " + burstName, areaX + 12, dy, activeBorderColor, false);
                            dy += 11;
                            g.drawString(this.font, "Versión: " + verStr, areaX + 12, dy, 0xFFFFCC00, false);
                            dy += 10;
                            g.drawString(this.font, "Tipo: " + typeStr, areaX + 12, dy, 0xFFFFCC00, false);
                            dy += 10;
                            g.drawString(this.font, "Enfriamiento: " + cdStr, areaX + 12, dy, 0xFFFFCC00, false);
                            dy += 14;

                            for (FormattedText line : descLines) {
                                g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                                dy += 10;
                            }
                            g.disableScissor();
                        } else {
                            g.drawString(this.font, "Extreme Burst", areaX + 12, detailY, activeBorderColor, false);
                            g.drawString(this.font, "Sin Extreme Burst asignado para este objeto.", areaX + 12, detailY + 11, 0xFFFF5555, false);
                        }
                    } else {
                        String abilityNameKey = switch (idx) {
                            case 0 -> "left_click";
                            case 1 -> "right_click";
                            case 2 -> "active1";
                            case 3 -> "active2";
                            default -> "";
                        };

                        String abName = translate(info.translationKey + ".ability." + abilityNameKey + ".name");
                        String abDesc = translate(info.translationKey + ".ability." + abilityNameKey + ".desc");

                        String dmg = (info.damages != null && idx < info.damages.length && !info.damages[idx].isEmpty()) ? info.damages[idx] : "N/A";
                        String cd = (info.cooldowns != null && idx < info.cooldowns.length && !info.cooldowns[idx].isEmpty()) ? info.cooldowns[idx] : "N/A";

                        List<FormattedText> descLines = this.font.getSplitter().splitLines(abDesc, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                        int totalH = 22 + descLines.size() * 10;
                        int maxScroll = Math.max(0, totalH - detailH);
                        this.analyzerScrollAmount = Mth.clamp(this.analyzerScrollAmount, 0.0F, maxScroll);

                        renderAutoHidingScrollbar(g, areaX + areaW - 6, detailY, 3, detailH, this.analyzerScrollAmount, maxScroll, totalH, this.lastAnalyzerScrollTime, this.isDraggingAnalyzerScroll);

                        g.enableScissor(areaX + 12, detailY, areaX + areaW - 12, areaY + areaH - 4);
                        int dy = detailY - (int) this.analyzerScrollAmount;

                        g.drawString(this.font, abName, areaX + 12, dy, activeBorderColor, false);
                        dy += 11;
                        String statLine = String.format("Damage: %s | Cooldown: %s", dmg, cd);
                        g.drawString(this.font, statLine, areaX + 12, dy, 0xFFFFCC00, false);
                        dy += 11;

                        for (FormattedText line : descLines) {
                            g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                            dy += 10;
                        }
                        g.disableScissor();
                    }
                } else {
                    int descY = areaY + 42;
                    int descH = areaY + areaH - 6 - descY;

                    String effectKey = info.translationKey.equals("item.unknown")
                            ? "item.unknown.enigma_effect." + this.unknownTextIndex
                            : info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);
                    if (effectText.equals(effectKey) || effectText.isEmpty()) {
                        effectText = translate(info.translationKey + ".enigma_lore");
                    }
                    if (effectText.equals(info.translationKey + ".enigma_lore") || effectText.isEmpty()) {
                        effectText = translate("gui.xeb.enigma_bios.analyzer.no_effect");
                    }

                    List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                    int totalHeight = 14 + effectLines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - descH);
                    this.analyzerScrollAmount = Mth.clamp(this.analyzerScrollAmount, 0.0F, maxScroll);

                    // Barra de scroll interactiva y auto-ocultable para Analyzer
                    renderAutoHidingScrollbar(g, areaX + areaW - 6, descY, 3, descH, this.analyzerScrollAmount, maxScroll, totalHeight, this.lastAnalyzerScrollTime, this.isDraggingAnalyzerScroll);

                    g.enableScissor(areaX + 12, descY, areaX + areaW - 12, areaY + areaH - 4);
                    int dy = descY - (int) analyzerScrollAmount;

                    g.drawString(this.font, translate("gui.xeb.enigma_bios.analyzer.effects"), areaX + 12, dy, activeBorderColor, false);
                    dy += 12;
                    for (FormattedText line : effectLines) {
                        g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFFFFFFF, false);
                        dy += 10;
                    }
                    g.disableScissor();
                }
            }
        } else if (this.activeTab == 1) {
            // TAB BESTIARIO
            List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
            if (!allBuffs.isEmpty()) {
                if (this.selectedBestiaryIndex < 0 || this.selectedBestiaryIndex >= allBuffs.size()) {
                    this.selectedBestiaryIndex = 0;
                }

                // Lista de Buffs (Columna Izquierda)
                int listX = areaX + 6;
                int listY = areaY + 6;
                int listW = 95;
                int listH = areaH - 12;

                g.fill(listX, listY, listX + listW, listY + listH, 0x1A000000);
                g.fill(listX, listY, listX + listW, listY + 1, borderColor);
                g.fill(listX, listY + listH - 1, listX + listW, listY + listH, borderColor);
                g.fill(listX, listY, listX + 1, listY + listH, borderColor);
                g.fill(listX + listW - 1, listY, listX + listW, listY + listH, borderColor);

                int totalListH = allBuffs.size() * 16;
                int maxListScroll = Math.max(0, totalListH - (listH - 4));
                this.bestiaryListScrollAmount = Mth.clamp(this.bestiaryListScrollAmount, 0.0F, maxListScroll);

                // Barra de Scroll de Lista de Bestiario
                renderAutoHidingScrollbar(g, listX + listW - 4, listY + 2, 2, listH - 4, this.bestiaryListScrollAmount, maxListScroll, totalListH, this.lastBestiaryListScrollTime, this.isDraggingBestiaryListScroll);

                g.enableScissor(listX + 1, listY + 1, listX + listW - 5, listY + listH - 1);
                for (int b = 0; b < allBuffs.size(); b++) {
                    int by = listY + 2 + b * 16 - (int) bestiaryListScrollAmount;
                    if (by + 14 < listY || by > listY + listH) continue;

                    EliteBuff buff = allBuffs.get(b);
                    boolean isSel = (this.selectedBestiaryIndex == b);
                    boolean bHov = mouseX >= listX + 2 && mouseX < listX + listW - 6 && mouseY >= by && mouseY < by + 14;

                    int itemBg = isSel ? 0xCC00FFCC : (bHov ? 0x4400FFCC : 0x1A00FFCC);
                    int itemTxt = isSel ? 0xFF08111E : (bHov ? 0xFFFFFFFF : 0xFF888888);

                    g.fill(listX + 2, by, listX + listW - 6, by + 14, itemBg);
                    g.drawString(this.font, buff.getDisplayName().getString(), listX + 6, by + 3, itemTxt, false);
                }
                g.disableScissor();

                // Panel de Detalles (Columna Derecha)
                int detX = areaX + 106;
                int detY = areaY + 6;
                int detW = areaW - 112;

                EliteBuff selBuff = allBuffs.get(this.selectedBestiaryIndex);
                g.drawString(this.font, selBuff.getDisplayName().getString(), detX, detY + 2, 0xFF00FFCC, false);

                MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];
                String tierName = switch (tier) {
                    case COMMON -> "BRONZE";
                    case RARE -> "SILVER";
                    case LEGENDARY -> "GOLD";
                };
                int tierColor = switch (tier) {
                    case COMMON -> 0xFFCD7F32;
                    case RARE -> 0xFFC0C0C0;
                    case LEGENDARY -> 0xFFFFD700;
                };

                g.fill(detX, detY + 14, detX + 46, detY + 24, tierColor);
                g.drawString(this.font, tierName, detX + 4, detY + 15, 0xFF08111E, false);

                int kills = 0;
                if (this.minecraft != null && this.minecraft.player != null) {
                    kills = this.minecraft.player.getPersistentData().getInt("xebKilled_" + selBuff.getId());
                }
                g.drawString(this.font, translate("gui.xeb.enigma_bios.bestiary.kills") + kills, detX + 52, detY + 15, 0xFFFFCC00, false);

                // Modelo de Medallón 3D Flotante
                float rotAngle = (System.currentTimeMillis() % 3600L) / 10.0F;
                int renderCenterX = detX + detW - 32;
                int renderCenterY = detY + 36;

                g.pose().pushPose();
                g.pose().translate(renderCenterX, renderCenterY, 150.0F);
                g.pose().scale(2.2F, 2.2F, 2.2F);
                MedallionRenderLayer.renderSingleMedallionGUI(g.pose(), g.bufferSource(), tier, selBuff.getId(), rotAngle, 0xF000F0);
                g.pose().popPose();

                // Línea Divisoria Superior
                g.fill(detX, detY + 28, detX + detW - 65, detY + 29, 0x4400FFCC);

                // INFORMACIÓN 100% FIEL Y EXACTA AL CÓDIGO INTERNO
                String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                if (descText.equals("xeb.buff." + selBuff.getId() + ".desc") || descText.isEmpty()) {
                    descText = "Medallón Élite (" + tierName + "): Confiere propiedades especiales a la entidad huésped.";
                }

                String tierQualityText = getBuffTierQualityDescription(selBuff, tier);

                String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                if (stratText.equals("xeb.buff." + selBuff.getId() + ".counter") || stratText.isEmpty()) {
                    stratText = "Usa encantamientos especiales o ataques combinados para contrarrestar este efecto.";
                }

                // Lore log data for this buff
                int buffLogNum = getBuffLogNumber(selBuff.getId());
                boolean loreUnlocked = buffLogNum > 0 && this.minecraft != null && this.minecraft.player != null
                        && this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + buffLogNum);
                String loreText;
                if (buffLogNum <= 0) {
                    loreText = "";
                } else if (loreUnlocked) {
                    loreText = translate("gui.xeb.enigma_bios.log" + buffLogNum + ".content");
                } else {
                    loreText = translate("gui.xeb.enigma_bios.bestiary.lore.corrupted");
                }

                List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> qualityLines = this.font.getSplitter().splitLines(tierQualityText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> counterLines = this.font.getSplitter().splitLines(stratText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> loreLines = loreText.isEmpty() ? java.util.Collections.emptyList()
                        : this.font.getSplitter().splitLines(loreText, detW - 68, net.minecraft.network.chat.Style.EMPTY);

                int descStartY = detY + 32;
                int scissorBottom = areaY + areaH - 4;
                int descMaxH = scissorBottom - descStartY;

                int loreSection = loreLines.isEmpty() ? 0 : (14 + loreLines.size() * 10);
                int totalBestiaryH = (descLines.size() * 10) + 14 + (qualityLines.size() * 10) + 14 + (counterLines.size() * 10) + 8 + loreSection;
                int maxBestiaryDetailsScroll = Math.max(0, totalBestiaryH - descMaxH);
                this.bestiaryDetailsScrollAmount = Mth.clamp(this.bestiaryDetailsScrollAmount, 0.0F, maxBestiaryDetailsScroll);

                // Barra de Scroll de Detalles de Bestiario
                renderAutoHidingScrollbar(g, detX + detW - 62, descStartY, 3, descMaxH, this.bestiaryDetailsScrollAmount, maxBestiaryDetailsScroll, totalBestiaryH, this.lastBestiaryDetailsScrollTime, this.isDraggingBestiaryDetailsScroll);

                // Renderizado Estructurado con Scissor Estricto (Sin Traslape de Texto)
                g.enableScissor(detX, descStartY, detX + detW - 65, scissorBottom);
                int bY = descStartY - (int) bestiaryDetailsScrollAmount;

                // Sección 1: Descripción
                for (FormattedText line : descLines) {
                    g.drawString(this.font, line.getString(), detX, bY, 0xFFE0E0E0, false);
                    bY += 10;
                }

                bY += 4;
                // Sección 2: Qualities & Effects
                g.drawString(this.font, "Qualities & Effects (" + tierName + "):", detX, bY, 0xFF00FFCC, false);
                bY += 10;
                for (FormattedText line : qualityLines) {
                    g.drawString(this.font, line.getString(), detX, bY, 0xFFFFCC00, false);
                    bY += 10;
                }

                bY += 4;
                // Sección 3: Counter Strategy
                g.drawString(this.font, "Counter Strategy:", detX, bY, 0xFF00FFCC, false);
                bY += 10;
                for (FormattedText line : counterLines) {
                    g.drawString(this.font, line.getString(), detX, bY, 0xFFE0E0E0, false);
                    bY += 10;
                }

                // Sección 4: Lore (Log #N)
                if (!loreLines.isEmpty() && buffLogNum > 0) {
                    bY += 4;
                    String loreHeader;
                    if (loreUnlocked) {
                        loreHeader = translate("gui.xeb.enigma_bios.bestiary.lore.header", buffLogNum);
                    } else {
                        loreHeader = translate("gui.xeb.enigma_bios.bestiary.lore.header.locked", buffLogNum);
                    }
                    g.drawString(this.font, loreHeader, detX, bY, loreUnlocked ? 0xFF00FFCC : 0xFFFF3333, false);
                    bY += 10;
                    int loreColor = loreUnlocked ? 0xFFFFCC55 : 0xFF773333;
                    for (FormattedText line : loreLines) {
                        g.drawString(this.font, line.getString(), detX, bY, loreColor, false);
                        bY += 10;
                    }
                }

                g.disableScissor();
            }
        } else if (this.activeTab == 2) {
            // TAB BITÁCORAS / LOGS (Sub-menú a la izquierda + Detalles a la derecha)
            int listX = areaX + 6;
            int listY = areaY + 6;
            int listW = 95;
            int listH = areaH - 12;

            // Borde y Fondo de Lista de Bitácoras
            g.fill(listX, listY, listX + listW, listY + listH, 0x1A000000);
            g.fill(listX, listY, listX + listW, listY + 1, borderColor);
            g.fill(listX, listY + listH - 1, listX + listW, listY + listH, borderColor);
            g.fill(listX, listY, listX + 1, listY + listH, borderColor);
            g.fill(listX + listW - 1, listY, listX + listW, listY + listH, borderColor);

            int totalListH = logs.size() * 18;
            int maxListScroll = Math.max(0, totalListH - (listH - 4));
            this.logListScrollAmount = Mth.clamp(this.logListScrollAmount, 0.0F, maxListScroll);

            renderAutoHidingScrollbar(g, listX + listW - 4, listY + 2, 2, listH - 4, this.logListScrollAmount, maxListScroll, totalListH, this.lastLogListScrollTime, this.isDraggingLogListScroll);

            g.enableScissor(listX + 1, listY + 1, listX + listW - 5, listY + listH - 1);
            for (int b = 0; b < logs.size(); b++) {
                int by = listY + 2 + b * 18 - (int) logListScrollAmount;
                if (by + 16 < listY || by > listY + listH) continue;

                boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                        this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (b + 1));
                boolean isSel = (this.selectedLogIndex == b);
                boolean bHov = mouseX >= listX + 2 && mouseX < listX + listW - 6 && mouseY >= by && mouseY < by + 16;

                int itemBg = isSel ? 0xCC00FFCC : (bHov ? 0x4400FFCC : 0x1A00FFCC);
                int itemTxt = isUnlocked
                        ? (isSel ? 0xFF08111E : (bHov ? 0xFFFFFFFF : 0xFF00FFCC))
                        : (isSel ? 0xFF08111E : 0xFFFF5555);

                g.fill(listX + 2, by, listX + listW - 6, by + 16, itemBg);
                String logItemLabel = translate("gui.xeb.enigma_bios.tab.log") + " " + (b + 1) + (isUnlocked ? "" : " 🔒");
                g.drawString(this.font, logItemLabel, listX + 6, by + 4, itemTxt, false);
            }
            g.disableScissor();

            // Panel de Detalles de la Bitácora Seleccionada (Columna Derecha)
            int detX = areaX + 106;
            int detY = areaY + 6;
            int detW = areaW - 112;
            int detH = areaH - 12;

            if (this.selectedLogIndex >= 0 && this.selectedLogIndex < logs.size()) {
                int index = this.selectedLogIndex;
                boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                        this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));

                if (!isUnlocked) {
                    g.drawString(this.font, translate("gui.xeb.enigma_bios.log.locked.title"), detX, detY + 2, 0xFFFF3333, false);
                    g.fill(detX, detY + 14, detX + detW - 6, detY + 15, 0x44FF3333);

                    int textY = detY + 20;
                    String lockedDesc = translate("gui.xeb.enigma_bios.log.locked.desc");
                    List<FormattedText> lines = this.font.getSplitter().splitLines(lockedDesc, detW - 10, net.minecraft.network.chat.Style.EMPTY);
                    for (FormattedText line : lines) {
                        g.drawString(this.font, line.getString(), detX, textY, 0xFF777777, false);
                        textY += 10;
                    }
                } else {
                    LogEntry log = logs.get(index);
                    g.drawString(this.font, translate(log.titleKey), detX, detY + 2, 0xFF00FFCC, false);
                    g.fill(detX, detY + 14, detX + detW - 6, detY + 15, 0x4400FFCC);

                    int textY = detY + 20;
                    int textH = detH - 20;

                    List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), detW - 14, net.minecraft.network.chat.Style.EMPTY);
                    int totalHeight = lines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - textH);
                    this.logDetailsScrollAmount = Mth.clamp(this.logDetailsScrollAmount, 0.0F, maxScroll);

                    renderAutoHidingScrollbar(g, detX + detW - 4, textY, 3, textH, this.logDetailsScrollAmount, maxScroll, totalHeight, this.lastLogDetailsScrollTime, this.isDraggingLogDetailsScroll);

                    g.enableScissor(detX, textY, detX + detW - 6, detY + detH);
                    int dy = textY - (int) logDetailsScrollAmount;
                    for (FormattedText line : lines) {
                        g.drawString(this.font, line.getString(), detX, dy, 0xFFE0E0E0, false);
                        dy += 10;
                    }
                    g.disableScissor();
                }
            }
        } else if (this.activeTab == 3) {
            // TAB MASTERY / MAESTRÍA ÉLITE
            int level = 0;
            if (this.minecraft != null && this.minecraft.player != null) {
                level = this.minecraft.player.getPersistentData().getInt("xebEliteMeterLevel");
            }

            int pX = areaX + 10;
            int pY = areaY + 6;
            int pW = areaW - 20;

            // Encabezado
            g.drawString(this.font, translate("gui.xeb.enigma_bios.mastery.title_panel"), pX, pY, 0xFF00FFCC, false);
            String lvlTag = (level > 10 ? translate("gui.xeb.enigma_bios.mastery.overflow_tag") : translate("gui.xeb.enigma_bios.mastery.level_tag", level));
            int lvlColor = level > 10 ? 0xFFFFD700 : (level >= 7 ? 0xFFFF7700 : (level >= 4 ? 0xFFAAFF00 : (level >= 1 ? 0xFF00FFCC : 0xFF888888)));
            g.drawString(this.font, lvlTag, pX + pW - this.font.width(lvlTag), pY, lvlColor, false);

            // Medidor Rediseñado de Alto Nivel (Barra Sci-Fi Segmentada)
            int gaugeY = pY + 13;
            int gaugeH = 14;
            g.fill(pX, gaugeY, pX + pW, gaugeY + gaugeH, 0xAA08111E);
            g.fill(pX, gaugeY, pX + pW, gaugeY + 1, 0x5500FFCC);
            g.fill(pX, gaugeY + gaugeH - 1, pX + pW, gaugeY + gaugeH, 0x5500FFCC);
            g.fill(pX, gaugeY, pX + 1, gaugeY + gaugeH, 0x5500FFCC);
            g.fill(pX + pW - 1, gaugeY, pX + pW, gaugeY + gaugeH, 0x5500FFCC);

            float fillFrac = level > 10 ? 1.0F : Mth.clamp(level / 10.0F, 0.0F, 1.0F);
            int fillW = (int) (fillFrac * (pW - 4));

            if (fillW > 0) {
                int barColor = level > 10 ? 0xFFFFD700 : (level >= 10 ? 0xFFCC0000 : (level >= 7 ? 0xFFFF5500 : (level >= 4 ? 0xFFAAFF00 : (level >= 1 ? 0xFF00FFCC : 0xFF555555))));
                g.fill(pX + 2, gaugeY + 2, pX + 2 + fillW, gaugeY + gaugeH - 2, barColor);

                // Brillo dinámico de sobrecarga
                if (level > 10) {
                    long t = System.currentTimeMillis();
                    float pulse = (float)(Math.sin(t * 0.008) * 0.5 + 0.5);
                    int glowA = (int)(120 * pulse);
                    g.fill(pX + 2, gaugeY + 2, pX + 2 + fillW, gaugeY + gaugeH - 2, (glowA << 24) | 0xFFFFFF);
                }
            }

            // Segmentos divisores
            for (int seg = 1; seg < 10; seg++) {
                int segX = pX + 2 + (int) (seg * (pW - 4) / 10.0F);
                g.fill(segX, gaugeY + 2, segX + 1, gaugeY + gaugeH - 2, 0x44000000);
            }

            String gaugeLabel = level > 10 ? (translate("gui.xeb.enigma_bios.mastery.overflow_tag") + " (+ " + (level - 10) + ")") : translate("gui.xeb.enigma_bios.mastery.progress_format", level * 10);
            g.drawString(this.font, gaugeLabel, pX + (pW - this.font.width(gaugeLabel)) / 2, gaugeY + 3, 0xFFFFFFFF, false);

            // Área de Contenido Escroleable
            int detY = gaugeY + gaugeH + 6;
            int detH = areaH - (detY - areaY) - 4;

            String loreKey = (level >= 10 ? "chat.xeb.mastery.desc.10" : (level >= 7 ? "chat.xeb.mastery.desc.7" : (level >= 4 ? "chat.xeb.mastery.desc.4" : (level >= 1 ? "chat.xeb.mastery.desc.1" : "gui.xeb.enigma_bios.mastery.level0"))));
            String rawLore = translate(loreKey).replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "");
            List<FormattedText> loreLines = this.font.getSplitter().splitLines("§o" + rawLore, pW - 12, net.minecraft.network.chat.Style.EMPTY);

            String detailKey = (level > 10 ? "gui.xeb.enigma_bios.mastery.detail.overflow" : (level >= 10 ? "gui.xeb.enigma_bios.mastery.detail.level10" : (level >= 7 ? "gui.xeb.enigma_bios.mastery.detail.level7" : (level >= 4 ? "gui.xeb.enigma_bios.mastery.detail.level4" : (level >= 1 ? "gui.xeb.enigma_bios.mastery.detail.level1" : "gui.xeb.enigma_bios.mastery.detail.level0")))));
            String detailText = level > 10 ? translate(detailKey, (level - 10)) : translate(detailKey);
            List<FormattedText> detailLines = this.font.getSplitter().splitLines(detailText, pW - 12, net.minecraft.network.chat.Style.EMPTY);

            int totalH = 12 + loreLines.size() * 9 + 14 + detailLines.size() * 10 + 6;
            int maxScroll = Math.max(0, totalH - detH);
            this.masteryScrollAmount = Mth.clamp(this.masteryScrollAmount, 0.0F, maxScroll);

            renderAutoHidingScrollbar(g, pX + pW - 3, detY, 3, detH, this.masteryScrollAmount, maxScroll, totalH, this.lastMasteryScrollTime, this.isDraggingMasteryScroll);

            g.enableScissor(pX, detY, pX + pW - 6, detY + detH);
            int mY = detY - (int) masteryScrollAmount;

            g.drawString(this.font, "Lore & Ambientación:", pX, mY, 0xFFFFCC00, false);
            mY += 10;
            for (FormattedText line : loreLines) {
                g.drawString(this.font, line.getString(), pX, mY, 0xFFDDDDDD, false);
                mY += 9;
            }

            mY += 4;
            g.drawString(this.font, translate("gui.xeb.enigma_bios.mastery.effects_header"), pX, mY, 0xFF00FFCC, false);
            mY += 10;
            for (FormattedText line : detailLines) {
                g.drawString(this.font, line.getString(), pX, mY, 0xFFFFFFFF, false);
                mY += 10;
            }

            g.disableScissor();
        } else if (this.activeTab == 4) {
            // TAB SCANNER / ESCÁNER DE DAÑO
            int pX = areaX + 10;
            int pY = areaY + 6;
            int pW = areaW - 20;

            // Encabezado
            g.drawString(this.font, translate("gui.xeb.enigma_bios.scanner.title_panel"), pX, pY, 0xFF00FFCC, false);

            // Botones interactivos de selección de Modo
            int btnY = pY + 12;
            int btnH = 15;
            org.xeb.xeb.damagenumber.DamageNumberMode[] modes = org.xeb.xeb.damagenumber.DamageNumberMode.values();
            int btnW = (pW - (modes.length - 1) * 4) / modes.length;

            org.xeb.xeb.damagenumber.DamageNumberMode currentMode = org.xeb.xeb.damagenumber.DamageNumberConfig.getMode();

            for (int i = 0; i < modes.length; i++) {
                org.xeb.xeb.damagenumber.DamageNumberMode mode = modes[i];
                int bx = pX + i * (btnW + 4);
                boolean isCurrent = (mode == currentMode);
                boolean isHov = mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH;

                int bg = isCurrent ? 0xCC00FFCC : (isHov ? 0x4400FFCC : 0x2200FFCC);
                int txt = isCurrent ? 0xFF08111E : (isHov ? 0xFFFFFFFF : 0xFF00FFCC);

                g.fill(bx, btnY, bx + btnW, btnY + btnH, bg);
                String mName = mode.getDisplayName();
                g.drawString(this.font, mName, bx + (btnW - this.font.width(mName)) / 2, btnY + 4, txt, false);
            }

            // Explicación corta del modo seleccionado (Posición Y Fija para no mover el mockup!)
            int descY = btnY + btnH + 4;
            g.drawString(this.font, currentMode.getDisplayName() + ":", pX, descY, 0xFFFFCC00, false);
            List<FormattedText> modeDescLines = this.font.getSplitter().splitLines(currentMode.getDescription(), pW, net.minecraft.network.chat.Style.EMPTY);
            int dy = descY + 9;
            for (int lineIdx = 0; lineIdx < Math.min(2, modeDescLines.size()); lineIdx++) {
                g.drawString(this.font, modeDescLines.get(lineIdx).getString(), pX, dy + lineIdx * 9, 0xFFDDDDDD, false);
            }

            // Previsualización Animada en Vivo (Mockup Canvas Box con Y Fija)
            int prevY = areaY + 62;
            int prevH = 34;
            g.fill(pX, prevY, pX + pW, prevY + prevH, 0xBB050C16);
            g.fill(pX, prevY, pX + pW, prevY + 1, 0x5500FFCC);
            g.fill(pX, prevY + prevH - 1, pX + pW, prevY + prevH, 0x5500FFCC);
            g.fill(pX, prevY, pX + 1, prevY + prevH, 0x5500FFCC);
            g.fill(pX + pW - 1, prevY, pX + pW, prevY + prevH, 0x5500FFCC);

            // Icono / Etiqueta del Objetivo de Pruebas
            g.drawString(this.font, "🎯 " + translate("gui.xeb.enigma_bios.scanner.dummy_target"), pX + 6, prevY + 13, 0xFF888888, false);

            // Renderizado animado de los números flotantes según el modo
            long time = System.currentTimeMillis();
            int centerPreviewX = pX + pW - 65;

            if (currentMode == org.xeb.xeb.damagenumber.DamageNumberMode.COMBINE) {
                // Modo Combine: un único total dinámico que va creciendo y cambiando de color
                float cycle = (time % 2000L) / 2000.0F; // 0 a 1
                float dmgVal = 12.0F + cycle * 95.0F; // 12.0 a 107.0
                int col = org.xeb.xeb.damagenumber.DamageNumberConfig.getColorForTotalDamage(dmgVal);
                String dmgStr = String.format(java.util.Locale.US, "%.1f", dmgVal);
                int floatY = prevY + 18 - (int)(cycle * 10.0F);
                g.drawString(this.font, "★ " + dmgStr, centerPreviewX, floatY, col, true);
            } else if (currentMode == org.xeb.xeb.damagenumber.DamageNumberMode.STACK) {
                // Modo Stack: 3 números independientes subiendo a diferente tiempo
                for (int s = 0; s < 3; s++) {
                    float cycle = ((time + s * 650L) % 1800L) / 1800.0F;
                    int floatY = prevY + 20 - (int)(cycle * 14.0F);
                    int sx = centerPreviewX - 25 + s * 26;
                    String sVal = String.format(java.util.Locale.US, "+%.1f", 4.0F + s * 2.5F);
                    int alpha = (int)(255 * (1.0F - cycle));
                    int col = (alpha << 24) | 0x00FFFF;
                    g.drawString(this.font, sVal, sx, floatY, col, false);
                }
            } else if (currentMode == org.xeb.xeb.damagenumber.DamageNumberMode.HYBRID) {
                // Modo Híbrido: un individual pequeño y un total acumulado grande
                float cycle = (time % 2000L) / 2000.0F;
                int floatY = prevY + 18 - (int)(cycle * 10.0F);
                g.drawString(this.font, "+4.5", centerPreviewX - 30, floatY + 4, 0xFF00FFFF, false);
                g.drawString(this.font, "★ 48.5", centerPreviewX + 5, floatY, 0xFFFF8C00, true);
            } else {
                // OFF (Traducido según idioma)
                String offText = translate("gui.xeb.enigma_bios.scanner.off");
                g.drawString(this.font, offText, centerPreviewX - 10, prevY + 13, 0xFF666666, false);
            }

            // Leyenda de Colores en 2 Líneas con Y Fija
            int legY = prevY + prevH + 3;
            g.drawString(this.font, translate("gui.xeb.enigma_bios.scanner.legend_text1"), pX, legY, 0xFF888888, false);
            g.drawString(this.font, translate("gui.xeb.enigma_bios.scanner.legend_text2"), pX, legY + 9, 0xFF888888, false);
        }
    }

    /** Returns the Enigma Bios log number (1-based) linked to this buff's lore, or -1 if unknown. */
    private static int getBuffLogNumber(String buffId) {
        // Matches registration order in Xeb.registerAllBuffs() starting at log #6.
        return switch (buffId) {
            case "spiky"               -> 6;
            case "reactive"            -> 7;
            case "damaging"            -> 8;
            case "tough"               -> 9;
            case "shielded"            -> 10;
            case "protected"           -> 11;
            case "speedy"              -> 12;
            case "flaming"             -> 13;
            case "creepy"              -> 14;
            case "lucky"               -> 15;
            case "static"              -> 16;
            case "bouncy"              -> 17;
            case "mirror"              -> 18;
            case "resonant"            -> 19;
            case "undying"             -> 20;
            case "healthy"             -> 21;
            case "sandy"               -> 22;
            case "infested"            -> 23;
            case "absorbent"           -> 24;
            case "depressing"          -> 25;
            case "slightly_depressing" -> 26;
            case "evolving"            -> 27;
            case "plow"                -> 28;
            case "mega"                -> 29;
            case "mad"                 -> 30;
            case "twin"                -> 31;
            case "hardy"               -> 32;
            case "sticky"              -> 33;
            default                    -> -1;
        };
    }

    /** Translates a key that takes a single integer argument (e.g. "Lore (Log %d):"). */
    private String translate(String key, int arg) {
        return Component.translatable(key, arg).getString();
    }

    private String getBuffTierQualityDescription(EliteBuff buff, MedallionType tier) {
        String id = buff.getId();
        return switch (id) {
            case "damaging" -> switch (tier) {
                case COMMON -> "+2.0 Melee Attack Damage (+1.0 for Bosses).";
                case RARE -> "+4.0 Melee Attack Damage (+2.0 for Bosses).";
                case LEGENDARY -> "+6.0 Melee Attack Damage (+3.0 for Bosses).";
            };
            case "healthy" -> switch (tier) {
                case COMMON -> "+4.0 Max Health & Permanent Regeneration III.";
                case RARE -> "+8.0 Max Health & Permanent Regeneration III.";
                case LEGENDARY -> "+12.0 Max Health & Permanent Regeneration III.";
            };
            case "tough" -> switch (tier) {
                case COMMON -> "+2.0 Armor (+1.0 for Bosses).";
                case RARE -> "+4.0 Armor (+2.0 for Bosses).";
                case LEGENDARY -> "+6.0 Armor (+3.0 for Bosses).";
            };
            case "speedy" -> switch (tier) {
                case COMMON -> "+0.08 Movement Speed & Speed II.";
                case RARE -> "+0.16 Movement Speed & Speed II.";
                case LEGENDARY -> "+0.24 Movement Speed & Speed II.";
            };
            case "shielded" -> switch (tier) {
                case COMMON -> "Projectiles Shield: Absorbs 3/5/7 projectile attacks (-30% Max HP).";
                case RARE -> "Projectiles Shield: Absorbs 6/10/14 projectile attacks (-30% Max HP).";
                case LEGENDARY -> "Projectiles Shield: Absorbs 9/15/21 projectile attacks (-30% Max HP).";
            };
            case "protected" -> switch (tier) {
                case COMMON -> "Holy Shield absorbs 100% of 1 hit, 30s regen cooldown (-20% Max HP).";
                case RARE -> "Holy Shield absorbs 100% of 1 hit, 20s regen cooldown (-20% Max HP).";
                case LEGENDARY -> "Holy Shield absorbs 100% of 1 hit, 10s regen cooldown (-20% Max HP).";
            };
            case "undying" -> switch (tier) {
                case COMMON -> "Revives once on death with 50% HP (medallion breaks in 2s).";
                case RARE -> "Revives once on death with 75% HP (medallion breaks in 2s).";
                case LEGENDARY -> "Revives once on death with 100% HP (medallion breaks in 2s).";
            };
            case "mirror" -> switch (tier) {
                case COMMON -> "Reflect II effect (20% incoming damage reflected).";
                case RARE -> "Reflect IV effect (40% incoming damage reflected).";
                case LEGENDARY -> "Reflect VI effect (60% incoming damage reflected).";
            };
            case "spiky" -> switch (tier) {
                case COMMON -> "Reflects 20% of incoming melee damage to attacker.";
                case RARE -> "Reflects 40% of incoming melee damage to attacker.";
                case LEGENDARY -> "Reflects 60% of incoming melee damage to attacker.";
            };
            case "reactive" -> switch (tier) {
                case COMMON -> "Grants Regeneration I for 5s upon taking damage.";
                case RARE -> "Grants Regeneration II for 5s upon taking damage.";
                case LEGENDARY -> "Grants Regeneration III for 5s upon taking damage.";
            };
            case "flaming" -> switch (tier) {
                case COMMON -> "Fire Immunity & ignites attackers for 4s on hit.";
                case RARE -> "Fire Immunity & ignites attackers for 8s on hit.";
                case LEGENDARY -> "Fire Immunity & ignites attackers for 12s on hit.";
            };
            case "creepy" -> switch (tier) {
                case COMMON -> "Detonates a power 2.0 explosion on death.";
                case RARE -> "Detonates a power 3.5 explosion on death.";
                case LEGENDARY -> "Detonates a power 5.0 explosion on death.";
            };
            case "static" -> switch (tier) {
                case COMMON -> "Zaps nearby targets with 3.0 Lightning damage every 3 seconds.";
                case RARE -> "Zaps nearby targets with 6.0 Lightning damage every 3 seconds.";
                case LEGENDARY -> "Zaps nearby targets with 9.0 Lightning damage every 3 seconds.";
            };
            case "bouncy" -> switch (tier) {
                case COMMON -> "Jump Boost III & Fall Damage Immunity.";
                case RARE -> "Jump Boost IV & Fall Damage Immunity.";
                case LEGENDARY -> "Jump Boost V & Fall Damage Immunity.";
            };
            case "resonant" -> switch (tier) {
                case COMMON -> "Sonic Blast every 10s pushing entities 4 blocks away.";
                case RARE -> "Sonic Blast every 8s pushing entities 6 blocks away.";
                case LEGENDARY -> "Sonic Blast every 5s pushing entities 8 blocks away.";
            };
            case "lucky" -> switch (tier) {
                case COMMON -> "15% Chance to dodge incoming attacks completely.";
                case RARE -> "30% Chance to dodge incoming attacks completely.";
                case LEGENDARY -> "45% Chance to dodge incoming attacks completely.";
            };
            case "depressing" -> switch (tier) {
                case COMMON -> "ALL_STATS_DOWN aura (-20% speed/dmg, -4 armor) within 10 blocks.";
                case RARE -> "ALL_STATS_DOWN aura within 14 blocks.";
                case LEGENDARY -> "ALL_STATS_DOWN aura within 18 blocks.";
            };
            case "slightly_depressing" -> switch (tier) {
                case COMMON -> "ALL_STATS_DOWN aura (-20% speed/dmg, -4 armor) within 2 blocks.";
                case RARE -> "ALL_STATS_DOWN aura within 4 blocks.";
                case LEGENDARY -> "ALL_STATS_DOWN aura within 6 blocks.";
            };
            case "sandy" -> switch (tier) {
                case COMMON -> "10% Dodge chance & Sandstorm Cloud (Blindness) on death (5b radius).";
                case RARE -> "20% Dodge chance & Sandstorm Cloud (Blindness) on death.";
                case LEGENDARY -> "30% Dodge chance & Sandstorm Cloud (Blindness) on death.";
            };
            case "infested" -> switch (tier) {
                case COMMON -> "Spawns 3-5 Elite Flies on death (inheriting host texture & medallions).";
                case RARE -> "Spawns 5-7 Elite Flies on death.";
                case LEGENDARY -> "Spawns 7-9 Elite Flies on death.";
            };
            case "plow" -> switch (tier) {
                case COMMON -> "Deals 1 trample damage when moving & charges 1 block to attacker on hit.";
                case RARE -> "Deals 2 trample damage when moving & charges 2 blocks to attacker.";
                case LEGENDARY -> "Deals 3 trample damage when moving & charges 3 blocks to attacker.";
            };
            case "mega" -> switch (tier) {
                case COMMON -> "Size +50%, +50% Max HP, +30% Attack Damage.";
                case RARE -> "Size +100%, +100% Max HP, +60% Attack Damage.";
                case LEGENDARY -> "Size +150%, +150% Max HP, +90% Attack Damage.";
            };
            case "mad" -> switch (tier) {
                case COMMON -> "Madness state — attacks all living entities indiscriminately.";
                case RARE -> "Madness state — attacks all living entities + 20% Attack Speed.";
                case LEGENDARY -> "Madness state — attacks all living entities + 40% Attack Speed.";
            };
            case "twin" -> switch (tier) {
                case COMMON -> "Spawns 1 identical Twin duplicate with copied medallions (-50% Max HP).";
                case RARE -> "Spawns 1 identical Twin duplicate with copied medallions (-50% Max HP).";
                case LEGENDARY -> "Spawns 1 identical Twin duplicate with copied medallions (-50% Max HP).";
            };
            case "sticky" -> switch (tier) {
                case COMMON -> "Applies TARRED effect (max 5 stacks, 5s duration) on contact or when hit.";
                case RARE -> "Applies TARRED effect (max 5 stacks, 7s duration).";
                case LEGENDARY -> "Applies TARRED effect (max 5 stacks, 10s duration).";
            };
            case "evolving" -> switch (tier) {
                case COMMON -> "Attaches a new random medallion every 30 seconds of combat (Cap 5).";
                case RARE -> "Attaches a new random medallion every 20 seconds of combat (Cap 5).";
                case LEGENDARY -> "Attaches a new random medallion every 10 seconds of combat (Cap 5).";
            };
            case "absorbent" -> switch (tier) {
                case COMMON -> "Drains 1-2 Mana/sec from stationary targets within 6 blocks into magic damage.";
                case RARE -> "Drains 2-3 Mana/sec from stationary targets within 8 blocks into magic damage.";
                case LEGENDARY -> "Drains 3-4 Mana/sec from stationary targets within 10 blocks into magic damage.";
            };
            default -> switch (tier) {
                case COMMON -> "Bronze Tier: Standard Medallion Potency.";
                case RARE -> "Silver Tier: Enhanced Medallion Potency.";
                case LEGENDARY -> "Gold Tier: Maximum Medallion Potency.";
            };
        };
    }

    private void renderInventory(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int invLeft = this.leftPos + (this.guiWidth - 162) / 2;
        int invTop = this.topPos + 160;

        ItemStack hoveredStack = ItemStack.EMPTY;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;
                g.fill(x, y, x + 18, y + 18, 0x3300FFCC);
                g.fill(x, y, x + 18, y + 1, 0x5500FFCC);
                g.fill(x, y + 17, x + 18, y + 18, 0x5500FFCC);
                g.fill(x, y, x + 1, y + 18, 0x5500FFCC);
                g.fill(x + 17, y, x + 18, y + 18, 0x5500FFCC);

                ItemStack stack = mc.player.getInventory().getItem(9 + row * 9 + col);
                if (!stack.isEmpty()) {
                    g.renderFakeItem(stack, x + 1, y + 1);
                    g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                    if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                        hoveredStack = stack;
                    }
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int x = invLeft + col * 18;
            int y = invTop + 58;
            g.fill(x, y, x + 18, y + 18, 0x3300FFCC);
            g.fill(x, y, x + 18, y + 1, 0x5500FFCC);
            g.fill(x, y + 17, x + 18, y + 18, 0x5500FFCC);
            g.fill(x, y, x + 1, y + 18, 0x5500FFCC);
            g.fill(x + 17, y, x + 18, y + 18, 0x5500FFCC);

            ItemStack stack = mc.player.getInventory().getItem(col);
            if (!stack.isEmpty()) {
                g.renderFakeItem(stack, x + 1, y + 1);
                g.renderItemDecorations(this.font, stack, x + 1, y + 1);
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    hoveredStack = stack;
                }
            }
        }

        if (!hoveredStack.isEmpty()) {
            g.renderTooltip(this.font, hoveredStack, mouseX, mouseY);
        }
    }

    private ItemStack getStackAtMouse(double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return ItemStack.EMPTY;

        int invLeft = this.leftPos + (this.guiWidth - 162) / 2;
        int invTop = this.topPos + 160;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invLeft + col * 18;
                int y = invTop + row * 18;
                if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                    return mc.player.getInventory().getItem(9 + row * 9 + col);
                }
            }
        }

        for (int col = 0; col < 9; col++) {
            int x = invLeft + col * 18;
            int y = invTop + 58;
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                return mc.player.getInventory().getItem(col);
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        if (mouseX >= startX && mouseX < startX + 65 && mouseY >= viewportY && mouseY < viewportY + viewportH) {
            int maxTabScroll = Math.max(0, 5 * 22 - viewportH);
            if (maxTabScroll > 0) {
                this.lastTabScrollTime = System.currentTimeMillis();
                this.tabScrollAmount = Mth.clamp(this.tabScrollAmount - (float) delta * 11.0F, 0.0F, maxTabScroll);
                return true;
            }
        }

        if (mouseX >= areaX && mouseX < areaX + areaW && mouseY >= areaY && mouseY < areaY + areaH) {
            if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
                AnalyzedInfo info = analyzeItem(this.analyzedStack);
                String itemLoreText = translate(info.translationKey + ".enigma_lore");
                if (itemLoreText.equals(info.translationKey + ".enigma_lore") || itemLoreText.isEmpty()) {
                    itemLoreText = translate(info.translationKey + ".enigma_effect");
                }
                if (!itemLoreText.isEmpty() && !itemLoreText.startsWith("item.xeb")) {
                    List<FormattedText> headerLoreLines = this.font.getSplitter().splitLines("§o" + itemLoreText, areaW - 48, net.minecraft.network.chat.Style.EMPTY);
                    int maxHeaderLoreScroll = Math.max(0, headerLoreLines.size() * 9 - 18);
                    if (maxHeaderLoreScroll > 0 && mouseX >= areaX + 34 && mouseX < areaX + areaW - 10 && mouseY >= areaY + 16 && mouseY < areaY + 34) {
                        this.lastHeaderLoreScrollTime = System.currentTimeMillis();
                        this.headerLoreScrollAmount = Mth.clamp(this.headerLoreScrollAmount - (float) delta * 9.0F, 0.0F, maxHeaderLoreScroll);
                        return true;
                    }
                }
                this.lastAnalyzerScrollTime = System.currentTimeMillis();
                this.analyzerScrollAmount = Mth.clamp(this.analyzerScrollAmount - (float) delta * 10.0F, 0.0F, 500.0F);
                return true;
            } else if (this.activeTab == 1) {
                int listX = areaX + 6;
                int listW = 95;
                int detX = areaX + 106;
                int detW = areaW - 112;

                if (mouseX >= listX && mouseX < listX + listW) {
                    int totalBuffs = EliteBuffRegistry.getAll().size();
                    int maxScroll = Math.max(0, totalBuffs * 16 - (areaH - 16));
                    if (maxScroll > 0) {
                        this.lastBestiaryListScrollTime = System.currentTimeMillis();
                        this.bestiaryListScrollAmount = Mth.clamp(this.bestiaryListScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                        return true;
                    }
                } else if (mouseX >= detX && mouseX < detX + detW) {
                    List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
                    if (!allBuffs.isEmpty() && this.selectedBestiaryIndex >= 0 && this.selectedBestiaryIndex < allBuffs.size()) {
                        EliteBuff selBuff = allBuffs.get(this.selectedBestiaryIndex);
                        MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];

                        String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                        if (descText.equals("xeb.buff." + selBuff.getId() + ".desc") || descText.isEmpty()) descText = "Medallón Élite";
                        String tierQualityText = getBuffTierQualityDescription(selBuff, tier);
                        String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                        if (stratText.equals("xeb.buff." + selBuff.getId() + ".counter") || stratText.isEmpty()) stratText = "Estrategia";

                        List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                        List<FormattedText> qualityLines = this.font.getSplitter().splitLines(tierQualityText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                        List<FormattedText> counterLines = this.font.getSplitter().splitLines(stratText, detW - 68, net.minecraft.network.chat.Style.EMPTY);

                        int logNum2 = getBuffLogNumber(selBuff.getId());
                        boolean loreUnlocked2 = logNum2 > 0 && this.minecraft != null && this.minecraft.player != null
                                && this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + logNum2);
                        String loreText2 = logNum2 <= 0 ? ""
                                : loreUnlocked2
                                    ? translate("gui.xeb.enigma_bios.log" + logNum2 + ".content")
                                    : translate("gui.xeb.enigma_bios.bestiary.lore.corrupted");
                        List<FormattedText> loreLines2 = loreText2.isEmpty() ? java.util.Collections.emptyList()
                                : this.font.getSplitter().splitLines(loreText2, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                        int loreSection2 = loreLines2.isEmpty() ? 0 : (14 + loreLines2.size() * 10);

                        int totalH = (descLines.size() * 10) + 14 + (qualityLines.size() * 10) + 14 + (counterLines.size() * 10) + 8 + loreSection2;
                        int maxScroll = Math.max(0, totalH - (areaH - 42));
                        if (maxScroll > 0) {
                            this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                            this.bestiaryDetailsScrollAmount = Mth.clamp(this.bestiaryDetailsScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                            return true;
                        }
                    }
                }
            } else if (this.activeTab == 2) {
                int listX = areaX + 6;
                int listW = 95;
                int detX = areaX + 106;
                int detW = areaW - 112;

                if (mouseX >= listX && mouseX < listX + listW) {
                    int maxScroll = Math.max(0, logs.size() * 18 - (areaH - 16));
                    if (maxScroll > 0) {
                        this.lastLogListScrollTime = System.currentTimeMillis();
                        this.logListScrollAmount = Mth.clamp(this.logListScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                        return true;
                    }
                } else if (mouseX >= detX && mouseX < detX + detW) {
                    if (this.selectedLogIndex >= 0 && this.selectedLogIndex < logs.size()) {
                        LogEntry log = logs.get(this.selectedLogIndex);
                        List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), detW - 14, net.minecraft.network.chat.Style.EMPTY);
                        int maxScroll = Math.max(0, lines.size() * 10 - (areaH - 32));
                        if (maxScroll > 0) {
                            this.lastLogDetailsScrollTime = System.currentTimeMillis();
                            this.logDetailsScrollAmount = Mth.clamp(this.logDetailsScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                            return true;
                        }
                    }
                }
            } else if (this.activeTab == 3) {
                int pX = areaX + 10;
                int pW = areaW - 20;
                int detH = areaH - 44;
                int level = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getPersistentData().getInt("xebEliteMeterLevel") : 0;
                String loreKey = (level >= 10 ? "chat.xeb.mastery.desc.10" : (level >= 7 ? "chat.xeb.mastery.desc.7" : (level >= 4 ? "chat.xeb.mastery.desc.4" : (level >= 1 ? "chat.xeb.mastery.desc.1" : "gui.xeb.enigma_bios.mastery.level0"))));
                String rawLore = translate(loreKey).replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "");
                List<FormattedText> loreLines = this.font.getSplitter().splitLines("§o" + rawLore, pW - 12, net.minecraft.network.chat.Style.EMPTY);
                String detailKey = (level > 10 ? "gui.xeb.enigma_bios.mastery.detail.overflow" : (level >= 10 ? "gui.xeb.enigma_bios.mastery.detail.level10" : (level >= 7 ? "gui.xeb.enigma_bios.mastery.detail.level7" : (level >= 4 ? "gui.xeb.enigma_bios.mastery.detail.level4" : (level >= 1 ? "gui.xeb.enigma_bios.mastery.detail.level1" : "gui.xeb.enigma_bios.mastery.detail.level0")))));
                String detailText = level > 10 ? translate(detailKey, (level - 10)) : translate(detailKey);
                List<FormattedText> detailLines = this.font.getSplitter().splitLines(detailText, pW - 12, net.minecraft.network.chat.Style.EMPTY);

                int totalH = 12 + loreLines.size() * 9 + 14 + detailLines.size() * 10 + 6;
                int maxScroll = Math.max(0, totalH - detH);
                if (maxScroll > 0) {
                    this.lastMasteryScrollTime = System.currentTimeMillis();
                    this.masteryScrollAmount = Mth.clamp(this.masteryScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                    return true;
                }
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int startX = this.leftPos + 6;
        int viewportY = this.topPos + 18;
        int viewportH = 130;

        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        // 1. SCROLLBAR PESTAÑAS (IZQUIERDA)
        int totalTabH = 5 * 22;
        int maxTabScroll = Math.max(0, totalTabH - viewportH);
        if (maxTabScroll > 0 && mouseX >= startX + 58 && mouseX <= startX + 66 && mouseY >= viewportY && mouseY <= viewportY + viewportH) {
            this.isDraggingTabScroll = true;
            this.dragStartY = mouseY;
            this.dragStartScroll = this.tabScrollAmount;
            this.lastTabScrollTime = System.currentTimeMillis();
            return true;
        }

        // Pestañas clicks (0..4)
        for (int i = 0; i < 5; i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (mouseX >= startX && mouseX < startX + 58 && mouseY >= y && mouseY < y + 20
                    && y >= viewportY && y + 20 <= viewportY + viewportH) {
                this.activeTab = i;
                this.contentScrollAmount = 0.0F;
                this.analyzerScrollAmount = 0.0F;
                this.headerLoreScrollAmount = 0.0F;
                this.bestiaryListScrollAmount = 0.0F;
                this.bestiaryDetailsScrollAmount = 0.0F;
                this.logListScrollAmount = 0.0F;
                this.logDetailsScrollAmount = 0.0F;
                this.masteryScrollAmount = 0.0F;
                this.scannerScrollAmount = 0.0F;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        // 2. ANALYZER TAB
        if (this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            AnalyzedInfo info = analyzeItem(this.analyzedStack);

            String itemLoreText = translate(info.translationKey + ".enigma_lore");
            if (itemLoreText.equals(info.translationKey + ".enigma_lore") || itemLoreText.isEmpty()) {
                itemLoreText = translate(info.translationKey + ".enigma_effect");
            }
            if (!itemLoreText.isEmpty() && !itemLoreText.startsWith("item.xeb")) {
                List<FormattedText> headerLoreLines = this.font.getSplitter().splitLines("§o" + itemLoreText, areaW - 48, net.minecraft.network.chat.Style.EMPTY);
                int maxHeaderLoreScroll = Math.max(0, headerLoreLines.size() * 9 - 18);
                if (maxHeaderLoreScroll > 0 && mouseX >= areaX + areaW - 8 && mouseX <= areaX + areaW - 2 && mouseY >= areaY + 16 && mouseY <= areaY + 34) {
                    this.isDraggingHeaderLoreScroll = true;
                    this.dragStartY = mouseY;
                    this.dragStartScroll = this.headerLoreScrollAmount;
                    this.lastHeaderLoreScrollTime = System.currentTimeMillis();
                    return true;
                }
            }

            if (info.hasCustomHUD) {
                int hudBtnX = areaX + areaW - 84;
                int hudBtnY = areaY + 4;
                int hudBtnW = 78;
                int hudBtnH = 11;
                if (mouseX >= hudBtnX && mouseX < hudBtnX + hudBtnW && mouseY >= hudBtnY && mouseY < hudBtnY + hudBtnH) {
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.minecraft.setScreen(new HUDPositionScreen(this, this.analyzedStack));
                    }
                    return true;
                }
            }

            if (info.hasAbilities) {
                int btnW = 50;
                int btnH = 14;
                int btnY = areaY + 42;
                for (int b = 0; b < 5; b++) {
                    int bx = areaX + 12 + b * 52;
                    if (mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                        if (info.isAbilityDisabled(b)) {
                            return false;
                        }
                        this.selectedAbilityIndex = b;
                        this.analyzerScrollAmount = 0.0F;
                        this.lastAnalyzerScrollTime = System.currentTimeMillis();
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }

            if (mouseX >= areaX + areaW - 8 && mouseX <= areaX + areaW && mouseY >= areaY + 34 && mouseY <= areaY + areaH - 4) {
                this.isDraggingAnalyzerScroll = true;
                this.dragStartY = mouseY;
                this.dragStartScroll = this.analyzerScrollAmount;
                this.lastAnalyzerScrollTime = System.currentTimeMillis();
                return true;
            }
        }

        // 3. BESTIARIO TAB
        if (this.activeTab == 1) {
            List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
            int listX = areaX + 6;
            int listY = areaY + 6;
            int listW = 95;
            int listH = areaH - 12;

            int maxListScroll = Math.max(0, allBuffs.size() * 16 - (listH - 4));
            if (maxListScroll > 0 && mouseX >= listX + listW - 6 && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
                this.isDraggingBestiaryListScroll = true;
                this.dragStartY = mouseY;
                this.dragStartScroll = this.bestiaryListScrollAmount;
                this.lastBestiaryListScrollTime = System.currentTimeMillis();
                return true;
            }

            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                for (int b = 0; b < allBuffs.size(); b++) {
                    int by = listY + 2 + b * 16 - (int) bestiaryListScrollAmount;
                    if (mouseX >= listX + 2 && mouseX < listX + listW - 6 && mouseY >= by && mouseY < by + 14) {
                        this.selectedBestiaryIndex = b;
                        this.bestiaryDetailsScrollAmount = 0.0F;
                        this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }

            int detX = areaX + 106;
            int detY = areaY + 6;
            int detW = areaW - 112;
            int descStartY = detY + 32;
            int descMaxH = areaH - 40;

            if (!allBuffs.isEmpty() && this.selectedBestiaryIndex >= 0 && this.selectedBestiaryIndex < allBuffs.size()) {
                EliteBuff selBuff = allBuffs.get(this.selectedBestiaryIndex);
                MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];
                String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                if (descText.equals("xeb.buff." + selBuff.getId() + ".desc") || descText.isEmpty()) descText = "Medallón Élite";
                String tierQualityText = getBuffTierQualityDescription(selBuff, tier);
                String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                if (stratText.equals("xeb.buff." + selBuff.getId() + ".counter") || stratText.isEmpty()) stratText = "Estrategia";

                List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> qualityLines = this.font.getSplitter().splitLines(tierQualityText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> counterLines = this.font.getSplitter().splitLines(stratText, detW - 68, net.minecraft.network.chat.Style.EMPTY);

                int totalBestiaryH = (descLines.size() * 10) + 14 + (qualityLines.size() * 10) + 14 + (counterLines.size() * 10) + 8;
                int maxDetailsScroll = Math.max(0, totalBestiaryH - descMaxH);

                if (maxDetailsScroll > 0 && mouseX >= detX + detW - 64 && mouseX <= detX + detW - 56 && mouseY >= descStartY && mouseY <= descStartY + descMaxH) {
                    this.isDraggingBestiaryDetailsScroll = true;
                    this.dragStartY = mouseY;
                    this.dragStartScroll = this.bestiaryDetailsScrollAmount;
                    this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                    return true;
                }
            }

            int renderCenterX = detX + detW - 32;
            int renderCenterY = detY + 36;
            double dist = Math.hypot(mouseX - renderCenterX, mouseY - renderCenterY);
            if (dist <= 36.0) {
                this.selectedBestiaryTierIndex = (this.selectedBestiaryTierIndex + 1) % 3;
                this.bestiaryDetailsScrollAmount = 0.0F;
                this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2F));
                }
                return true;
            }
        }

        // 4. BITÁCORAS TAB CLICKS
        if (this.activeTab == 2) {
            int listX = areaX + 6;
            int listY = areaY + 6;
            int listW = 95;
            int listH = areaH - 12;

            if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
                for (int b = 0; b < logs.size(); b++) {
                    int by = listY + 2 + b * 18 - (int) logListScrollAmount;
                    if (mouseX >= listX + 2 && mouseX < listX + listW - 6 && mouseY >= by && mouseY < by + 16) {
                        this.selectedLogIndex = b;
                        this.logDetailsScrollAmount = 0.0F;
                        this.lastLogDetailsScrollTime = System.currentTimeMillis();
                        if (this.minecraft != null) {
                            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        }
                        return true;
                    }
                }
            }
        }

        // 5. SCANNER TAB CLICKS
        if (this.activeTab == 4) {
            int pX = areaX + 10;
            int pY = areaY + 6;
            int pW = areaW - 20;
            int btnY = pY + 12;
            int btnH = 15;
            org.xeb.xeb.damagenumber.DamageNumberMode[] modes = org.xeb.xeb.damagenumber.DamageNumberMode.values();
            int btnW = (pW - (modes.length - 1) * 4) / modes.length;

            for (int i = 0; i < modes.length; i++) {
                int bx = pX + i * (btnW + 4);
                if (mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
                    org.xeb.xeb.damagenumber.DamageNumberConfig.setMode(modes[i]);
                    if (this.minecraft != null) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    }
                    return true;
                }
            }
        }

        // Inventory Click Analysis
        ItemStack clickedStack = getStackAtMouse(mouseX, mouseY);
        if (!clickedStack.isEmpty()) {
            this.analyzedStack = clickedStack.copy();
            this.activeTab = 0;
            this.analyzerScrollAmount = 0.0F;
            this.headerLoreScrollAmount = 0.0F;
            this.lastAnalyzerScrollTime = System.currentTimeMillis();
            this.lastHeaderLoreScrollTime = 0L;
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
            this.selectedAbilityIndex = info.getFirstEnabledAbilityIndex();

            if (info.translationKey.equals("item.unknown")) {
                this.lastAnalyzedUnknown = true;
                this.lastAnalyzedTime = System.currentTimeMillis();
                this.unknownTextIndex = (int) (Math.random() * 5);
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_DEACTIVATE, 0.5F));
                }
            } else {
                this.lastAnalyzedUnknown = false;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5F));
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int viewportH = 130;
        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        if (this.isDraggingTabScroll) {
            int totalTabH = 5 * 22;
            int maxTabScroll = Math.max(0, totalTabH - viewportH);
            if (maxTabScroll > 0) {
                double deltaY = mouseY - this.dragStartY;
                float scrollDelta = (float) (deltaY * maxTabScroll / (viewportH - 20));
                this.tabScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxTabScroll);
                this.lastTabScrollTime = System.currentTimeMillis();
                return true;
            }
        }

        if (this.isDraggingHeaderLoreScroll && this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            AnalyzedInfo info = analyzeItem(this.analyzedStack);
            String itemLoreText = translate(info.translationKey + ".enigma_lore");
            if (itemLoreText.equals(info.translationKey + ".enigma_lore") || itemLoreText.isEmpty()) {
                itemLoreText = translate(info.translationKey + ".enigma_effect");
            }
            if (!itemLoreText.isEmpty() && !itemLoreText.startsWith("item.xeb")) {
                List<FormattedText> headerLoreLines = this.font.getSplitter().splitLines("§o" + itemLoreText, areaW - 48, net.minecraft.network.chat.Style.EMPTY);
                int maxHeaderLoreScroll = Math.max(0, headerLoreLines.size() * 9 - 18);
                if (maxHeaderLoreScroll > 0) {
                    double deltaY = mouseY - this.dragStartY;
                    float scrollDelta = (float) (deltaY * maxHeaderLoreScroll / 18.0F);
                    this.headerLoreScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxHeaderLoreScroll);
                    this.lastHeaderLoreScrollTime = System.currentTimeMillis();
                    return true;
                }
            }
        }

        if (this.isDraggingAnalyzerScroll && this.activeTab == 0 && !this.analyzedStack.isEmpty()) {
            this.analyzerScrollAmount = Mth.clamp(this.dragStartScroll + (float)(mouseY - this.dragStartY), 0.0F, 500.0F);
            this.lastAnalyzerScrollTime = System.currentTimeMillis();
            return true;
        }

        if (this.isDraggingBestiaryListScroll && this.activeTab == 1) {
            int totalBuffs = EliteBuffRegistry.getAll().size();
            int listH = areaH - 12;
            int maxScroll = Math.max(0, totalBuffs * 16 - (listH - 4));
            if (maxScroll > 0) {
                double deltaY = mouseY - this.dragStartY;
                float scrollDelta = (float) (deltaY * maxScroll / (listH - 16));
                this.bestiaryListScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxScroll);
                this.lastBestiaryListScrollTime = System.currentTimeMillis();
                return true;
            }
        }

        if (this.isDraggingBestiaryDetailsScroll && this.activeTab == 1) {
            List<EliteBuff> allBuffs = new ArrayList<>(EliteBuffRegistry.getAll());
            if (!allBuffs.isEmpty() && this.selectedBestiaryIndex >= 0 && this.selectedBestiaryIndex < allBuffs.size()) {
                EliteBuff selBuff = allBuffs.get(this.selectedBestiaryIndex);
                MedallionType tier = MedallionType.values()[this.selectedBestiaryTierIndex % MedallionType.values().length];
                String descText = translate("xeb.buff." + selBuff.getId() + ".desc");
                if (descText.equals("xeb.buff." + selBuff.getId() + ".desc") || descText.isEmpty()) descText = "Medallón Élite";
                String tierQualityText = getBuffTierQualityDescription(selBuff, tier);
                String stratText = translate("xeb.buff." + selBuff.getId() + ".counter");
                if (stratText.equals("xeb.buff." + selBuff.getId() + ".counter") || stratText.isEmpty()) stratText = "Estrategia";

                int detW = 280 - 112;
                List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> qualityLines = this.font.getSplitter().splitLines(tierQualityText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> counterLines = this.font.getSplitter().splitLines(stratText, detW - 68, net.minecraft.network.chat.Style.EMPTY);

                int descMaxH = areaH - 40;
                int totalH = (descLines.size() * 10) + 14 + (qualityLines.size() * 10) + 14 + (counterLines.size() * 10) + 8;
                int maxScroll = Math.max(0, totalH - descMaxH);
                if (maxScroll > 0) {
                    double deltaY = mouseY - this.dragStartY;
                    float scrollDelta = (float) (deltaY * maxScroll / (descMaxH - 12));
                    this.bestiaryDetailsScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxScroll);
                    this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                    return true;
                }
            }
        }

        if (this.isDraggingLogListScroll && this.activeTab == 2) {
            int maxScroll = Math.max(0, logs.size() * 18 - (areaH - 16));
            if (maxScroll > 0) {
                double deltaY = mouseY - this.dragStartY;
                float scrollDelta = (float) (deltaY * maxScroll / (areaH - 16));
                this.logListScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxScroll);
                this.lastLogListScrollTime = System.currentTimeMillis();
                return true;
            }
        }

        if (this.isDraggingLogDetailsScroll && this.activeTab == 2) {
            if (this.selectedLogIndex >= 0 && this.selectedLogIndex < logs.size()) {
                LogEntry log = logs.get(this.selectedLogIndex);
                int detW = areaW - 112;
                List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), detW - 14, net.minecraft.network.chat.Style.EMPTY);
                int maxScroll = Math.max(0, lines.size() * 10 - (areaH - 32));
                if (maxScroll > 0) {
                    double deltaY = mouseY - this.dragStartY;
                    float scrollDelta = (float) (deltaY * maxScroll / (areaH - 32));
                    this.logDetailsScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxScroll);
                    this.lastLogDetailsScrollTime = System.currentTimeMillis();
                    return true;
                }
            }
        }

        if (this.isDraggingMasteryScroll && this.activeTab == 3) {
            int pW = areaW - 20;
            int detH = areaH - 44;
            int level = this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getPersistentData().getInt("xebEliteMeterLevel") : 0;
            String loreKey = (level >= 10 ? "chat.xeb.mastery.desc.10" : (level >= 7 ? "chat.xeb.mastery.desc.7" : (level >= 4 ? "chat.xeb.mastery.desc.4" : (level >= 1 ? "chat.xeb.mastery.desc.1" : "gui.xeb.enigma_bios.mastery.level0"))));
            String rawLore = translate(loreKey).replaceAll("§[0-9a-fklmnorA-FKLMNOR]", "");
            List<FormattedText> loreLines = this.font.getSplitter().splitLines("§o" + rawLore, pW - 12, net.minecraft.network.chat.Style.EMPTY);
            String detailKey = (level > 10 ? "gui.xeb.enigma_bios.mastery.detail.overflow" : (level >= 10 ? "gui.xeb.enigma_bios.mastery.detail.level10" : (level >= 7 ? "gui.xeb.enigma_bios.mastery.detail.level7" : (level >= 4 ? "gui.xeb.enigma_bios.mastery.detail.level4" : (level >= 1 ? "gui.xeb.enigma_bios.mastery.detail.level1" : "gui.xeb.enigma_bios.mastery.detail.level0")))));
            String detailText = level > 10 ? translate(detailKey, (level - 10)) : translate(detailKey);
            List<FormattedText> detailLines = this.font.getSplitter().splitLines(detailText, pW - 12, net.minecraft.network.chat.Style.EMPTY);

            int totalH = 12 + loreLines.size() * 9 + 14 + detailLines.size() * 10 + 6;
            int maxScroll = Math.max(0, totalH - detH);
            if (maxScroll > 0) {
                double deltaY = mouseY - this.dragStartY;
                float scrollDelta = (float) (deltaY * maxScroll / (detH - 12));
                this.masteryScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxScroll);
                this.lastMasteryScrollTime = System.currentTimeMillis();
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingTabScroll = false;
        this.isDraggingAnalyzerScroll = false;
        this.isDraggingHeaderLoreScroll = false;
        this.isDraggingBestiaryListScroll = false;
        this.isDraggingBestiaryDetailsScroll = false;
        this.isDraggingLogListScroll = false;
        this.isDraggingLogDetailsScroll = false;
        this.isDraggingLogScroll = false;
        this.isDraggingMasteryScroll = false;
        this.isDraggingScannerScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private AnalyzedInfo analyzeItem(ItemStack stack) {
        Item item = stack.getItem();
        String name = stack.getHoverName().getString();

        if (item == ModItems.GOLDEN_FLOWER.get()) {
            return new AnalyzedInfo(name, "item.xeb.golden_flower", true, true,
                    new String[]{"2", "8 c/u", "4", "3 / tick", "15-40"},
                    new String[]{"0.4s", "Charge", "8s", "12s", "300s"},
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.DOOMFIST_V2.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist_v2", true, true,
                    new String[]{"8", "8-15", "6", "0", "88.0"},
                    new String[]{"0.5s", "3s", "6s", "8s", "400s"},
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.DOOMFIST.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist", true, true,
                    new String[]{"10", "6-12", "5", "6", "88.0"},
                    new String[]{"0.5s", "3s", "5s", "6s", "400s"},
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.OPTIC_BLAST.get()) {
            return new AnalyzedInfo(name, "item.xeb.optic_blast", true, true,
                    new String[]{"3", "5 / tick", "4", "6", "56.0"},
                    new String[]{"0.5s", "Energy", "10s", "8s", "300s"},
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.HOLY_DUALITY_BLADE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_duality_blade", true, true,
                    new String[]{"8", "18", "10", "12", ""},
                    new String[]{"Standard", "20s", "10s", "15s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.MECHA_OVERDRIVE.get()) {
            return new AnalyzedInfo(name, "item.xeb.mecha_overdrive", true, true,
                    new String[]{"", "2", "8", "7", ""},
                    new String[]{"", "0s", "4s", "8s", ""},
                    new boolean[]{true, false, false, false, true});
        }
        if (item == ModItems.BROKEN_DIAMOND.get()) {
            return new AnalyzedInfo(name, "item.xeb.broken_diamond", true, true,
                    new String[]{"", "8 / sec", "8", "0", ""},
                    new String[]{"", "Variable", "5s", "15s", ""},
                    new boolean[]{true, false, false, false, true});
        }
        if (item == ModItems.THE_TEARS.get()) {
            return new AnalyzedInfo(name, "item.xeb.the_tears", true, true,
                    new String[]{"4", "8", "5 c/u", "Area", ""},
                    new String[]{"0.4s", "2s", "10s", "15s", ""});
        }
        if (item == ModItems.SMART_HALBERD.get()) {
            return new AnalyzedInfo(name, "item.xeb.smart_halberd", true, false,
                    new String[]{"9", "14", "", "", ""},
                    new String[]{"1.0s", "Target Lunge", "", "", ""},
                    new boolean[]{false, false, true, true, true});
        }
        if (item == ModItems.OMEGA_FLOWERY.get()) {
            return new AnalyzedInfo(name, "item.xeb.omega_flowery", true, false,
                    new String[]{"", "", "", "", "15-40"},
                    new String[]{"", "", "", "", "300s"},
                    new boolean[]{true, true, true, true, false});
        }
        if (item == ModItems.DOGMA.get()) {
            return new AnalyzedInfo(name, "item.xeb.dogma", true, false,
                    new String[]{"", "", "", "", "25.0"},
                    new String[]{"", "", "", "", "200s"},
                    new boolean[]{true, true, true, true, false});
        }
        if (item == ModItems.QUANTUM_CAT_BARRAGE.get()) {
            return new AnalyzedInfo(name, "item.xeb.quantum_cat_barrage", true, false,
                    new String[]{"", "", "", "", "50.0"},
                    new String[]{"", "", "", "", "180s"},
                    new boolean[]{true, true, true, true, false});
        }
        if (item == ModItems.METEOR_STRIKE.get()) {
            return new AnalyzedInfo(name, "item.xeb.meteor_strike", true, false,
                    new String[]{"", "", "", "", "88.0"},
                    new String[]{"", "", "", "", "400s"},
                    new boolean[]{true, true, true, true, false});
        }
        if (item == ModItems.FULL_APERTURE_SUPERNOVA.get()) {
            return new AnalyzedInfo(name, "item.xeb.full_aperture_supernova", true, false,
                    new String[]{"", "", "", "", "56.0"},
                    new String[]{"", "", "", "", "300s"},
                    new boolean[]{true, true, true, true, false});
        }
        if (item == ModItems.JUDGEMENT_CUT.get()) {
            return new AnalyzedInfo(name, "item.xeb.judgement_cut", true, false,
                    new String[]{"", "", "", "", "80.0"},
                    new String[]{"", "", "", "", "300s"},
                    new boolean[]{true, true, true, true, false});
        }
        if (item == ModItems.SOVEREIGN_ARSENAL.get()) {
            return new AnalyzedInfo(name, "item.xeb.sovereign_arsenal", true, false,
                    new String[]{"", "", "", "", "35.0"},
                    new String[]{"", "", "", "", "300s"},
                    new boolean[]{true, true, true, true, false});
        }

        if (item == ModItems.ENIGMA_BIOS.get()) {
            return new AnalyzedInfo(name, "item.xeb.enigma_bios", false, false, null, null);
        }
        if (item == ModItems.MOON_TEAR.get()) {
            return new AnalyzedInfo(name, "item.xeb.moon_tear", false, false, null, null);
        }
        if (item == ModItems.TINFOIL_HAT.get()) {
            return new AnalyzedInfo(name, "item.xeb.tinfoil_hat", false, false, null, null);
        }
        if (item == ModItems.HOLY_MANTLE.get()) {
            return new AnalyzedInfo(name, "item.xeb.holy_mantle", false, false, null, null);
        }
        if (item == ModItems.BRASS_KNUCKLES.get()) {
            return new AnalyzedInfo(name, "item.xeb.brass_knuckles", false, false, null, null);
        }
        if (item == ModItems.DEMON_CORE.get()) {
            return new AnalyzedInfo(name, "item.xeb.demon_core", false, false, null, null);
        }
        if (item == ModItems.MOB_ENERGY.get()) {
            return new AnalyzedInfo(name, "item.xeb.mob_energy", false, false, null, null);
        }

        return new AnalyzedInfo(name, "item.unknown", false, false, null, null);
    }

    private static class LogEntry {
        final String titleKey;
        final String contentKey;

        LogEntry(String titleKey, String contentKey) {
            this.titleKey = titleKey;
            this.contentKey = contentKey;
        }
    }

    private static class AnalyzedInfo {
        final String name;
        final String translationKey;
        final boolean hasAbilities;
        final boolean hasCustomHUD;
        final String[] damages;
        final String[] cooldowns;
        final boolean[] disabledAbilities;

        AnalyzedInfo(String name, String translationKey, boolean hasAbilities, boolean hasCustomHUD, String[] damages, String[] cooldowns) {
            this(name, translationKey, hasAbilities, hasCustomHUD, damages, cooldowns, new boolean[]{false, false, false, false, false});
        }

        AnalyzedInfo(String name, String translationKey, boolean hasAbilities, boolean hasCustomHUD, String[] damages, String[] cooldowns, boolean[] disabledAbilities) {
            this.name = name;
            this.translationKey = translationKey;
            this.hasAbilities = hasAbilities;
            this.hasCustomHUD = hasCustomHUD;
            this.damages = damages;
            this.cooldowns = cooldowns;
            this.disabledAbilities = disabledAbilities;
        }

        public boolean isAbilityDisabled(int index) {
            if (disabledAbilities == null || index < 0 || index >= disabledAbilities.length) {
                return false;
            }
            return disabledAbilities[index];
        }

        public int getFirstEnabledAbilityIndex() {
            if (disabledAbilities != null) {
                for (int i = 0; i < disabledAbilities.length; i++) {
                    if (!disabledAbilities[i]) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }
}
