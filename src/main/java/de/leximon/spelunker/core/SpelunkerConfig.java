package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import de.siphalor.tweed4.data.hjson.HjsonList;
import de.siphalor.tweed4.data.hjson.HjsonObject;
import de.siphalor.tweed4.data.hjson.HjsonSerializer;
import de.siphalor.tweed4.data.hjson.HjsonValue;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpelunkerConfig {

    private static final ArrayList<BlockHighlightEntry> DEFAULT_BLOCK_HIGHLIGHT_COLORS = new ArrayList<>();
    private record BlockHighlightEntry(String color, String[] ids) {}

    static {
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#ffd1bd", new String[] {"minecraft:iron_ore", "minecraft:deepslate_iron_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#eb5e34", new String[]{"minecraft:copper_ore", "minecraft:deepslate_copper_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#505050", new String[]{"minecraft:coal_ore", "minecraft:deepslate_coal_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#fff52e", new String[]{"minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#2ee0ff", new String[]{"minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#2eff35", new String[]{"minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#312eff", new String[]{"minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#ff2e2e", new String[]{"minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"}));
        DEFAULT_BLOCK_HIGHLIGHT_COLORS.add(new BlockHighlightEntry("#ffffff", new String[]{"minecraft:nether_quartz_ore"}));
    }

    public static boolean serverValidating = true;
    private static int effectRadius = 16;
    public static int chunkRadius = 1;
    public static int blockRadiusMax = 16 * 16;
    public static int blockRadiusMin = 15 * 15;
    public static boolean blockTransitions = true;
    public static HashMap<Block, Integer> parsedBlockHighlightColors = new HashMap<>();

    public static int shortPotionChance = 10;
    public static int longPotionChance = 25;
    public static int minRolls = 1, maxRolls = 1;

    public static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "spelunker.hjson");

    public static void createDefaultConfig() throws IOException {
        HjsonObject obj;
        boolean rewrite = false;
        if(CONFIG_FILE.exists()) {
            InputStream in = new FileInputStream(CONFIG_FILE);
            obj = HjsonSerializer.INSTANCE.readValue(in).asObject();
            in.close();
        } else {
            obj = HjsonSerializer.INSTANCE.newObject();
            rewrite = true;
        }

        if(!obj.hasBoolean("server-validating")) {
            obj.set("server-validating", serverValidating).setComment("""
                    Checks serverside for blocks to be highlighted and sends them to the client
                    recommended if the server has an anti-xray mod
                    default: true
                    """);
            rewrite = true;
        }
        if(!obj.hasInt("effect-radius")) {
            obj.set("effect-radius", effectRadius).setComment("""
                    How many blocks the effect should range
                    a higher value than 32 is not recommended
                    default: 16
                    Must be greater or equal to 1
                    """);
            rewrite = true;
        }
        if (!obj.hasBoolean("block-transitions")) {
            obj.set("block-transitions", blockTransitions).setComment("default: true");
            rewrite = true;
        }
        if (!obj.has("block-highlight-colors")) {
            HjsonList list = obj.addList("block-highlight-colors");
            list.setComment("The blocks to be highlighted in the specific color");
            for (BlockHighlightEntry entry : DEFAULT_BLOCK_HIGHLIGHT_COLORS) {
                HjsonObject eObj = list.addObject(list.size());
                eObj.set("highlightColor", entry.color());
                HjsonList idList = eObj.addList("blockIds");
                for (String id : entry.ids())
                    idList.set(idList.size(), id);
            }
            rewrite = true;
        }
        {

            HjsonObject lootTableObj;
            if(obj.hasObject("loot-table")) {
                lootTableObj = obj.get("loot-table").asObject();
            } else {
                lootTableObj = obj.addObject("loot-table");
                rewrite = true;
            }
            if (!lootTableObj.hasObject("rolls")) {
                HjsonObject rollsObj = lootTableObj.addObject("rolls");
                rollsObj.setComment("""
                        Defines how often the game will try to generate a potion.
                        Higher values lead to the generation of multiple potions in the same chest.
                        default: 1; 1
                        """);
                rollsObj.set("min", minRolls);
                rollsObj.set("max", maxRolls);
                rewrite = true;
            }

            if(!lootTableObj.hasInt("short-potion-chance")) {
                lootTableObj.set("short-potion-chance", shortPotionChance).setComment("""
                        The chance how likely it is to find a short or long potion in a chest
                        default: 10; 25
                        """);
                rewrite = true;
            }
            if (!lootTableObj.hasInt("long-potion-chance")) {
                lootTableObj.set("long-potion-chance", longPotionChance);
                rewrite = true;
            }
        }

        if(rewrite) {
            CONFIG_FILE.getParentFile().mkdir();

            FileOutputStream out = new FileOutputStream(CONFIG_FILE);
            HjsonSerializer.INSTANCE.writeValue(out, obj);
            out.close();
        }
    }

    public static void loadConfig() throws IOException {
        InputStream in = new FileInputStream(CONFIG_FILE);
        HjsonObject obj = HjsonSerializer.INSTANCE.readValue(in).asObject();
        in.close();

        serverValidating = obj.getBoolean("server-validating", true);
        effectRadius = obj.getInt("effect-radius", 16);
        parseEffectRadius();
        blockTransitions = obj.getBoolean("block-transitions", true);
        HjsonList blockHighlightColorList = obj.get("block-highlight-colors").asList();
        for (HjsonValue value : blockHighlightColorList) {
            HjsonObject blockObj = value.asObject();
            String color = blockObj.getString("highlightColor", "#ffffff");
            List<String> blockIds = new ArrayList<>();
            HjsonList blockIdList = blockObj.get("blockIds").asList();
            for (HjsonValue blockIdValue : blockIdList)
                blockIds.add(blockIdValue.asString());

            int c = TextColor.parse(color).getRgb();
            for (String blockId : blockIds) {
                Optional<Block> optBlock = Registry.BLOCK.getOrEmpty(new Identifier(blockId));
                if(optBlock.isEmpty())
                    SpelunkerMod.LOGGER.error("Unknown block id in config: '{}'", blockId);
                else
                    parsedBlockHighlightColors.put(optBlock.get(), c);
            }
        }

        if(obj.has("loot-table")) {
            HjsonObject lootTableObj = obj.get("loot-table").asObject();
            if(lootTableObj.has("rolls")) {
                HjsonObject rollsObj = lootTableObj.get("rolls").asObject();
                minRolls = rollsObj.getInt("min", 1);
                maxRolls = rollsObj.getInt("max", 1);
            }

            shortPotionChance = Math.max(lootTableObj.getInt("short-potion-chance", 10), 0);
            longPotionChance = Math.max(lootTableObj.getInt("long-potion-chance", 25), 0);
            if (shortPotionChance + longPotionChance >= 100) {
                SpelunkerMod.LOGGER.warn("Short and long potion chances together are greater than 100%.");
                longPotionChance = 100 - shortPotionChance;
                SpelunkerMod.LOGGER.warn("Limited long potion chance to: " + longPotionChance);
            } else if (shortPotionChance + longPotionChance == 0)
                SpelunkerMod.LOGGER.warn("The Spelunker effect cannot be obtained in survival because the potion has 0 chance of generating.");
        }
    }

    public static void writePacket(PacketByteBuf buf) {
        buf.writeBoolean(serverValidating);
        buf.writeVarInt(effectRadius);
        buf.writeVarInt(parsedBlockHighlightColors.size());
        for (Map.Entry<Block, Integer> entry : parsedBlockHighlightColors.entrySet()) {
            Identifier id = Registry.BLOCK.getId(entry.getKey());
            buf.writeIdentifier(id);
            buf.writeInt(entry.getValue());
        }
    }

    public static void readPacket(PacketByteBuf buf) {
        serverValidating = buf.readBoolean();
        effectRadius = buf.readVarInt();
        parseEffectRadius();

        parsedBlockHighlightColors.clear();
        int c = buf.readVarInt();
        for (int i = 0; i < c; i++) {
            Block block = Registry.BLOCK.get(buf.readIdentifier());
            int color = buf.readInt();
            parsedBlockHighlightColors.put(block, color);
        }
    }

    private static void parseEffectRadius() {
        chunkRadius = (int) Math.ceil(effectRadius / 16f);
        blockRadiusMax = (int) Math.pow(effectRadius, 2);
        blockRadiusMin = (int) Math.pow(effectRadius - 1, 2);
    }
}