package de.leximon.spelunker.core;

import com.google.common.base.CaseFormat;
import de.siphalor.tweed4.annotated.AConfigConstraint;
import de.siphalor.tweed4.annotated.AConfigEntry;
import de.siphalor.tweed4.annotated.AConfigExclude;
import de.siphalor.tweed4.annotated.AConfigListener;
import de.siphalor.tweed4.annotated.ATweedConfig;
import de.siphalor.tweed4.config.ConfigEnvironment;
import de.siphalor.tweed4.config.ConfigScope;
import de.siphalor.tweed4.config.constraints.RangeConstraint;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;

@ATweedConfig(serializer = "tweed4:hjson", scope = ConfigScope.WORLD, environment = ConfigEnvironment.SYNCED, casing = CaseFormat.LOWER_HYPHEN)
public class SpelunkerConfig {

    @AConfigEntry(comment = "Checks serverside for blocks to be highlighted and sends them to the client\nrecommended if the server has an anti-xray mod")
    public static boolean serverSideValidating = true;
    @AConfigEntry(comment = "How many blocks the effect should range\na higher value than 32 is not recommended", constraints = @AConfigConstraint(value = RangeConstraint.class, param = "1.."))
    public static int radius = 16;
    public static List<BlockEntry> blockHighlightColors = Arrays.asList(
            new BlockEntry("#ffd1bd", "minecraft:iron_ore", "minecraft:deepslate_iron_ore"),
            new BlockEntry("#eb5e34", "minecraft:copper_ore", "minecraft:deepslate_copper_ore"),
            new BlockEntry("#505050", "minecraft:coal_ore", "minecraft:deepslate_coal_ore"),
            new BlockEntry("#fff52e", "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore"),
            new BlockEntry("#2ee0ff", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"),
            new BlockEntry("#2eff35", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore"),
            new BlockEntry("#312eff", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore"),
            new BlockEntry("#ff2e2e", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore"),
            new BlockEntry("#ffffff", "minecraft:nether_quartz_ore")
    );
    @AConfigEntry(environment = ConfigEnvironment.CLIENT)
    public static boolean transitions = true;

    @AConfigExclude public static HashMap<Block, Integer> parsedBlockHighlightColors = new HashMap<>();
    @AConfigExclude public static int chunkRadius = 1;
    @AConfigExclude public static int blockRadiusMax = (int) Math.pow(16, 2);
    @AConfigExclude public static int blockRadiusMin = (int) Math.pow(15, 2);

    @AConfigListener
    public void reload() {
        chunkRadius = (int) Math.ceil(radius / 16f);
        blockRadiusMax = (int) Math.pow(radius, 2);
        blockRadiusMin = (int) Math.pow(radius - 1, 2);

        parsedBlockHighlightColors.clear();
        for (SpelunkerConfig.BlockEntry blockEntry : blockHighlightColors) {
            int color = HexFormat.fromHexDigits(blockEntry.highlightColor.substring(1));
            for (String blockId : blockEntry.blockIds) {
                Block block = Registry.BLOCK.get(new Identifier(blockId));
                parsedBlockHighlightColors.put(block, color);
            }
        }
    }

    public static class BlockEntry {
        public List<String> blockIds;
        public String highlightColor;

        public BlockEntry() {
        }

        public BlockEntry(String highlightColor, String... blockIds) {
            this.blockIds = Arrays.asList(blockIds);
            this.highlightColor = highlightColor;
        }
    }
}