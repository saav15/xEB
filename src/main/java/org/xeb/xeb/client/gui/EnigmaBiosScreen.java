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

import java.util.ArrayList;
import java.util.List;

public class EnigmaBiosScreen extends Screen {
    private final int guiWidth = 360;
    private final int guiHeight = 260;
    private int leftPos;
    private int topPos;

    private int activeTab = 0; // 0: Analyzer, 1: Bestiary, 2-6: Bitácoras 1-5
    private ItemStack analyzedStack = ItemStack.EMPTY;
    private int selectedAbilityIndex = 0;

    private int selectedBestiaryIndex = 0;
    private int selectedBestiaryTierIndex = 0; // 0: BRONZE, 1: SILVER, 2: GOLD

    private final List<LogEntry> logs = new ArrayList<>();

    // Scroll Amounts
    private float tabScrollAmount = 0.0F;
    private float contentScrollAmount = 0.0F;
    private float analyzerScrollAmount = 0.0F;
    private float headerLoreScrollAmount = 0.0F;
    private float bestiaryListScrollAmount = 0.0F;
    private float bestiaryDetailsScrollAmount = 0.0F;

    // Last Scroll Times for 1-Second Auto-Hiding Scrollbars
    private long lastTabScrollTime = 0L;
    private long lastAnalyzerScrollTime = 0L;
    private long lastHeaderLoreScrollTime = 0L;
    private long lastBestiaryListScrollTime = 0L;
    private long lastBestiaryDetailsScrollTime = 0L;
    private long lastLogScrollTime = 0L;

    // Mouse Dragging States for ALL Scrollbars
    private boolean isDraggingTabScroll = false;
    private boolean isDraggingAnalyzerScroll = false;
    private boolean isDraggingHeaderLoreScroll = false;
    private boolean isDraggingBestiaryListScroll = false;
    private boolean isDraggingBestiaryDetailsScroll = false;
    private boolean isDraggingLogScroll = false;
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
                case "gui.xeb.enigma_bios.tab.log" -> "Bitácora";
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

        // Scanlines Sci-Fi
        renderFuturisticBackgroundScanlines(g, borderColor);

        // Marco Exterior
        g.fill(this.leftPos, this.topPos, this.leftPos + this.guiWidth, this.topPos + 2, borderColor);
        g.fill(this.leftPos, this.topPos + this.guiHeight - 2, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos, this.topPos, this.leftPos + 2, this.topPos + this.guiHeight, borderColor);
        g.fill(this.leftPos + this.guiWidth - 2, this.topPos, this.leftPos + this.guiWidth, this.topPos + this.guiHeight, borderColor);

