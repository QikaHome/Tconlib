package tk.qikahome.tconlib.modifiers;

import java.util.List;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.build.ConditionalStatModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ValidateModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.modifiers.ability.fluid.SpittingModifier;
import slimeknights.tconstruct.tools.modifiers.upgrades.ranged.ScopeModifier;
import slimeknights.tconstruct.tools.modules.OvergrowthModule;
import tk.qikahome.tconlib.QikasTconlibMod;
import tk.qikahome.tconlib.entity.ThrownTool;
import tk.qikahome.tconlib.init.Modifiers;

public record ToolThrowingModule(LevelingInt render_mode, LevelingInt durability_of_dup)
        implements ModifierModule, GeneralInteractionModifierHook, InventoryTickModifierHook {

    public static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider
            .<ToolThrowingModule>defaultHooks(ModifierHooks.GENERAL_INTERACT);
    public static final RecordLoadable<ToolThrowingModule> LOADER = RecordLoadable.create(
            LevelingInt.LOADABLE.requiredField("render_mode", ToolThrowingModule::render_mode),
            LevelingInt.LOADABLE.requiredField("durability_cost", ToolThrowingModule::durability_of_dup),
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
        return UseAnim.SPEAR;
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand,
            InteractionSource source) {
        if ((tool.getStats().getInt(ToolStats.DURABILITY) - tool.getDamage() >= durability_of_dup.flat())
                && !tool.isBroken() && source == InteractionSource.RIGHT_CLICK
                && (tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0
                        ? tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId())
                                - tool.getPersistentData().getInt(ToolDuplicateManagerModifier.DupCountLocation) > 0
                        : true)) {
            // launch if the fluid has effects, cannot simulate as we don't know the target
            // yet
            GeneralInteractionModifierHook.startUsingWithDrawtime(tool, modifier.getId(), player, hand, 1.5f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 72000;
    }

    @Override
    public void onStoppedUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity, int timeLeft) {
        ScopeModifier.stopScoping(entity);
        Level world = entity.level();
        if (!world.isClientSide) {
            int chargeTime = getUseDuration(tool, modifier) - timeLeft;
            if (chargeTime > 3) {
                // other stats now that we know we are shooting
                // velocity determines how far it goes, does not impact damage unlike bows
                float velocity = ConditionalStatModifierHook.getModifiedStat(tool, entity,
                        ToolStats.VELOCITY);
                float inaccuracy = ModifierUtil.getInaccuracy(tool, entity);

                // multishot stuff

                float startAngle = ModifiableLauncherItem.getAngleStart(1);
                ToolStack newTool = ((ToolStack) tool).copy();

                if (durability_of_dup.flat() != 0) {
                    tool.setDamage(tool.getDamage() + durability_of_dup.flat());
                    newTool.setDamage(newTool.getStats().getInt(ToolStats.DURABILITY) - durability_of_dup.flat());
                    newTool.addModifier(Modifiers.IS_DUPLICATE.getId(), 1);
                    if (tool.getModifierLevel(
                            Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0) {
                        tool.getPersistentData().putInt(ToolDuplicateManagerModifier.DupCountLocation,
                                tool.getPersistentData().getInt(ToolDuplicateManagerModifier.DupCountLocation) + 1);
                        newTool.getPersistentData().putInt(ToolDuplicateManagerModifier.DupCountLocation,
                                tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) - 1);
                    }
                } else {
                    for (ItemStack stack : entity.getAllSlots()) {
                        if (ItemStack.isSameItemSameTags(stack, ((ToolStack) tool).createStack()))
                            stack.setCount(stack.getCount() - 1);
                    }
                }

                ItemStack newStack = newTool.createStack();
                newStack.getTag().putInt("thrownRenderMode", render_mode.flat());
                ThrownTool toolEntity = new ThrownTool(world, entity, newStack);

                // setup projectile target
                Vec3 upVector = entity.getUpVector(1.0f);
                float angle = startAngle+15;
                Vector3f targetVector = entity.getViewVector(1.0f).toVector3f()
                        .rotate((new Quaternionf()).setAngleAxis(angle * Math.PI / 180F, upVector.x,
                                upVector.y, upVector.z));
                toolEntity.shoot(targetVector.x(), targetVector.y(), targetVector.z(), velocity, inaccuracy);

                // fetch the persistent data for the arrow as modifiers may want to store data
                ModDataNBT arrowData = PersistentDataCapability.getOrWarn(toolEntity);
                // let modifiers set properties
                for (ModifierEntry entry : tool.getModifierList()) {
                    entry.getHook(ModifierHooks.PROJECTILE_LAUNCH).onProjectileLaunch(tool, entry,
                            entity, toolEntity, null, arrowData, true);
                }

                // finally, fire the projectile
                world.addFreshEntity(toolEntity);
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F,
                        1.0F / (world.getRandom().nextFloat() * 0.4F + 1.2F)
                                + (angle / 10f));
            }
        }

    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder,
            int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if (!world.isClientSide && holder.tickCount % 20 == 0) {
            ToolUUIDProviderModifier provider = Modifiers.TOOL_UUID_PROVIDER.get();
            ModifierEntry entry = tool.getModifier(provider);
            // has a 5% chance of restoring each second per level
            if (entry.getLevel() == 0) {
                ((ToolStack) tool).addModifier(Modifiers.TOOL_UUID_PROVIDER.getId(), 1);
                if (holder instanceof ServerPlayer player) {
                    stack.setCount(0);
                    player.addItem(((ToolStack) tool).createStack());
                }

            }
        }
    }
}
