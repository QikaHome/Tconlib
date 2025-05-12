package tk.qikahome.tconlib.entity;

import java.lang.reflect.Method;
import java.util.function.DoubleSupplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tools.logic.ToolEvents;
import tk.qikahome.tconlib.init.Entities;
import tk.qikahome.tconlib.init.Modifiers;
import tk.qikahome.tconlib.modifiers.ToolDuplicateManagerModifier;
import tk.qikahome.tconlib.modifiers.ToolUUIDProviderModifier;
import net.minecraftforge.api.distmarker.Dist;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class ThrownTool extends AbstractArrow implements ItemSupplier {
   public static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(ThrownTool.class,
         EntityDataSerializers.ITEM_STACK);
   public ItemStack toolItem = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct:sword")));
   public boolean dealtDamage;
   public int clientSideReturnTridentTickCount;

   public boolean inGround() {
      return this.inGround;
   }

   @Override
   public Component getDisplayName() {
      if (this.getItem() != null && this.getItem().getHoverName() != null)
         return Component.translatable("entity.qikas_tconlib.thrown_tool_with_tool_name",
               this.getItem().getHoverName());
      return super.getDisplayName();
   }

   public void defineSynchedData() {
      this.getEntityData().define(DATA_ITEM_STACK,
            new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct:sword"))).copyWithCount(1));
      super.defineSynchedData();
   }

   public ThrownTool(EntityType<? extends ThrownTool> entity, Level level) {
      super(entity, level);
      // System.out.println("Try to spawn entity");
   }

   public ThrownTool(Level level, LivingEntity shotter, ItemStack tool) {
      super(Entities.THROWN_TOOL.get(), shotter, level);
      this.setItem(tool.copy());
      // System.out.println("Try to spawn entity");
   }

   public void tick() {
      if (this.inGroundTime > 4) {
         this.dealtDamage = true;
      }

      Entity entity = this.getOwner();
      int i = (byte) (ToolStack.copyFrom(toolItem).getModifierLevel(new ModifierId("qikas_tconlib", "loyalty")));
      if (i > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
         if (!this.isAcceptibleReturnOwner()) {
            if (!this.level().isClientSide && this.pickup == AbstractArrow.Pickup.ALLOWED) {
               this.spawnAtLocation(this.getPickupItem(), 0.1F);
            }

            this.discard();
         } else {
            this.setNoPhysics(true);
            Vec3 vec3 = entity.getEyePosition().subtract(this.position());
            this.setPosRaw(this.getX(), this.getY() + vec3.y * 0.015D * (double) i, this.getZ());
            if (this.level().isClientSide) {
               this.yOld = this.getY();
            }

            double d0 = 0.05D * (double) i;
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(vec3.normalize().scale(d0)));
            if (this.clientSideReturnTridentTickCount == 0) {
               this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
            }

            ++this.clientSideReturnTridentTickCount;
         }
      }

      super.tick();
   }

   public boolean isAcceptibleReturnOwner() {
      Entity entity = this.getOwner();
      if (entity != null && entity.isAlive()) {
         return !(entity instanceof ServerPlayer) || !entity.isSpectator();
      } else {
         return false;
      }
   }

   public ItemStack getPickupItem() {
      return getItem().copy();
   }

   public boolean isFoil() {
      return 0 != ToolStack.copyFrom(this.toolItem).getModifierLevel(new ModifierId("qikas_tconlib", "foil"));
   }

   @Nullable
   public EntityHitResult findHitEntity(Vec3 p_37575_, Vec3 p_37576_) {
      return this.dealtDamage ? null : super.findHitEntity(p_37575_, p_37576_);
   }

   public void onHitBlock(BlockHitResult p_37573_) {
      ProjectileImpactEvent event = new ProjectileImpactEvent(this, p_37573_);
      try {
         // 获取方法对象
         Method method = ToolEvents.class.getDeclaredMethod("projectileHit", ProjectileImpactEvent.class);
         method.setAccessible(true); // 突破访问限制

         // 调用方法（假设 event 是构造好的 ProjectileImpactEvent 实例）
         method.invoke(null, event); // 静态方法，第一个参数传 null
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (event.isCanceled()) {
         this.dealtDamage = true;
         return;
      }
      super.onHitBlock(p_37573_);
   }

   public void onHitEntity(EntityHitResult p_37573_) {
      ProjectileImpactEvent event = new ProjectileImpactEvent(this, p_37573_);
      try {
         // 获取方法对象
         Method method = ToolEvents.class.getDeclaredMethod("projectileHit", ProjectileImpactEvent.class);
         method.setAccessible(true); // 突破访问限制

         // 调用方法（假设 event 是构造好的 ProjectileImpactEvent 实例）
         method.invoke(null, event); // 静态方法，第一个参数传 null
      } catch (Exception e) {
         e.printStackTrace();
      }
      if (event.isCanceled()) {
         this.dealtDamage = true;
         return;
      }
      Entity targetEntity = p_37573_.getEntity();
      LivingEntity attacker = null;
      Entity owner = this.getOwner();
      if (owner != null)
         attacker = ToolAttackUtil.getLivingEntity(owner);
      if (attacker == null)
         return;
      ItemStack itemStack = getItem();
      ToolStack tool = ToolStack.from(itemStack);
      SoundEvent soundevent = SoundEvents.TRIDENT_HIT;
      ToolAttackUtil.attackEntity(tool, attacker, InteractionHand.MAIN_HAND, targetEntity, new DoubleSupplier() {
         public double getAsDouble() {
            return 1;
         }
      }, false);
      this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
      float f1 = 1.0F;
      if (this.level() instanceof ServerLevel && this.level().isThundering() && this.isChanneling()) {
         BlockPos blockpos = targetEntity.blockPosition();
         if (this.level().canSeeSky(blockpos)) {
            LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(this.level());
            if (lightningbolt != null) {
               lightningbolt.moveTo(Vec3.atBottomCenterOf(blockpos));
               lightningbolt.setCause(attacker instanceof ServerPlayer ? (ServerPlayer) attacker : null);
               this.level().addFreshEntity(lightningbolt);
               soundevent = SoundEvents.TRIDENT_THUNDER;
               f1 = 5.0F;
            }
         }
      }
      this.playSound(soundevent, f1, 1.0F);
      if (tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) == 0
            && tool.getModifierLevel(Modifiers.IS_DUPLICATE.getId()) != 0)
         this.discard();
   }

   public boolean isChanneling() {
      return EnchantmentHelper.hasChanneling(this.toolItem);
   }

   public boolean tryPickup(Player player) {
      if (this.pickup == Pickup.ALLOWED) {
         ItemStack itemStack = this.getPickupItem();
         ToolStack toolStack = ToolStack.from(itemStack);
         //System.out.println(toolStack.getModifierLevel(Modifiers.IS_DUPLICATE.getId()));
         if (toolStack.getModifierLevel(Modifiers.IS_DUPLICATE.getId()) != 0) {
            for (ItemStack stack : player.getInventory().items) {
               if (stack.getItem() == itemStack.getItem()) {
                  ToolStack toolStack2 = ToolStack.from(stack);
                  //System.out.println(ToolUUIDProviderModifier.getUUID(toolStack) + " and "
                  //      + ToolUUIDProviderModifier.getUUID(toolStack2));
                  if (ToolUUIDProviderModifier.getUUID(toolStack).toString().equals(ToolUUIDProviderModifier
                        .getUUID(toolStack2).toString())) {
                     //System.out.println("find same item");
                     if (toolStack.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0) {
                        toolStack2.setDamage(Math.max(toolStack2.getDamage(), toolStack.getDamage()));
                        ToolDuplicateManagerModifier.setDupCount(toolStack2,
                              ToolDuplicateManagerModifier.getDupCount(toolStack)
                                    + ToolDuplicateManagerModifier.getDupCount(toolStack2));
                     } else
                        toolStack2.setDamage(toolStack2.getDamage() - toolStack.getDamage() - 1);
                     toolStack2.updateStack(stack);
                     player.getInventory().setChanged();
                     return true;
                  }
               }
            }
            ItemStack stack = player.getInventory().offhand.get(0);
            if (true) {
               if (stack.getItem() == itemStack.getItem()) {
                  ToolStack toolStack2 = ToolStack.from(stack);
                  //System.out.println(ToolUUIDProviderModifier.getUUID(toolStack) + " and "
                  //      + ToolUUIDProviderModifier.getUUID(toolStack2));
                  if (ToolUUIDProviderModifier.getUUID(toolStack).toString().equals(ToolUUIDProviderModifier
                        .getUUID(toolStack2).toString())) {
                     //System.out.println("find same item");
                     if (toolStack.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0) {
                        toolStack2.setDamage(Math.max(toolStack2.getDamage(), toolStack.getDamage()));
                        ToolDuplicateManagerModifier.setDupCount(toolStack2,
                              ToolDuplicateManagerModifier.getDupCount(toolStack)
                                    + ToolDuplicateManagerModifier.getDupCount(toolStack2));
                     } else
                        toolStack2.setDamage(toolStack2.getDamage() - toolStack.getDamage());
                     toolStack2.updateStack(stack);
                     player.getInventory().setChanged();
                     return true;
                  }
               }
            }
            //System.out.println("for is over");
            if (toolStack.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) == 0)
               return false;
         }
      }
      return super.tryPickup(player);
   }

   public SoundEvent getDefaultHitGroundSoundEvent() {
      return SoundEvents.TRIDENT_HIT_GROUND;
   }

   public void playerTouch(Player p_37580_) {
      if (this.ownedBy(p_37580_) || this.getOwner() == null) {
         super.playerTouch(p_37580_);
      }

   }

   public void readAdditionalSaveData(CompoundTag p_37578_) {
      // System.out.println("Read Additional Save Data " + p_37578_.getAsString());
      if (p_37578_.contains("item", 10)) {
         this.setItem(ItemStack.of(p_37578_.getCompound("item")));
      }
      super.readAdditionalSaveData(p_37578_);
   }

   public void addAdditionalSaveData(CompoundTag p_37582_) {
      super.addAdditionalSaveData(p_37582_);
      p_37582_.put("item", this.toolItem.save(new CompoundTag()));
      p_37582_.putBoolean("DealtDamage", this.dealtDamage);
   }

   public void tickDespawn() {
      int i = (byte) (ToolStack.copyFrom(toolItem).getModifierLevel(new ModifierId("qikas_tconlib", "loyalty")));
      if (this.pickup != AbstractArrow.Pickup.ALLOWED || i <= 0) {
         super.tickDespawn();
      }

   }

   public void setItem(ItemStack p_37447_) {
      // System.out.println("Set item tag" + p_37447_.getTag().getAsString());
      this.toolItem = p_37447_.copy();
      this.getEntityData().set(DATA_ITEM_STACK, toolItem);
   }

   public float getWaterInertia() {
      return super.getWaterInertia();
   }

   public boolean shouldRender(double p_37588_, double p_37589_, double p_37590_) {
      return true;
   }

   public ItemStack getItem() {
      toolItem = this.getEntityData().get(DATA_ITEM_STACK);
      // if (this.level().isClientSide)
      // System.out.println("Client get item " + Item.getId(item.getItem()));
      return toolItem;
   }

}