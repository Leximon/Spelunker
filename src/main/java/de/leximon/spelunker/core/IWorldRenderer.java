package de.leximon.spelunker.core;

import net.minecraft.client.world.ClientWorld;

public interface IWorldRenderer {

    SpelunkyEffectRenderer getSpelunkyEffectRenderer();

    ClientWorld getWorld();

}
