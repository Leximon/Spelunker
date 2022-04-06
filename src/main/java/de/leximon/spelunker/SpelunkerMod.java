package de.leximon.spelunker;

import de.leximon.spelunker.core.SpelunkerConfig;
import de.leximon.spelunker.mixin.BrewingRecipeRegistryAccessor;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.loot.v1.FabricLootPoolBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.condition.LootConditionTypes;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetPotionLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SpelunkerMod implements ModInitializer {
	public static final String MODID = "spelunker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final Identifier MINESHAFT_LOOT_TABLE = new Identifier("chests/abandoned_mineshaft");
	public static final Identifier AMETHYST_CLUSTER_LOOT_TABLE = new Identifier("blocks/amethyst_cluster");

	public static final Identifier PACKET_ORE_CHUNKS = identifier("ore_chunks");
	public static final Identifier PACKET_CONFIG = identifier("config");

	public static Item AMETHYST_DUST;

	public static StatusEffect STATUS_EFFECT_SPELUNKER;
	public static Potion SPELUNKER_POTION;
	public static Potion LONG_SPELUNKER_POTION;

	@Override
	public void onInitialize() {
		AMETHYST_DUST = Registry.register(Registry.ITEM, identifier("amethyst_dust"), new Item(new FabricItemSettings().group(ItemGroup.MISC)));

		STATUS_EFFECT_SPELUNKER = Registry.register(Registry.STATUS_EFFECT, identifier("spelunker"), new SpelunkerStatusEffect());
		SPELUNKER_POTION = Registry.register(Registry.POTION, identifier("spelunker"), new Potion(new StatusEffectInstance(STATUS_EFFECT_SPELUNKER, 20 * 90)));
		LONG_SPELUNKER_POTION = Registry.register(Registry.POTION, identifier("long_spelunker"), new Potion(new StatusEffectInstance(STATUS_EFFECT_SPELUNKER, 20 * 90 * 2)));

		BrewingRecipeRegistryAccessor.spelunkerRegisterPotionRecipe(Potions.NIGHT_VISION, AMETHYST_DUST, SPELUNKER_POTION);
		BrewingRecipeRegistryAccessor.spelunkerRegisterPotionRecipe(Potions.LONG_NIGHT_VISION, AMETHYST_DUST, LONG_SPELUNKER_POTION);
		BrewingRecipeRegistryAccessor.spelunkerRegisterPotionRecipe(SPELUNKER_POTION, Items.REDSTONE, LONG_SPELUNKER_POTION);

		// load config
		try {
			SpelunkerConfig.createDefaultConfig();
			SpelunkerConfig.loadConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// add potion to mineshafts
		LootTableLoadingCallback.EVENT.register((resourceManager, lootManager, id, table, setter) -> {
			if(MINESHAFT_LOOT_TABLE.equals(id)) {
				LootNumberProvider rollProvider;
				if(SpelunkerConfig.minRolls == SpelunkerConfig.maxRolls)
					rollProvider = ConstantLootNumberProvider.create(SpelunkerConfig.minRolls);
				else rollProvider = UniformLootNumberProvider.create(SpelunkerConfig.minRolls, SpelunkerConfig.maxRolls);
				table.pool(FabricLootPoolBuilder.builder()
						.rolls(rollProvider)
						.with(ItemEntry.builder(Items.POTION)
								.apply(() -> SetPotionLootFunction.builder(SPELUNKER_POTION).build())
								.weight(SpelunkerConfig.shortPotionChance)
						)
						.with(ItemEntry.builder(Items.POTION)
								.apply(() -> SetPotionLootFunction.builder(LONG_SPELUNKER_POTION).build())
								.weight(SpelunkerConfig.longPotionChance)
						)
						.with(ItemEntry.builder(Items.AIR)
								.weight(100 - (SpelunkerConfig.shortPotionChance + SpelunkerConfig.longPotionChance))
						)
				);
			}
			if(AMETHYST_CLUSTER_LOOT_TABLE.equals(id)) {
				table.pool(FabricLootPoolBuilder.builder()
						.rolls(ConstantLootNumberProvider.create(1))
								.withCondition(MatchToolLootCondition.builder(ItemPredicate.Builder.create()
												.enchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, NumberRange.IntRange.ANY)))
										.invert()
										.build()
								)
						.with(ItemEntry.builder(AMETHYST_DUST)
								.weight(1)
						)
						.with(ItemEntry.builder(Items.AIR)
								.weight(9)
						)
				);
			}
		});
	}

	public static Identifier identifier(String name) {
		return new Identifier(MODID, name);
	}

	private static class SpelunkerStatusEffect extends StatusEffect {

		public SpelunkerStatusEffect() {
			super(StatusEffectCategory.BENEFICIAL, 0xffe32e);
		}
	}
}
