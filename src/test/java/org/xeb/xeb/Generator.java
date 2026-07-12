package org.xeb.xeb;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Generator {

    @Test
    public void generateJsons() throws IOException {
        Map<String, String> descMap = new HashMap<>();
        descMap.put("spiky", "Reflects damage to attackers when blocking");
        descMap.put("reactive", "Explodes when hit by projectiles or magic");
        descMap.put("damaging", "Deals increased damage and ignore target armor");
        descMap.put("flaming", "Sets targets on fire");
        descMap.put("creepy", "Spawns small helpers when attacking");
        descMap.put("mad", "Goes into rage on low health");
        descMap.put("mirror", "Has chance to replicate target attack style");
        descMap.put("plow", "Breaks blocks and shields when hitting");
        descMap.put("resonant", "Emits shockwaves when landing critical hits");
        descMap.put("sandy", "Blinds and slows targets");
        descMap.put("static", "Deals electric damage on hit");
        
        descMap.put("absorbent", "Absorbs damage and converts to shielding");
        descMap.put("hardy", "Increases defense stats permanently");
        descMap.put("healthy", "Increases maximum health");
        descMap.put("protected", "Gives temporary immunity shield");
        descMap.put("shielded", "Reduces projectile and fall damage");
        descMap.put("tough", "Gives armor toughness");
        descMap.put("undying", "Has chance to cheat death once");
        
        descMap.put("bouncy", "Repels attackers and increases jump height");
        descMap.put("evolving", "Gains stats on each kill");
        descMap.put("lucky", "Gains looting and luck bonus");
        descMap.put("mega", "Increases size and reach");
        descMap.put("speedy", "Increases movement speed");
        descMap.put("sticky", "Slows and traps targets in cobwebs/slime");
        descMap.put("twin", "Attacks twice");
        
        descMap.put("depressing", "Decreases target damage and speed");
        descMap.put("infested", "Spawns silverfish or bees when hit");
        descMap.put("slightly_depressing", "Slows targets slightly");

        Map<String, String> descMapEs = new HashMap<>();
        descMapEs.put("spiky", "Refleja el daño a los atacantes al bloquear");
        descMapEs.put("reactive", "Explota al ser golpeado por proyectiles o magia");
        descMapEs.put("damaging", "Aumenta el daño e ignora la armadura del objetivo");
        descMapEs.put("flaming", "Prende fuego a los objetivos");
        descMapEs.put("creepy", "Invoca pequeños ayudantes al atacar");
        descMapEs.put("mad", "Entra en furia al tener poca vida");
        descMapEs.put("mirror", "Tiene probabilidad de replicar el estilo de ataque del objetivo");
        descMapEs.put("plow", "Rompe bloques y escudos al golpear");
        descMapEs.put("resonant", "Emite ondas de choque al asestar golpes críticos");
        descMapEs.put("sandy", "Ciega y ralentiza a los objetivos");
        descMapEs.put("static", "Inflige daño eléctrico al golpear");
        
        descMapEs.put("absorbent", "Absorbe daño y lo convierte en escudo");
        descMapEs.put("hardy", "Aumenta las estadísticas de defensa permanentemente");
        descMapEs.put("healthy", "Aumenta la vida máxima");
        descMapEs.put("protected", "Otorga un escudo de inmunidad temporal");
        descMapEs.put("shielded", "Reduce el daño por proyectiles y caídas");
        descMapEs.put("tough", "Otorga dureza de armadura");
        descMapEs.put("undying", "Tiene probabilidad de evitar la muerte una vez");
        
        descMapEs.put("bouncy", "Repele a los atacantes e incrementa la altura de salto");
        descMapEs.put("evolving", "Gana estadísticas con cada baja");
        descMapEs.put("lucky", "Otorga bonificación de botín y suerte");
        descMapEs.put("mega", "Incrementa el tamaño y alcance");
        descMapEs.put("speedy", "Incrementa la velocidad de movimiento");
        descMapEs.put("sticky", "Ralentiza y atrapa a los objetivos en telarañas/limo");
        descMapEs.put("twin", "Ataca dos veces");
        
        descMapEs.put("depressing", "Reduce el daño y velocidad del objetivo");
        descMapEs.put("infested", "Invoca lepisma o abejas al recibir daño");
        descMapEs.put("slightly_depressing", "Ralentiza ligeramente a los objetivos");

        Map<String, String> nameMapEs = new HashMap<>();
        nameMapEs.put("spiky", "Espinoso");
        nameMapEs.put("reactive", "Reactivo");
        nameMapEs.put("damaging", "Dañino");
        nameMapEs.put("flaming", "Flamígero");
        nameMapEs.put("creepy", "Escalofriante");
        nameMapEs.put("mad", "Frenético");
        nameMapEs.put("mirror", "Reflector");
        nameMapEs.put("plow", "Rompe-escudos");
        nameMapEs.put("resonant", "Resonante");
        nameMapEs.put("sandy", "Arenoso");
        nameMapEs.put("static", "Estático");
        nameMapEs.put("absorbent", "Absorbente");
        nameMapEs.put("hardy", "Robusto");
        nameMapEs.put("healthy", "Saludable");
        nameMapEs.put("protected", "Protegido");
        nameMapEs.put("shielded", "Escudado");
        nameMapEs.put("tough", "Resistente");
        nameMapEs.put("undying", "Inmortal");
        nameMapEs.put("bouncy", "Rebotador");
        nameMapEs.put("evolving", "Evolutivo");
        nameMapEs.put("lucky", "Afortunado");
        nameMapEs.put("mega", "Mega");
        nameMapEs.put("speedy", "Veloz");
        nameMapEs.put("sticky", "Pegajoso");
        nameMapEs.put("twin", "Gemelo");
        nameMapEs.put("depressing", "Deprimente");
        nameMapEs.put("infested", "Infestado");
        nameMapEs.put("slightly_depressing", "Ligeramente Deprimente");

        File baseDir = new File("src/main/resources");
        
        // Clean up old namespace resources
        deleteDir(new File(baseDir, "data/xeb/tinkering"));
        deleteDir(new File(baseDir, "assets/xeb/models/tool_materials"));

        File modDefDir = new File(baseDir, "data/tconstruct/tinkering/modifiers");
        File modRecDir = new File(baseDir, "data/tconstruct/recipes/modifiers");
        File modelItemDir = new File(baseDir, "assets/xeb/models/item");
        File textureItemDir = new File(baseDir, "assets/xeb/textures/item");
        File langDir = new File(baseDir, "assets/xeb/lang");
        
        // Corrected tags folder: data/tconstruct/tags/items/materials/
        File tconTagItemMaterialDir = new File(baseDir, "data/tconstruct/tags/items/materials");
        File tconRecipeCastingMaterialDir = new File(baseDir, "data/tconstruct/recipes/smeltery/casting/material");
        
        // Corrected definition folder path
        File matDefDir = new File(baseDir, "data/tconstruct/tinkering/materials/definition");
        File matStatsDir = new File(baseDir, "data/tconstruct/tinkering/materials/stats");
        File matTraitsDir = new File(baseDir, "data/tconstruct/tinkering/materials/traits");
        File toolMatModelDir = new File(baseDir, "assets/tconstruct/models/tool_materials");

        modDefDir.mkdirs();
        modRecDir.mkdirs();
        modelItemDir.mkdirs();
        textureItemDir.mkdirs();
        tconTagItemMaterialDir.mkdirs();
        tconRecipeCastingMaterialDir.mkdirs();
        matDefDir.mkdirs();
        matStatsDir.mkdirs();
        matTraitsDir.mkdirs();
        toolMatModelDir.mkdirs();

        // Clean up old incorrect definitions files directly under materials/
        deleteFile(new File(baseDir, "data/tconstruct/tinkering/materials/bronze_elite.json"));
        deleteFile(new File(baseDir, "data/tconstruct/tinkering/materials/silver_elite.json"));
        deleteFile(new File(baseDir, "data/tconstruct/tinkering/materials/gold_elite.json"));
        
        // Clean up old incorrect nested namespace tag directory
        deleteDir(new File(baseDir, "data/tconstruct/tags/items/materials/tconstruct"));

        // ── Generate Material-Item Tags (in standard tconstruct namespace, linking ingots and bits) ──
        writeTagFile(new File(tconTagItemMaterialDir, "bronze_elite.json"), "xeb:bronze_elite_ingot", "xeb:bronze_elite_bit");
        writeTagFile(new File(tconTagItemMaterialDir, "silver_elite.json"), "xeb:silver_elite_ingot", "xeb:silver_elite_bit");
        writeTagFile(new File(tconTagItemMaterialDir, "gold_elite.json"), "xeb:gold_elite_ingot", "xeb:gold_elite_bit");

        // ── Generate Material Fluid Recipes ──
        writeMaterialFluidFile(new File(tconRecipeCastingMaterialDir, "bronze_elite.json"), "xeb:molten_bronze_elite", 700, "tconstruct:bronze_elite");
        writeMaterialFluidFile(new File(tconRecipeCastingMaterialDir, "silver_elite.json"), "xeb:molten_silver_elite", 950, "tconstruct:silver_elite");
        writeMaterialFluidFile(new File(tconRecipeCastingMaterialDir, "gold_elite.json"), "xeb:molten_gold_elite", 1200, "tconstruct:gold_elite");

        // ── Generate Material Definitions directly inside tinkering/materials/definition/ ──
        // Bronze Elite
        writeFile(new File(matDefDir, "bronze_elite.json"), 
                  "{\n  \"craftable\": false,\n  \"fluid\": \"xeb:molten_bronze_elite\",\n  \"fallback\": \"tconstruct:bronze\"\n}");
        writeFile(new File(matStatsDir, "bronze_elite.json"), 
                  "{\n  \"material\": \"tconstruct:bronze_elite\",\n  \"stats\": {\n" +
                  "    \"tconstruct:head\": {\n" +
                  "      \"durability\": 450,\n" +
                  "      \"mining_speed\": 6.5,\n" +
                  "      \"mining_tier\": \"minecraft:diamond\",\n" +
                  "      \"attack_damage\": 2.5\n" +
                  "    },\n" +
                  "    \"tconstruct:handle\": {\n" +
                  "      \"durability\": 1.1,\n" +
                  "      \"mining_speed\": 1.0,\n" +
                  "      \"attack_speed\": 1.0,\n" +
                  "      \"attack_damage\": 1.0\n" +
                  "    },\n" +
                  "    \"tconstruct:extra\": {\n" +
                  "      \"durability\": 100\n" +
                  "    }\n  }\n}");
        writeFile(new File(matTraitsDir, "bronze_elite.json"), 
                  "{\n  \"material\": \"tconstruct:bronze_elite\",\n  \"traits\": [\n    {\n      \"name\": \"tconstruct:spiked_conditioning\",\n      \"level\": 1\n    }\n  ]\n}");

        // Silver Elite
        writeFile(new File(matDefDir, "silver_elite.json"), 
                  "{\n  \"craftable\": false,\n  \"fluid\": \"xeb:molten_silver_elite\",\n  \"fallback\": \"tconstruct:iron\"\n}");
        writeFile(new File(matStatsDir, "silver_elite.json"), 
                  "{\n  \"material\": \"tconstruct:silver_elite\",\n  \"stats\": {\n" +
                  "    \"tconstruct:head\": {\n" +
                  "      \"durability\": 900,\n" +
                  "      \"mining_speed\": 8.5,\n" +
                  "      \"mining_tier\": \"minecraft:netherite\",\n" +
                  "      \"attack_damage\": 3.5\n" +
                  "    },\n" +
                  "    \"tconstruct:handle\": {\n" +
                  "      \"durability\": 1.2,\n" +
                  "      \"mining_speed\": 1.1,\n" +
                  "      \"attack_speed\": 1.05,\n" +
                  "      \"attack_damage\": 1.1\n" +
                  "    },\n" +
                  "    \"tconstruct:extra\": {\n" +
                  "      \"durability\": 150\n" +
                  "    }\n  }\n}");
        writeFile(new File(matTraitsDir, "silver_elite.json"), 
                  "{\n  \"material\": \"tconstruct:silver_elite\",\n  \"traits\": [\n    {\n      \"name\": \"tconstruct:elite_reflexes\",\n      \"level\": 1\n    }\n  ]\n}");

        // Gold Elite
        writeFile(new File(matDefDir, "gold_elite.json"), 
                  "{\n  \"craftable\": false,\n  \"fluid\": \"xeb:molten_gold_elite\",\n  \"fallback\": \"tconstruct:gold\"\n}");
        writeFile(new File(matStatsDir, "gold_elite.json"), 
                  "{\n  \"material\": \"tconstruct:gold_elite\",\n  \"stats\": {\n" +
                  "    \"tconstruct:head\": {\n" +
                  "      \"durability\": 2200,\n" +
                  "      \"mining_speed\": 12.0,\n" +
                  "      \"mining_tier\": \"minecraft:netherite\",\n" +
                  "      \"attack_damage\": 5.0\n" +
                  "    },\n" +
                  "    \"tconstruct:handle\": {\n" +
                  "      \"durability\": 1.4,\n" +
                  "      \"mining_speed\": 1.2,\n" +
                  "      \"attack_speed\": 1.15,\n" +
                  "      \"attack_damage\": 1.2\n" +
                  "    },\n" +
                  "    \"tconstruct:extra\": {\n" +
                  "      \"durability\": 300\n" +
                  "    }\n  }\n}");
        writeFile(new File(matTraitsDir, "gold_elite.json"), 
                  "{\n  \"material\": \"tconstruct:gold_elite\",\n  \"traits\": [\n    {\n      \"name\": \"tconstruct:elite_slayer\",\n      \"level\": 1\n    }\n  ]\n}");

        // ── Generate Tool Material Render Info files ──
        writeFile(new File(toolMatModelDir, "bronze_elite.json"), "{\n  \"color\": \"B87333\",\n  \"fallbacks\": [\n    \"metal\"\n  ]\n}");
        writeFile(new File(toolMatModelDir, "silver_elite.json"), "{\n  \"color\": \"C0C0C0\",\n  \"fallbacks\": [\n    \"metal\"\n  ]\n}");
        writeFile(new File(toolMatModelDir, "gold_elite.json"), "{\n  \"color\": \"FFD700\",\n  \"fallbacks\": [\n    \"metal\"\n  ]\n}");

        // ── Generate Essences Modifiers JSONs (usable for ALL modifiable tools) & PNGs ──
        for (Map.Entry<String, String> entry : descMap.entrySet()) {
            String buffId = entry.getKey();
            String desc = entry.getValue();

            // 1. Definition JSON - Composable Modifier
            String defJson = "{\n" +
                    "  \"type\": \"tconstruct:composable\",\n" +
                    "  \"modules\": []\n" +
                    "}";
            writeFile(new File(modDefDir, buffId + ".json"), defJson);

            // Determine nugget type based on buffId tier
            String nugget = getNuggetItem(buffId);

            // 2. Recipe JSON (allows all modifiable tools, requires essence + tier nugget + moon tear)
            String recJson = "{\n" +
                    "  \"type\": \"tconstruct:modifier\",\n" +
                    "  \"inputs\": [\n" +
                    "    { \"item\": \"xeb:" + buffId + "_essence\" },\n" +
                    "    { \"item\": \"" + nugget + "\" },\n" +
                    "    { \"item\": \"xeb:moon_tear\" }\n" +
                    "  ],\n" +
                    "  \"tools\": { \"tag\": \"tconstruct:modifiable\" },\n" +
                    "  \"result\": { \"name\": \"tconstruct:" + buffId + "\", \"level\": 1 },\n" +
                    "  \"max_level\": 3,\n" +
                    "  \"slots\": { \"upgrades\": 1 }\n" +
                    "}";
            writeFile(new File(modRecDir, buffId + ".json"), recJson);

            // 3. Item Model JSON
            String modelJson = "{\n" +
                    "  \"parent\": \"minecraft:item/generated\",\n" +
                    "  \"textures\": {\n" +
                    "    \"layer0\": \"xeb:item/" + buffId + "_essence\"\n" +
                    "  }\n" +
                    "}";
            writeFile(new File(modelItemDir, buffId + "_essence.json"), modelJson);

            // 4. PNG Shaded Gem Texture based on category
            Color color = getCategoryColor(buffId);
            writeItemPng(new File(textureItemDir, buffId + "_essence.png"), color);
        }

        // Write the modifier definitions for the 3 traits as well
        writeFile(new File(modDefDir, "spiked_conditioning.json"), "{\n  \"type\": \"tconstruct:composable\",\n  \"modules\": []\n}");
        writeFile(new File(modDefDir, "elite_reflexes.json"), "{\n  \"type\": \"tconstruct:composable\",\n  \"modules\": []\n}");
        writeFile(new File(modDefDir, "elite_slayer.json"), "{\n  \"type\": \"tconstruct:composable\",\n  \"modules\": []\n}");

        // ── Generate Bits (Nuggets), Ingots & Molten Buckets PNGs ──
        writeNuggetPng(new File(textureItemDir, "bronze_elite_bit.png"), new Color(0xcd, 0x7f, 0x32)); // Bronze Nugget
        writeNetheriteIngotPng(new File(textureItemDir, "bronze_elite_ingot.png"), new Color(0xb8, 0x73, 0x33)); // Netherite-style Bronze Ingot
        writeItemPng(new File(textureItemDir, "molten_bronze_elite_bucket.png"), new Color(0xd2, 0x69, 0x1e));

        writeNuggetPng(new File(textureItemDir, "silver_elite_bit.png"), new Color(0xc0, 0xc0, 0xc0)); // Silver Nugget
        writeNetheriteIngotPng(new File(textureItemDir, "silver_elite_ingot.png"), new Color(0xa9, 0xa9, 0xa9)); // Netherite-style Silver Ingot
        writeItemPng(new File(textureItemDir, "molten_silver_elite_bucket.png"), new Color(0xbd, 0xc3, 0xc7));

        writeNuggetPng(new File(textureItemDir, "gold_elite_bit.png"), new Color(0xff, 0xd7, 0x00)); // Gold Nugget
        writeNetheriteIngotPng(new File(textureItemDir, "gold_elite_ingot.png"), new Color(0xda, 0xa5, 0x20)); // Netherite-style Gold Ingot
        writeItemPng(new File(textureItemDir, "molten_gold_elite_bucket.png"), new Color(0xf1, 0xc4, 0x0f));

        // ── Generate/Append English Lang File ──
        File enLangFile = new File(langDir, "en_us.json");
        Map<String, String> enAdditions = new HashMap<>();
        enAdditions.put("item.xeb.bronze_elite_bit", "Bronze Elite Bit");
        enAdditions.put("item.xeb.silver_elite_bit", "Silver Elite Bit");
        enAdditions.put("item.xeb.gold_elite_bit", "Gold Elite Bit");
        enAdditions.put("item.xeb.bronze_elite_ingot", "Bronze Elite Ingot");
        enAdditions.put("item.xeb.silver_elite_ingot", "Silver Elite Ingot");
        enAdditions.put("item.xeb.gold_elite_ingot", "Gold Elite Ingot");
        enAdditions.put("item.xeb.molten_bronze_elite_bucket", "Molten Bronze Elite Bucket");
        enAdditions.put("item.xeb.molten_silver_elite_bucket", "Molten Silver Elite Bucket");
        enAdditions.put("item.xeb.molten_gold_elite_bucket", "Molten Gold Elite Bucket");
        enAdditions.put("item.xeb.essence.buff", "%s Essence");
        enAdditions.put("item.xeb.essence.tier", "Level %s");
        enAdditions.put("creativeTab.xeb.xeb_tinkers", "xEB - Tinkers!");
        enAdditions.put("fluid.xeb.molten_bronze_elite", "Molten Bronze Elite");
        enAdditions.put("fluid.xeb.molten_silver_elite", "Molten Silver Elite");
        enAdditions.put("fluid.xeb.molten_gold_elite", "Molten Gold Elite");
        
        enAdditions.put("modifier.tconstruct.spiked_conditioning", "Spiked Conditioning");
        enAdditions.put("modifier.tconstruct.spiked_conditioning.description", "When blocking with this tool, gain 0.5 armor for 3 seconds");
        enAdditions.put("modifier.tconstruct.elite_reflexes", "Elite Reflexes");
        enAdditions.put("modifier.tconstruct.elite_reflexes.description", "+10% attack speed when below 50% HP");
        enAdditions.put("modifier.tconstruct.elite_slayer", "Elite Slayer");
        enAdditions.put("modifier.tconstruct.elite_slayer.description", "+25% damage against medallion-bearing mobs, -10% against others");
        
        enAdditions.put("material.tconstruct.bronze_elite", "Bronze Elite");
        enAdditions.put("material.tconstruct.silver_elite", "Silver Elite");
        enAdditions.put("material.tconstruct.gold_elite", "Gold Elite");
        for (Map.Entry<String, String> entry : descMap.entrySet()) {
            String buffId = entry.getKey();
            String desc = entry.getValue();
            enAdditions.put("item.xeb.essence." + buffId + ".desc", desc);
            
            // Modifier & Essence name
            String capName = buffId.substring(0, 1).toUpperCase() + buffId.substring(1);
            if (buffId.contains("_")) {
                String[] parts = buffId.split("_");
                capName = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1) + " " +
                          parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
            }
            enAdditions.put("item.xeb." + buffId + "_essence", capName + " Essence");
            enAdditions.put("modifier.tconstruct." + buffId, capName);
            enAdditions.put("modifier.tconstruct." + buffId + ".description", desc);
        }
        appendTranslations(enLangFile, enAdditions);

        // ── Generate/Append Spanish Lang Files ──
        for (String fileStr : new String[]{"es_es.json", "es_mx.json"}) {
            File esLangFile = new File(langDir, fileStr);
            Map<String, String> esAdditions = new HashMap<>();
            esAdditions.put("item.xeb.bronze_elite_bit", "Fragmento de Elite de Bronce");
            esAdditions.put("item.xeb.silver_elite_bit", "Fragmento de Elite de Plata");
            esAdditions.put("item.xeb.gold_elite_bit", "Fragmento de Elite de Oro");
            esAdditions.put("item.xeb.bronze_elite_ingot", "Lingote de Elite de Bronce");
            esAdditions.put("item.xeb.silver_elite_ingot", "Lingote de Elite de Plata");
            esAdditions.put("item.xeb.gold_elite_ingot", "Lingote de Elite de Oro");
            esAdditions.put("item.xeb.molten_bronze_elite_bucket", "Cubo de Bronce Elite Fundido");
            esAdditions.put("item.xeb.molten_silver_elite_bucket", "Cubo de Plata Elite Fundida");
            esAdditions.put("item.xeb.molten_gold_elite_bucket", "Cubo de Oro Elite Fundido");
            esAdditions.put("item.xeb.essence.buff", "Esencia de %s");
            esAdditions.put("item.xeb.essence.tier", "Nivel %s");
            esAdditions.put("creativeTab.xeb.xeb_tinkers", "xEB - Tinkers!");
            esAdditions.put("fluid.xeb.molten_bronze_elite", "Bronce Elite Fundido");
            esAdditions.put("fluid.xeb.molten_silver_elite", "Plata Elite Fundida");
            esAdditions.put("fluid.xeb.molten_gold_elite", "Oro Elite Fundido");
            
            esAdditions.put("modifier.tconstruct.spiked_conditioning", "Condicionamiento Espinoso");
            esAdditions.put("modifier.tconstruct.spiked_conditioning.description", "Al bloquear con esta herramienta, ganas 0.5 de armadura por 3 segundos");
            esAdditions.put("modifier.tconstruct.elite_reflexes", "Reflejos de Elite");
            esAdditions.put("modifier.tconstruct.elite_reflexes.description", "+10% velocidad de ataque al estar por debajo del 50% de vida");
            esAdditions.put("modifier.tconstruct.elite_slayer", "Cazador de Elites");
            esAdditions.put("modifier.tconstruct.elite_slayer.description", "+25% daño contra monstruos con medallón, -10% contra otros");
            
            esAdditions.put("material.tconstruct.bronze_elite", "Bronce Elite");
            esAdditions.put("material.tconstruct.silver_elite", "Plata Elite");
            esAdditions.put("material.tconstruct.gold_elite", "Oro Elite");
            for (Map.Entry<String, String> entry : descMapEs.entrySet()) {
                String buffId = entry.getKey();
                String descEs = entry.getValue();
                esAdditions.put("item.xeb.essence." + buffId + ".desc", descEs);
                
                String spName = nameMapEs.get(buffId);
                esAdditions.put("item.xeb." + buffId + "_essence", "Esencia de " + spName);
                esAdditions.put("modifier.tconstruct." + buffId, spName);
                esAdditions.put("modifier.tconstruct." + buffId + ".description", descEs);
            }
            appendTranslations(esLangFile, esAdditions);
        }

        System.out.println("Generated all JSONs under tconstruct namespace, textures, and updated lang files successfully!");
    }

    private String getNuggetItem(String id) {
        if (id.equals("spiky") || id.equals("reactive") || id.equals("damaging") || id.equals("flaming") ||
            id.equals("creepy") || id.equals("mad") || id.equals("mirror") || id.equals("plow") ||
            id.equals("resonant") || id.equals("sandy") || id.equals("static")) {
            return "xeb:bronze_elite_bit";
        }
        if (id.equals("absorbent") || id.equals("hardy") || id.equals("healthy") || id.equals("protected") ||
            id.equals("shielded") || id.equals("tough") || id.equals("undying") ||
            id.equals("depressing") || id.equals("infested") || id.equals("slightly_depressing")) {
            return "xeb:silver_elite_bit";
        }
        return "xeb:gold_elite_bit";
    }

    private Color getCategoryColor(String id) {
        if (id.equals("spiky") || id.equals("reactive") || id.equals("damaging") || id.equals("flaming") ||
            id.equals("creepy") || id.equals("mad") || id.equals("mirror") || id.equals("plow") ||
            id.equals("resonant") || id.equals("sandy") || id.equals("static")) {
            return new Color(0xe6, 0x39, 0x46);
        }
        if (id.equals("absorbent") || id.equals("hardy") || id.equals("healthy") || id.equals("protected") ||
            id.equals("shielded") || id.equals("tough") || id.equals("undying")) {
            return new Color(0x45, 0x7b, 0x9d);
        }
        if (id.equals("bouncy") || id.equals("evolving") || id.equals("lucky") || id.equals("mega") ||
            id.equals("speedy") || id.equals("sticky") || id.equals("twin")) {
            return new Color(0x2a, 0x9d, 0x8f);
        }
        return new Color(0x6d, 0x59, 0x7a);
    }

    private void writeFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        String path = file.getAbsolutePath();
        if (path.contains("src\\main\\resources")) {
            String buildPath = path.replace("src\\main\\resources", "build\\resources\\main");
            File buildFile = new File(buildPath);
            buildFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(buildFile)) {
                writer.write(content);
            }
        }
    }

    private void deleteDir(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    deleteDir(child);
                }
            }
            file.delete();
        }
        String path = file.getAbsolutePath();
        if (path.contains("src\\main\\resources")) {
            String buildPath = path.replace("src\\main\\resources", "build\\resources\\main");
            File buildFile = new File(buildPath);
            if (buildFile.exists()) {
                if (buildFile.isDirectory()) {
                    for (File child : buildFile.listFiles()) {
                        deleteDir(child);
                    }
                }
                buildFile.delete();
            }
        }
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
        String path = file.getAbsolutePath();
        if (path.contains("src\\main\\resources")) {
            String buildPath = path.replace("src\\main\\resources", "build\\resources\\main");
            File buildFile = new File(buildPath);
            if (buildFile.exists()) {
                buildFile.delete();
            }
        }
    }

    private void writeTagFile(File file, String... items) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"replace\": false,\n  \"values\": [\n");
        for (int i = 0; i < items.length; i++) {
            sb.append("    \"").append(items[i]).append("\"");
            if (i < items.length - 1) {
                sb.append(",\n");
            }
        }
        sb.append("\n  ]\n}");
        writeFile(file, sb.toString());
    }

    private void writeMaterialFluidFile(File file, String fluid, int temp, String output) throws IOException {
        String content = "{\n" +
                "  \"type\": \"tconstruct:material_fluid\",\n" +
                "  \"fluid\": {\n" +
                "    \"name\": \"" + fluid + "\",\n" +
                "    \"amount\": 144\n" +
                "  },\n" +
                "  \"temperature\": " + temp + ",\n" +
                "  \"output\": \"" + output + "\"\n" +
                "}";
        writeFile(file, content);
    }

    private void writeNuggetPng(File file, Color color) throws IOException {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, 16, 16);

        g.setColor(color.darker().darker());
        g.drawRect(5, 6, 6, 4);
        g.drawRect(6, 5, 4, 6);

        g.setColor(color);
        g.fillRect(6, 6, 4, 4);

        g.setColor(color.brighter());
        g.fillRect(6, 6, 1, 1);
        g.fillRect(7, 7, 2, 2);

        g.dispose();
        writePng(img, file);
    }

    private void writeNetheriteIngotPng(File file, Color color) throws IOException {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, 16, 16);

        g.setColor(color.darker().darker());
        g.drawRect(2, 5, 11, 7);
        g.drawRect(4, 3, 7, 11);

        g.setColor(color);
        g.fillRect(3, 6, 9, 5);
        g.fillRect(5, 4, 5, 9);

        g.setColor(color.darker());
        g.drawLine(3, 10, 11, 10);
        g.drawLine(10, 4, 10, 10);

        g.setColor(color.brighter());
        g.drawLine(4, 4, 9, 4);
        g.drawLine(4, 5, 4, 9);

        g.dispose();
        writePng(img, file);
    }

    private void writeItemPng(File file, Color color) throws IOException {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setBackground(new Color(0, 0, 0, 0));
        g.clearRect(0, 0, 16, 16);

        g.setColor(color.darker().darker());
        g.drawRect(2, 4, 11, 8);
        g.drawRect(4, 2, 7, 11);

        g.setColor(color);
        g.fillRect(3, 5, 9, 6);
        g.fillRect(5, 3, 5, 9);

        g.setColor(color.brighter());
        g.fillRect(4, 4, 2, 2);
        g.fillRect(5, 5, 3, 3);

        g.dispose();
        writePng(img, file);
    }

    private void writePng(BufferedImage img, File file) throws IOException {
        ImageIO.write(img, "png", file);
        String path = file.getAbsolutePath();
        if (path.contains("src\\main\\resources")) {
            String buildPath = path.replace("src\\main\\resources", "build\\resources\\main");
            File buildFile = new File(buildPath);
            buildFile.getParentFile().mkdirs();
            ImageIO.write(img, "png", buildFile);
        }
    }

    private void appendTranslations(File langFile, Map<String, String> additions) throws IOException {
        if (!langFile.exists()) return;
        String content = new String(Files.readAllBytes(langFile.toPath()), "UTF-8").trim();
        
        int lastBrace = content.lastIndexOf('}');
        if (lastBrace == -1) return;
        
        String cleanContent = content.substring(0, lastBrace).trim();
        
        boolean endsWithComma = cleanContent.endsWith(",");
        if (endsWithComma) {
            cleanContent = cleanContent.substring(0, cleanContent.length() - 1).trim();
        }
        
        StringBuilder sb = new StringBuilder(cleanContent);
        
        for (Map.Entry<String, String> entry : additions.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            
            String keyPattern = "\"" + key + "\":";
            int keyIdx = sb.indexOf(keyPattern);
            if (keyIdx != -1) {
                int lineEnd = sb.indexOf("\n", keyIdx);
                if (lineEnd == -1) lineEnd = sb.length();
                
                String originalLine = sb.substring(keyIdx, lineEnd);
                boolean hasComma = originalLine.trim().endsWith(",");
                
                String newLine = "\"" + key + "\": \"" + val + "\"" + (hasComma ? "," : "");
                sb.replace(keyIdx, lineEnd, newLine);
            } else {
                sb.append(",\n  \"").append(key).append("\": \"").append(val).append("\"");
            }
        }
        
        sb.append("\n}");
        writeFile(langFile, sb.toString());
    }
}
