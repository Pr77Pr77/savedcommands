package de.aliba2468pr77pr77.savedcommands.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

@Mixin(KeyMapping.class)
public interface KeyBindingAccessor {
    @Accessor("key")
    InputConstants.Key getBoundKey();
}