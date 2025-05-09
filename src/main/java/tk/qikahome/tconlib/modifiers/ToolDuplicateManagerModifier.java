package tk.qikahome.tconlib.modifiers;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.IncrementalModifierEntry;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import tk.qikahome.tconlib.QikasTconlibMod;
import net.minecraft.nbt.Tag;

public class ToolDuplicateManagerModifier extends Modifier
        implements ModifierRemovalHook, InventoryTickModifierHook {
    public static String localId = "tool_duplicate_manager";
    public static ResourceLocation UUIDLocation = new ResourceLocation(QikasTconlibMod.MODID, "tool_uuid");
    public static ResourceLocation DupCountLocation = new ResourceLocation(QikasTconlibMod.MODID, "tool_dup_count");
    /*
     * @Override
     * public ModifierId getId() {
     * return new ModifierId(QikasTconlibMod.MODID, "tool_duplicate_count");
     * }
     */

    @Override
    public Component getDisplayName(IToolStackView tool, ModifierEntry entry, @Nullable RegistryAccess access) {
        return IncrementalModifierEntry.addAmountToName(getDisplayName(entry.getLevel()),
                tool.getPersistentData().getInt(DupCountLocation),
                entry.getLevel());
    }

    public Component getDisplayName(int level) {
        return getDisplayName();
    }

    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    @Nullable
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        ToolDataNBT data = (ToolDataNBT) tool.getPersistentData();
        if (data.getInt(DupCountLocation) < tool.getModifierLevel(modifier))
            return Component.translatable("message." + QikasTconlibMod.MODID + ".modifierRemoveError.badToolDupCount");
        data.remove(UUIDLocation);
        data.remove(DupCountLocation);
        return null;
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder,
            int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        ToolStack toolStack = (ToolStack) tool;
        if (!toolStack.getPersistentData().contains(UUIDLocation, Tag.TAG_STRING)) {
            toolStack.getPersistentData().putString(UUIDLocation, UUID.randomUUID().toString());
        }
        if (!toolStack.getPersistentData().contains(DupCountLocation, Tag.TAG_INT)) {
            toolStack.getPersistentData().putInt(DupCountLocation, modifier.getLevel());
        }
    }

}