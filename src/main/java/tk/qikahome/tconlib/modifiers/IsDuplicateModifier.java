package tk.qikahome.tconlib.modifiers;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import tk.qikahome.tconlib.init.Modifiers;

public class IsDuplicateModifier extends Modifier
        implements InventoryTickModifierHook, ValidateModifierHook, ModifierRemovalHook {
    public static String localId = "is_duplicate";

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder,
            int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) == 0)
            stack.setCount(0);
    }

    @Override
    @Nullable
    public Component validate(IToolStackView tool, ModifierEntry modifier) {
        return Component.translatable("message.qikas_tconlib.is_duplicate.cantEdit");
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    @Nullable
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        return Component.translatable("message.qikas_tconlib.is_duplicate.cantEdit");
    }
}
