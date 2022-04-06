package de.leximon.spelunker.core;

import de.leximon.spelunker.SpelunkerMod;
import de.siphalor.tweed4.data.hjson.HjsonList;
import de.siphalor.tweed4.data.hjson.HjsonObject;
import de.siphalor.tweed4.data.hjson.HjsonSerializer;
import de.siphalor.tweed4.data.hjson.HjsonValue;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpelunkerConfig {

    public static boolean serverValidating = true;
    private static int effectRadius = 16;
    public static int chunkRadius = 1;
    public static int blockRadiusMax = 16 * 16;
    public static int blockRadiusMin = 15 * 15;
    public static boolean blockTransitions = true;
    public static HashMap<Block, Integer> parsedBlockHighlightColors = new HashMap<>();

    public static int shortPotionChance = 10;
    public static int longPotionChance = 25;
    public static int[] lootTableRolls = new int[2];

    public static final File CONFIG_FILE = new File("config", "spelunker.hjson");

    public static void createDefaultConfigIfNeeded() throws IOException {
        if(CONFIG_FILE.exists())
            return;
        File parent = CONFIG_FILE.getParentFile();
        parent.mkdirs();

        InputStream in = SpelunkerConfig.class.getResourceAsStream("/default_config.hjson");
        if(in == null)
            throw new FileNotFoundException();
        FileOutputStream out = new FileOutputStream(CONFIG_FILE);
        in.transferTo(out);
        in.close();
        out.close();
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
                Block block = Registry.BLOCK.get(new Identifier(blockId));
                parsedBlockHighlightColors.put(block, c);
            }
        }

        HjsonList lootTableRollsArray = obj.get("loottable-rolls").asList();
        if(lootTableRollsArray.size() > 2 || lootTableRollsArray.size() == 0)
            throw new UnsupportedOperationException("LootTable-Rolls can only have one or two values!");
        for (int i = 0; i < lootTableRollsArray.size(); i++)
            lootTableRolls[i] = lootTableRollsArray.get(i).asInt();

        shortPotionChance = Math.max(obj.getInt("short-potion-chance", 10), 0);
        longPotionChance = Math.max(obj.getInt("long-potion-chance", 25), 0);
        if(shortPotionChance + longPotionChance >= 100) {
            SpelunkerMod.LOGGER.warn("Short and long potion chances together are greater than 100%.");
            longPotionChance = 100 - shortPotionChance;
            SpelunkerMod.LOGGER.warn("Limited long potion chance to: " + longPotionChance);
        } else if(shortPotionChance + longPotionChance == 0)
            SpelunkerMod.LOGGER.warn("The Spelunker effect cannot be obtained in survival because the potion has 0 chance of generating.");
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