        // Header Title Bar
        g.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.guiWidth - 4, this.topPos + 16, 0x3300FFCC);
        g.drawString(this.font, "ENIGMA BIOS v1.0", this.leftPos + 8, this.topPos + 6, borderColor, false);
        String statusText = translate("gui.xeb.enigma_bios.status");
        g.drawString(this.font, statusText, this.leftPos + this.guiWidth - 8 - this.font.width(statusText), this.topPos + 6, borderColor, false);

        renderTabs(g, mouseX, mouseY);
        renderContent(g, mouseX, mouseY, borderColor, flashing, elapsed);
        renderInventory(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderFuturisticBackgroundScanlines(GuiGraphics g, int borderColor) {
        long time = System.currentTimeMillis();
        int scanY = (int) ((time % 8000L) / 80.0F * (this.guiHeight / 100.0F));

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
        int totalTabH = (2 + logs.size()) * 22;
        int maxTabScroll = Math.max(0, totalTabH - viewportH);
        this.tabScrollAmount = Mth.clamp(this.tabScrollAmount, 0.0F, maxTabScroll);

        // Barra de scroll de pestañas auto-ocultable en 1s
        renderAutoHidingScrollbar(g, startX + 60, viewportY, 3, viewportH, this.tabScrollAmount, maxTabScroll, totalTabH, this.lastTabScrollTime, this.isDraggingTabScroll);

        g.enableScissor(startX, viewportY, startX + 58, viewportY + viewportH);

        for (int i = 0; i < 2 + logs.size(); i++) {
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
                default -> translate("gui.xeb.enigma_bios.tab.log") + " " + (i - 1);
            };

            g.drawString(this.font, label, startX + 4, y + 6, textColor, false);
        }

        g.disableScissor();
    }

    private void renderContent(GuiGraphics g, int mouseX, int mouseY, int borderColor, boolean flashing, long elapsed) {
        int areaX = this.leftPos + 72;
        int areaY = this.topPos + 18;
        int areaW = 280;
        int areaH = 130;

        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, 0x11000000);
        g.fill(areaX, areaY, areaX + areaW, areaY + 1, borderColor);
        g.fill(areaX, areaY + areaH - 1, areaX + areaW, areaY + areaH, borderColor);
        g.fill(areaX, areaY, areaX + 1, areaY + areaH, borderColor);
        g.fill(areaX + areaW - 1, areaY, areaX + areaW, areaY + areaH, borderColor);

        if (this.activeTab == 0) {
            // ANALYZER TAB
            if (this.analyzedStack.isEmpty()) {
                g.drawString(this.font, translate("gui.xeb.enigma_bios.analyzer.empty"), areaX + 12, areaY + 12, 0xFF888888, false);
            } else {
                AnalyzedInfo info = analyzeItem(this.analyzedStack);

                g.renderFakeItem(this.analyzedStack, areaX + 12, areaY + 8);
                g.renderItemDecorations(this.font, this.analyzedStack, areaX + 12, areaY + 8);

                int nameColor = flashing ? 0xFFFF3333 : 0xFF00FFCC;
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

                    g.fill(hudBtnX, hudBtnY, hudBtnX + hudBtnW, hudBtnY + hudBtnH, btnHov ? 0xCC00FFCC : 0x4400FFCC);
                    g.drawString(this.font, translate("gui.xeb.enigma_bios.hud_pos"), hudBtnX + 4, hudBtnY + 2, btnHov ? 0xFF08111E : 0xFF00FFCC, false);
                }

                g.fill(areaX + 12, areaY + 38, areaX + areaW - 12, areaY + 39, borderColor);

                if (info.hasAbilities) {
                    int btnW = 50;
                    int btnH = 14;
                    int btnY = areaY + 42;

                    String[] labels = new String[]{"L-Click", "R-Click", "Act 1", "Act 2", "Burst"};

                    for (int b = 0; b < 5; b++) {
                        int bx = areaX + 12 + b * 52;
                        boolean isSelected = (this.selectedAbilityIndex == b);
                        boolean isDisabled = info.isAbilityDisabled(b);
                        boolean isHov = !isDisabled && mouseX >= bx && mouseX < bx + btnW && mouseY >= btnY && mouseY < btnY + btnH;

                        int btnBg = isDisabled ? 0x22444444 : (isSelected ? 0xCC00FFCC : (isHov ? 0x4400FFCC : 0x2200FFCC));
                        int btnTxt = isDisabled ? 0xFF666666 : (isSelected ? 0xFF08111E : (isHov ? 0xFFFFFFFF : 0xFF00FFCC));

                        g.fill(bx, btnY, bx + btnW, btnY + btnH, btnBg);
                        g.drawString(this.font, labels[b], bx + (btnW - this.font.width(labels[b])) / 2, btnY + 3, btnTxt, false);
                    }

                    g.fill(areaX + 12, areaY + 58, areaX + areaW - 12, areaY + 59, 0x4400FFCC);

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

                            g.drawString(this.font, "Extreme Burst: " + burstName, areaX + 12, dy, 0xFF00FFCC, false);
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
                            g.drawString(this.font, "Extreme Burst", areaX + 12, detailY, 0xFF00FFCC, false);
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

                        g.drawString(this.font, abName, areaX + 12, dy, 0xFF00FFCC, false);
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
                    int descY = areaY + 38;
                    int descH = areaY + areaH - 6 - descY;

                    List<FormattedText> loreLines = (!itemLoreText.isEmpty() && !itemLoreText.startsWith("item.xeb"))
                            ? this.font.getSplitter().splitLines("§o" + itemLoreText, areaW - 28, net.minecraft.network.chat.Style.EMPTY)
                            : new ArrayList<>();

                    String effectKey = info.translationKey.equals("item.unknown")
                            ? "item.unknown.enigma_effect." + this.unknownTextIndex
                            : info.translationKey + ".enigma_effect";
                    String effectText = translate(effectKey);
                    if (effectText.equals(effectKey) || effectText.isEmpty()) {
                        effectText = translate(info.translationKey + ".enigma_lore");
                    }
                    if (effectText.equals(info.translationKey + ".enigma_lore") || effectText.isEmpty()) {
                        effectText = "Objeto analizado: Sin propiedades de combate reliquia detectadas.";
                    }

                    List<FormattedText> effectLines = this.font.getSplitter().splitLines(effectText, areaW - 28, net.minecraft.network.chat.Style.EMPTY);
                    int loreH = loreLines.isEmpty() ? 0 : (14 + loreLines.size() * 10 + 6);
                    int totalHeight = loreH + effectLines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - descH);
                    this.analyzerScrollAmount = Mth.clamp(this.analyzerScrollAmount, 0.0F, maxScroll);

                    // Barra de scroll interactiva y auto-ocultable para Analyzer
                    renderAutoHidingScrollbar(g, areaX + areaW - 6, descY, 3, descH, this.analyzerScrollAmount, maxScroll, totalHeight, this.lastAnalyzerScrollTime, this.isDraggingAnalyzerScroll);

                    g.enableScissor(areaX + 12, descY, areaX + areaW - 12, areaY + areaH - 4);
                    int dy = descY - (int) analyzerScrollAmount;

                    if (!loreLines.isEmpty()) {
                        g.drawString(this.font, "Lore & Overview:", areaX + 12, dy, 0xFFFFCC00, false);
                        dy += 11;
                        for (FormattedText line : loreLines) {
                            g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFE0E0E0, false);
                            dy += 10;
                        }
                        dy += 3;
                        g.fill(areaX + 12, dy, areaX + areaW - 12, dy + 1, 0x4400FFCC);
                        dy += 6;
                    }

                    g.drawString(this.font, "Passive Effect:", areaX + 12, dy, 0xFF00FFCC, false);
                    dy += 11;
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

                List<FormattedText> descLines = this.font.getSplitter().splitLines(descText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> qualityLines = this.font.getSplitter().splitLines(tierQualityText, detW - 68, net.minecraft.network.chat.Style.EMPTY);
                List<FormattedText> counterLines = this.font.getSplitter().splitLines(stratText, detW - 68, net.minecraft.network.chat.Style.EMPTY);

                int descStartY = detY + 32;
                int descMaxH = areaH - 40;

                int totalBestiaryH = (descLines.size() * 10) + 14 + (qualityLines.size() * 10) + 14 + (counterLines.size() * 10) + 8;
                int maxBestiaryDetailsScroll = Math.max(0, totalBestiaryH - descMaxH);
                this.bestiaryDetailsScrollAmount = Mth.clamp(this.bestiaryDetailsScrollAmount, 0.0F, maxBestiaryDetailsScroll);

                // Barra de Scroll de Detalles de Bestiario
                renderAutoHidingScrollbar(g, detX + detW - 62, descStartY, 3, descMaxH, this.bestiaryDetailsScrollAmount, maxBestiaryDetailsScroll, totalBestiaryH, this.lastBestiaryDetailsScrollTime, this.isDraggingBestiaryDetailsScroll);

                // Renderizado Estructurado con Scissor Estricto (Sin Traslape de Texto)
                g.enableScissor(detX, descStartY, detX + detW - 65, detY + areaH - 4);
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

                g.disableScissor();
            }
        } else {
            // TAB BITÁCORAS
            int index = this.activeTab - 2;
            if (index >= 0 && index < logs.size()) {
                boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                        this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));

                if (!isUnlocked) {
                    g.drawString(this.font, translate("gui.xeb.enigma_bios.log.locked.title"), areaX + 12, areaY + 8, 0xFFFF3333, false);
                    g.fill(areaX + 12, areaY + 19, areaX + areaW - 12, areaY + 20, 0x44FF3333);

                    int textY = areaY + 26;
                    String lockedDesc = translate("gui.xeb.enigma_bios.log.locked.desc");
                    List<FormattedText> lines = this.font.getSplitter().splitLines(lockedDesc, areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    for (FormattedText line : lines) {
                        g.drawString(this.font, line.getString(), areaX + 12, textY, 0xFF777777, false);
                        textY += 10;
                    }
                } else {
                    LogEntry log = logs.get(index);
                    g.drawString(this.font, translate(log.titleKey), areaX + 12, areaY + 8, 0xFF00FFCC, false);
                    g.fill(areaX + 12, areaY + 19, areaX + areaW - 12, areaY + 20, 0x4400FFCC);

                    int textY = areaY + 26;
                    int textH = areaY + areaH - 8 - textY;

                    List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                    int totalHeight = lines.size() * 10;
                    int maxScroll = Math.max(0, totalHeight - textH);

                    // Barra de Scroll de Bitácoras
                    renderAutoHidingScrollbar(g, areaX + areaW - 6, textY, 3, textH, this.contentScrollAmount, maxScroll, totalHeight, this.lastLogScrollTime, this.isDraggingLogScroll);

                    g.enableScissor(areaX + 12, textY, areaX + areaW - 12, areaY + areaH - 8);
                    int dy = textY - (int) contentScrollAmount;
                    for (FormattedText line : lines) {
                        g.drawString(this.font, line.getString(), areaX + 12, dy, 0xFFE0E0E0, false);
                        dy += 10;
                    }
                    g.disableScissor();
                }
            }
        }
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
            int maxTabScroll = Math.max(0, (2 + logs.size()) * 22 - viewportH);
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

                        int totalH = (descLines.size() * 10) + 14 + (qualityLines.size() * 10) + 14 + (counterLines.size() * 10) + 8;
                        int maxScroll = Math.max(0, totalH - (areaH - 40));
                        if (maxScroll > 0) {
                            this.lastBestiaryDetailsScrollTime = System.currentTimeMillis();
                            this.bestiaryDetailsScrollAmount = Mth.clamp(this.bestiaryDetailsScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                            return true;
                        }
                    }
                }
            } else if (this.activeTab >= 2) {
                int index = this.activeTab - 2;
                if (index >= 0 && index < logs.size()) {
                    boolean isUnlocked = this.minecraft != null && this.minecraft.player != null &&
                            this.minecraft.player.getPersistentData().getBoolean("xebUnlockedBitacora" + (index + 1));
                    if (isUnlocked) {
                        LogEntry log = logs.get(index);
                        int textY = areaY + 26;
                        int textH = areaY + areaH - 8 - textY;
                        List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                        int maxScroll = Math.max(0, lines.size() * 10 - textH);
                        if (maxScroll > 0) {
                            this.lastLogScrollTime = System.currentTimeMillis();
                            this.contentScrollAmount = Mth.clamp(this.contentScrollAmount - (float) delta * 10.0F, 0.0F, maxScroll);
                            return true;
                        }
                    }
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
        int totalTabH = (2 + logs.size()) * 22;
        int maxTabScroll = Math.max(0, totalTabH - viewportH);
        if (maxTabScroll > 0 && mouseX >= startX + 58 && mouseX <= startX + 66 && mouseY >= viewportY && mouseY <= viewportY + viewportH) {
            this.isDraggingTabScroll = true;
            this.dragStartY = mouseY;
            this.dragStartScroll = this.tabScrollAmount;
            this.lastTabScrollTime = System.currentTimeMillis();
            return true;
        }

        // Pestañas clicks
        for (int i = 0; i < 2 + logs.size(); i++) {
            int y = viewportY + i * 22 - (int) tabScrollAmount;
            if (mouseX >= startX && mouseX < startX + 58 && mouseY >= y && mouseY < y + 20
                    && y >= viewportY && y + 20 <= viewportY + viewportH) {
                this.activeTab = i;
                this.contentScrollAmount = 0.0F;
                this.analyzerScrollAmount = 0.0F;
                this.headerLoreScrollAmount = 0.0F;
                this.bestiaryListScrollAmount = 0.0F;
                this.bestiaryDetailsScrollAmount = 0.0F;
                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                }
                return true;
            }
        }

        // 2. ANALYZER TAB SCROLLBAR & CLICKS
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

            // Scrollbar Analyzer Drag Trigger
            if (mouseX >= areaX + areaW - 8 && mouseX <= areaX + areaW && mouseY >= areaY + 34 && mouseY <= areaY + areaH - 4) {
                this.isDraggingAnalyzerScroll = true;
                this.dragStartY = mouseY;
                this.dragStartScroll = this.analyzerScrollAmount;
                this.lastAnalyzerScrollTime = System.currentTimeMillis();
                return true;
            }
        }

        // 3. BESTIARIO TAB SCROLLBARS & CLICKS
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

        // 4. BITÁCORAS TAB SCROLLBAR
        if (this.activeTab >= 2) {
            int index = this.activeTab - 2;
            if (index >= 0 && index < logs.size()) {
                LogEntry log = logs.get(index);
                int textY = areaY + 26;
                int textH = areaY + areaH - 8 - textY;
                List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), areaW - 24, net.minecraft.network.chat.Style.EMPTY);
                int maxScroll = Math.max(0, lines.size() * 10 - textH);

                if (maxScroll > 0 && mouseX >= areaX + areaW - 8 && mouseX <= areaX + areaW && mouseY >= textY && mouseY <= textY + textH) {
                    this.isDraggingLogScroll = true;
                    this.dragStartY = mouseY;
                    this.dragStartScroll = this.contentScrollAmount;
                    this.lastLogScrollTime = System.currentTimeMillis();
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
            int totalTabH = (2 + logs.size()) * 22;
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

        if (this.isDraggingLogScroll && this.activeTab >= 2) {
            int index = this.activeTab - 2;
            if (index >= 0 && index < logs.size()) {
                LogEntry log = logs.get(index);
                int textY = areaY + 26;
                int textH = areaY + areaH - 8 - textY;
                List<FormattedText> lines = this.font.getSplitter().splitLines(translate(log.contentKey), 256, net.minecraft.network.chat.Style.EMPTY);
                int maxScroll = Math.max(0, lines.size() * 10 - textH);
                if (maxScroll > 0) {
                    double deltaY = mouseY - this.dragStartY;
                    float scrollDelta = (float) (deltaY * maxScroll / (textH - 12));
                    this.contentScrollAmount = Mth.clamp(this.dragStartScroll + scrollDelta, 0.0F, maxScroll);
                    this.lastLogScrollTime = System.currentTimeMillis();
                    return true;
                }
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
        this.isDraggingLogScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private AnalyzedInfo analyzeItem(ItemStack stack) {
        Item item = stack.getItem();
        String name = stack.getHoverName().getString();

        if (item == ModItems.GOLDEN_FLOWER.get()) {
            return new AnalyzedInfo(name, "item.xeb.golden_flower", true, true,
                    new String[]{"2", "8 c/u", "4", "3 / tick", ""},
                    new String[]{"0.4s", "Charge", "8s", "12s", ""});
        }
        if (item == ModItems.DOOMFIST_V2.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist_v2", true, true,
                    new String[]{"8", "8-15", "6", "0", ""},
                    new String[]{"0.5s", "3s", "6s", "8s", ""},
                    new boolean[]{false, false, false, false, false});
        }
        if (item == ModItems.DOOMFIST.get()) {
            return new AnalyzedInfo(name, "item.xeb.doomfist", true, true,
                    new String[]{"10", "6-12", "5", "6", ""},
                    new String[]{"0.5s", "3s", "5s", "6s", ""},
                    new boolean[]{false, false, false, false, true});
        }
        if (item == ModItems.OPTIC_BLAST.get()) {
            return new AnalyzedInfo(name, "item.xeb.optic_blast", true, true,
                    new String[]{"3", "5 / tick", "4", "6", ""},
                    new String[]{"0.5s", "Energy", "10s", "8s", ""},
                    new boolean[]{false, false, false, false, true});
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
