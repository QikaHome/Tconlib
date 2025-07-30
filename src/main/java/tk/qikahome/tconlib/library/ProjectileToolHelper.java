package tk.qikahome.tconlib.library;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import static slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper.TANK_HELPER;

import java.util.function.Supplier;

import slimeknights.tconstruct.library.tools.capability.inventory.ToolInventoryCapability;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import tk.qikahome.tconlib.QikasTconlibMod;
import tk.qikahome.tconlib.init.Modifiers;
import tk.qikahome.tconlib.modifiers.ToolDuplicateManagerModifier;

public class ProjectileToolHelper {
    /***
     * 创建一个工具栈的投掷副本
     * 
     * @param origin          原始工具栈
     * @param durability_cost 每次投掷花费的耐久度
     * @param just_try        如果为真，则不会修改原始物品栈
     * @return 投掷副本；如果没能创建则返回null
     * 
     */
    public static @Nullable ItemStack createProjectileStack(ItemStack origin, int durability_cost, Boolean just_try) {
        // 错误处理
        if (origin == null) {
            QikasTconlibMod.LOGGER.warn("Are you kidding me? Don't try to throw \"NullPointerException\"!");
            return null;
        }
        if (durability_cost == 0) {
            QikasTconlibMod.LOGGER.warn("Cost no durability, just throw itself, please.");
            return null;
        }
        ToolStack tool = ToolStack.from(origin);
        if (tool == null) {
            QikasTconlibMod.LOGGER.warn(origin.getDisplayName() + " is not a tool, don't throw it please.");
            return null;
        }
        if(tool.isBroken())
        {
            QikasTconlibMod.LOGGER.debug("Tool " + origin.getDisplayName() + " is broken.");
            return null;
        }
        // todo:调用ProjectileHook.beforePrijectileShoot
        if (tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0)// 具有工具副本
        {
            int dup_count = ToolDuplicateManagerModifier.getDupCount(tool);
            if (dup_count == 0)// 工具已用尽
            {
                QikasTconlibMod.LOGGER.debug("Tool " + origin.getDisplayName() + " is used up.");
                return null;
            }
            if (dup_count == 1 && tool.getModifierLevel(Modifiers.IS_DUPLICATE.getId()) > 0)// 投掷自身
            {
                QikasTconlibMod.LOGGER.debug("Will throw " + origin.getDisplayName() + " itself.");
                ItemStack returning = origin.copy();
                if (!just_try)
                    origin.setCount(0);
                return returning;
            }
            // 投掷一个副本
            QikasTconlibMod.LOGGER.debug("Will throw a copy of " + origin.getDisplayName() + ".");
            ToolStack returning = tool.copy();
            ToolDuplicateManagerModifier.setDupCount(returning, 1);
            ToolDuplicateManagerModifier.setDupCount(tool, ToolDuplicateManagerModifier.getDupCount(returning) - 1);
            FluidStack fluid = TANK_HELPER.getFluid(tool);
            if (TANK_HELPER.getCapacity(tool) > 0 && fluid.getAmount() > 0)// 工具存有流体
            {
                TANK_HELPER.setFluid(returning, FluidStack.EMPTY);
                TagKey<Modifier> spilling_like = ModifierManager
                        .getTag(new ResourceLocation("qikas_tconlib:spilling_like"));
                int level = 0;
                for (ModifierEntry modifier : tool.getModifierList()) {
                    if (ModifierManager.isInTag(modifier.getId(), spilling_like))
                        level++;
                }
                FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
                int possible_cost = recipe.getAmount(fluid.getFluid()) * level;
                int amount = fluid.getAmount();
                int amount2 = Math.min(Math.min(amount, possible_cost), TANK_HELPER.getCapacity(tool));
                TANK_HELPER.setFluid(returning, new FluidStack(fluid, amount2));
                if (!just_try)
                    fluid.shrink(amount2);
            }
            ToolInventoryCapability inv = new ToolInventoryCapability((Supplier<? extends IToolStackView>) returning);
            int i = 0;
            while (i < inv.getSlots()) {
                inv.setStackInSlot(i, ItemStack.EMPTY);
            }
            if (!just_try)
                ToolDamageUtil.directDamage(tool, durability_cost, null, null);
            ToolDamageUtil.directDamage(returning, returning.getStats().getInt(ToolStats.DURABILITY) - durability_cost,
                    null, null);
            return returning.createStack();
        } // 不具有工具副本
        QikasTconlibMod.LOGGER.debug("Will throw " + origin.getDisplayName() + " taking durability.");
        ToolStack returning = tool.copy();
        FluidStack fluid = TANK_HELPER.getFluid(tool);
        if (TANK_HELPER.getCapacity(tool) > 0 && fluid.getAmount() > 0)// 工具存有流体
        {
            TANK_HELPER.setFluid(returning, FluidStack.EMPTY);
            TagKey<Modifier> spilling_like = ModifierManager
                    .getTag(new ResourceLocation("qikas_tconlib:spilling_like"));
            int level = 0;
            for (ModifierEntry modifier : tool.getModifierList()) {
                if (ModifierManager.isInTag(modifier.getId(), spilling_like))
                    level++;
            }
            FluidEffects recipe = FluidEffectManager.INSTANCE.find(fluid.getFluid());
            int possible_cost = recipe.getAmount(fluid.getFluid()) * level;
            int amount = fluid.getAmount();
            int amount2 = Math.min(Math.min(amount, possible_cost), TANK_HELPER.getCapacity(tool));
            TANK_HELPER.setFluid(returning, new FluidStack(fluid, amount2));
            if (!just_try)
                fluid.shrink(amount2);
        }
        ToolInventoryCapability inv = new ToolInventoryCapability((Supplier<? extends IToolStackView>) returning);
        int i = 0;
        while (i < inv.getSlots()) {
            inv.setStackInSlot(i, ItemStack.EMPTY);
        }
        if (!just_try)
            ToolDamageUtil.directDamage(tool, durability_cost, null, null);
        ToolDamageUtil.directDamage(returning, returning.getStats().getInt(ToolStats.DURABILITY) - durability_cost,
                null, null);
        return returning.createStack();
    }
}
