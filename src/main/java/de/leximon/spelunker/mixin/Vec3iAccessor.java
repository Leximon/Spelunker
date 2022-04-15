package de.leximon.spelunker.mixin;

import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Vec3i.class)
public interface Vec3iAccessor {

    @Accessor("x") void spelunkerSetX(int x);
    @Accessor("y") void spelunkerSetY(int y);
    @Accessor("z") void spelunkerSetZ(int z);

}
