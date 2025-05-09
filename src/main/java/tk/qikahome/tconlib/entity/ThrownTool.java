package tk.qikahome.tconlib.entity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import tk.qikahome.tconlib.init.Entities;
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
      return this.toolItem.copy();
   }

   public boolean isFoil() {
      return 0 != ToolStack.copyFrom(this.toolItem).getModifierLevel(new ModifierId("qikas_tconlib", "foil"));
   }

   @Nullable
   public EntityHitResult findHitEntity(Vec3 p_37575_, Vec3 p_37576_) {
      return this.dealtDamage ? null : super.findHitEntity(p_37575_, p_37576_);
   }

   public void onHitEntity(EntityHitResult p_37573_) {
      Entity entity = p_37573_.getEntity();
      float damage = ToolStack.copyFrom(toolItem).getStats().get(ToolStats.ATTACK_DAMAGE);
      if (entity instanceof LivingEntity livingentity) {
         damage += EnchantmentHelper.getDamageBonus(this.toolItem, livingentity.getMobType());
      }
      Entity entity1 = this.getOwner();
      DamageSource damagesource = this.damageSources().trident(this, (Entity) (entity1 == null ? this : entity1));
      this.dealtDamage = true;
      SoundEvent soundevent = SoundEvents.TRIDENT_HIT;
      if (entity.hurt(damagesource, damage)) {
         if (entity.getType() == EntityType.ENDERMAN) {
            return;
         }

         if (entity instanceof LivingEntity) {
            LivingEntity livingentity1 = (LivingEntity) entity;
            if (entity1 instanceof LivingEntity) {
               EnchantmentHelper.doPostHurtEffects(livingentity1, entity1);
               EnchantmentHelper.doPostDamageEffects((LivingEntity) entity1, livingentity1);
            }

            this.doPostHurtEffects(livingentity1);
         }
      }

      this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
      float f1 = 1.0F;
      if (this.level() instanceof ServerLevel && this.level().isThundering() && this.isChanneling()) {
         BlockPos blockpos = entity.blockPosition();
         if (this.level().canSeeSky(blockpos)) {
            LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(this.level());
            if (lightningbolt != null) {
               lightningbolt.moveTo(Vec3.atBottomCenterOf(blockpos));
               lightningbolt.setCause(entity1 instanceof ServerPlayer ? (ServerPlayer) entity1 : null);
               this.level().addFreshEntity(lightningbolt);
               soundevent = SoundEvents.TRIDENT_THUNDER;
               f1 = 5.0F;
            }
         }
      }

      this.playSound(soundevent, f1, 1.0F);
   }

   public boolean isChanneling() {
      return EnchantmentHelper.hasChanneling(this.toolItem);
   }

   public boolean tryPickup(Player p_150196_) {
      return super.tryPickup(p_150196_)
            || this.isNoPhysics() && this.ownedBy(p_150196_) && p_150196_.getInventory().add(this.getPickupItem());
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