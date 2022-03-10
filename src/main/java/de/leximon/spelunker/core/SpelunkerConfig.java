package de.leximon.spelunker.core;

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
    public static int blockRadiusMax = (int) Math.pow(16, 2);
    public static int blockRadiusMin = (int) Math.pow(15, 2);
    public static boolean blockTransitions = true;
    public static HashMap<Block, Integer> parsedBlockHighlightColors = new HashMap<>();

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