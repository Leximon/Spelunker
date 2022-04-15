package de.leximon.spelunker.mixin;

import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Vec3i.class)
public interface Vec3iAccessor {

    @Invoker("setX") Vec3i spelunkerSetX(int x);
    @Invoker("setY") Vec3i spelunkerSetY(int y);
    @Invoker("setZ") Vec3i spelunkerSetZ(int z);

}
