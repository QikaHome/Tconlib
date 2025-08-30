package tk.qikahome.tconlib.modifiers;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.modifiers.ability.fluid.SpillingModifier;
import tk.qikahome.tconlib.QikasTconlibMod;
import tk.qikahome.tconlib.entity.ThrownTool;
import tk.qikahome.tconlib.init.Modifiers;
import tk.qikahome.tconlib.library.ProjectileToolHelper;
import tk.qikahome.tconlib.library.ThrownRenderMode;

public record ToolThrowingModule(ThrownRenderMode render_mode, int durability_cost, boolean can_direct_use,
        String use_anim)
        implements ModifierModule, GeneralInteractionModifierHook {// }, InventoryTickModifierHook {

    public static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider
            .<ToolThrowingModule>defaultHooks(ModifierHooks.GENERAL_INTERACT);
    public static final RecordLoadable<ToolThrowingModule> LOADER = RecordLoadable.create(
            ThrownRenderMode.LOADABLE.nullableField("render_mode", ToolThrowingModule::render_mode),
            IntLoadable.ANY_SHORT.defaultField("durability_cost", 0, ToolThrowingModule::durability_cost),
            BooleanLoadable.DEFAULT.defaultField("can_direct_use", true, ToolThrowingModule::can_direct_use),
            StringLoadable.DEFAULT.defaultField("use_anim", "spear", ToolThrowingModule::use_anim),
            ToolThrowingModule::new);

    @Override
    public RecordLoadable<? extends IHaveLoader> getLoader() {
        return LOADER;
    }

    @Override
    public List<ModuleHook<?>> getDefaultHooks() {
        return DEFAULT_HOOKS;
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        try {
            return UseAnim.valueOf(use_anim.toUpperCase());
        } catch (IllegalArgumentException e) {
            QikasTconlibMod.LOGGER.warn("Fail to get use anim: unknown anim name " + use_anim);
            return UseAnim.NONE;
        }
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand,
            InteractionSource source) {

        player.level().isClientSide();
        ProjectileToolHelper.createProjectileStack(null, 0, null);
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return this.getUseAction(tool, modifier) == UseAnim.NONE ? 4 : 72000;
    }

    /***
     * 
     * @param tool   将要被掷出的工具
     * @param power  弹射物的速度（一般0~2.5）
     * @param entity 发射工具的实体
     */
    public static void shoot(IToolStackView tool, float power, LivingEntity entity, ThrownRenderMode render_mode,
            int durability_cost) {
        Level world = entity.level();
        // other stats now that we know we are shooting
        // velocity determines how far it goes, does not impact damage unlike bows
        float velocity = ConditionalStatModifierHook.getModifiedStat(tool, entity,
                ToolStats.VELOCITY) * power;
        float inaccuracy = ModifierUtil.getInaccuracy(tool, entity);

        // multishot stuff

        float startAngle = ModifiableLauncherItem.getAngleStart(1);
        ToolStack newTool = ((ToolStack) tool).copy();

        if (durability_cost != 0) {
            newTool.addModifier(Modifiers.IS_DUPLICATE.getId(), 1);
            if (tool.getModifierLevel(
                    Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0) {
                tool.getPersistentData().putInt(ToolDuplicateManagerModifier.DupCountLocation,
                        tool.getPersistentData().getInt(ToolDuplicateManagerModifier.DupCountLocation) + 1);
                newTool.getPersistentData().putInt(ToolDuplicateManagerModifier.DupCountLocation,
                        tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) - 1);
            } else {
                tool.setDamage(tool.getDamage() + durability_cost);
                newTool.setDamage(durability_cost);
            }
        } else {
            /*
             * for (ItemStack stack : entity.getAllSlots()) {
             * if (ItemStack.isSameItemSameTags(stack, ((ToolStack) tool).createStack()))
             * stack.setCount(stack.getCount() - 1);
             * }
             */
            ((ToolStack) tool).addModifier(Modifiers.IS_DUPLICATE.getId(), 1);
        }
        ToolUUIDProviderModifier.setUUID(newTool, ToolUUIDProviderModifier.getUUID(tool));
        newTool.rebuildStats();
        ItemStack newStack = newTool.createStack();
        newStack.addTagElement("render_mode", render_mode.toNBT());
        ThrownTool toolEntity = new ThrownTool(world, entity, newStack);
        // toolEntity.getEntityData().set(ThrownTool.RENDER_MODE,render_mode);
        // setup projectile target
        Vec3 upVector = entity.getUpVector(1.0f);
        float angle = startAngle;
        Vector3f targetVector = entity.getViewVector(1.0f).toVector3f()
                .rotate((new Quaternionf()).setAngleAxis(angle * Math.PI / 180F, upVector.x,
                        upVector.y, upVector.z));
        toolEntity.shoot(targetVector.x(), targetVector.y(), targetVector.z(), velocity, inaccuracy);

        // fetch the persistent data for the arrow as modifiers may want to store data
        ModDataNBT arrowData = PersistentDataCapability.getOrWarn(toolEntity);
        // let modifiers set properties
        for (ModifierEntry entry : tool.getModifierList()) {
            entry.getHook(ModifierHooks.PROJECTILE_LAUNCH).onProjectileLaunch(tool, entry,
                    entity, newStack, toolEntity, toolEntity, arrowData, true);
        }

        // finally, fire the projectile
        world.addFreshEntity(toolEntity);
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F,
                1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F)
                        + (angle / 10f));
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        // ScopeModifier.stopScoping(entity);
        Level world = entity.level();
        if (!world.isClientSide) {
            int chargeTime = (int) ((getUseDuration(tool, modifier) - timeLeft)
                    * tool.getStats().get(ToolStats.DRAW_SPEED));
            if (throw_mode == "crossbow" && chargeTime >= 25) {
                tool.getPersistentData().putBoolean(new ResourceLocation("reloaded"), true);
            } else if (throw_mode == "bow" || throw_mode == "trident") {
                float power = -7.5f / chargeTime + 2.6f;
                if (power > 0)
                    shoot(tool, power, entity, render_mode, chargeTime);
            }
        }
        return;
    }
    /*
     * /
     * 
     * @Override
     * public void onInventoryTick(IToolStackView tool, ModifierEntry modifier,
     * Level world, LivingEntity holder,
     * int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
     * if (!world.isClientSide && holder.tickCount % 20 == 0) {
     * ToolUUIDProviderModifier provider = Modifiers.TOOL_UUID_PROVIDER.get();
     * ModifierEntry entry = tool.getModifier(provider);
     * // has a 5% chance of restoring each second per level
     * if (entry.getLevel() == 0) {
     * ((ToolStack) tool).addModifier(Modifiers.TOOL_UUID_PROVIDER.getId(), 1);
     * if (holder instanceof ServerPlayer player) {
     * stack.setCount(0);
     * player.addItem(((ToolStack) tool).createStack());
     * }
     * 
     * }
     * }
     * }
     */
}
