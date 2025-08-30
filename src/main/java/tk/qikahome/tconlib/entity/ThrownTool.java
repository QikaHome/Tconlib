package tk.qikahome.tconlib.entity;

import net.minecraft.tags.TagKey;
import java.lang.reflect.Method;
import java.rmi.registry.Registry;
import java.util.Dictionary;
import java.util.List;
import java.util.function.DoubleSupplier;
import tk.qikahome.tconlib.init.Modifiers;
import javax.annotation.Nullable;

import org.apache.logging.log4j.spi.ExtendedLogger;
import org.checkerframework.checker.nullness.qual.NonNull;

import tk.qikahome.tconlib.QikasTconlibMod;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagFile;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.common.data.tags.ModifierTagProvider;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectManager;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffects;
import slimeknights.tconstruct.library.modifiers.modules.display.DurabilityBarColorModule;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.logic.ToolEvents;
import tk.qikahome.tconlib.init.Entities;
import tk.qikahome.tconlib.init.Modifiers;
import tk.qikahome.tconlib.init.Stats;
import tk.qikahome.tconlib.modifiers.ToolDuplicateManagerModifier;
import tk.qikahome.tconlib.modifiers.ToolThrowingModule;
import tk.qikahome.tconlib.modifiers.ToolUUIDProviderModifier;
import net.minecraftforge.api.distmarker.Dist;

import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.util.TagHelper;

public class ThrownTool extends AbstractArrow implements ItemSupplier {
   public static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(ThrownTool.class,
         EntityDataSerializers.ITEM_STACK);
   public ItemStack toolItem = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("tconstruct:sword")));
   public boolean dealtDamage;
   public int clientSideReturnTridentTickCount;
   public BlockState lastState;
   public int life;
   public final IntOpenHashSet ignoredEntities = new IntOpenHashSet();

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
      // 获取投掷的工具物品
      ItemStack stack = getItem();

      // 如果工具已经落地超过4tick，标记为已造成伤害（防止重复伤害）
      if (this.inGroundTime > 4) {
         this.dealtDamage = true;
      }

      // 获取投掷者实体
      Entity entity = this.getOwner();
      // 从物品堆栈创建工具对象
      ToolStack tool = ToolStack.from(stack);
      // 获取工具的返回速度属性
      float return_speed = tool.getStats().get(Stats.RETURN_SPEED);

      // 忠诚度返回机制：当有返回速度且满足条件时
      if (return_speed > 0 && (this.dealtDamage || this.isNoPhysics()) && entity != null) {
         // 检查是否可返回给投掷者
         if (!this.isAcceptibleReturnOwner()) {
            // 不可返回时留在原地（无操作）
         } else {
            // 启用无物理效果（忽略碰撞）
            this.setNoPhysics(true);
            // 计算投掷者眼睛位置到当前实体的向量
            Vec3 vec3 = entity.getEyePosition().subtract(this.position());

            // 更新位置：向投掷者方向移动
            this.setPosRaw(
                  this.getX(),
                  this.getY() + vec3.y * 0.015D * (double) return_speed,
                  this.getZ());

            // 客户端更新Y轴旧值
            if (this.level().isClientSide) {
               this.yOld = this.getY();
            }

            // 计算返回速度增量
            double d0 = 0.05D * (double) return_speed;
            // 更新运动向量：95%保持当前速度 + 5%向投掷者方向移动
            this.setDeltaMovement(
                  this.getDeltaMovement().scale(0.95D).add(vec3.normalize().scale(d0)));

            // 播放返回音效（仅首次触发时）
            if (this.clientSideReturnTridentTickCount == 0) {
               // 从NBT获取自定义返回音效
               String soundName = stack.getTag().getCompound("thrownTempData").getString("returnSound");
               ResourceLocation soundId = new ResourceLocation(soundName);
               SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(soundId);

               // 播放音效
               this.playSound(soundEvent, 10.0F, 1.0F);
            }

            // 增加返回计时器
            ++this.clientSideReturnTridentTickCount;
         }
      }

      // 调用父类(Projectile)的tick方法处理基础物理
      ((Projectile) this).tick();

      // 检查是否处于无物理状态
      boolean flag = this.isNoPhysics();
      // 获取当前运动向量
      Vec3 vec3 = this.getDeltaMovement();

      // 初始化旋转角度（如果尚未设置）
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         // 计算水平移动距离
         double d0 = vec3.horizontalDistance();
         // 计算Y轴旋转（偏航角）：基于X和Z方向
         this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * (180F / (float) Math.PI)));
         // 计算X轴旋转（俯仰角）：基于Y方向和水平距离
         this.setXRot((float) (Mth.atan2(vec3.y, d0) * (180F / (float) Math.PI)));
         // 更新旧旋转角度
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }

      // 获取当前位置的方块状态
      BlockPos blockpos = this.blockPosition();
      BlockState blockstate = this.level().getBlockState(blockpos);

      // 碰撞检测：检查是否嵌入方块中
      if (!blockstate.isAir() && !flag) {
         // 获取方块的碰撞形状
         VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
         if (!voxelshape.isEmpty()) {
            Vec3 vec31 = this.position();

            // 检查所有碰撞框是否包含当前实体位置
            for (AABB aabb : voxelshape.toAabbs()) {
               if (aabb.move(blockpos).contains(vec31)) {
                  // 标记为已嵌入地面
                  this.inGround = true;
                  break;
               }
            }
         }
      }

      // 减少震动时间（如果有）
      if (this.shakeTime > 0) {
         --this.shakeTime;
      }

      // 地面嵌入处理
      if (this.inGround && !flag) {
         // 检查是否应该从地面弹出（方块改变或碰撞框变化）
         if (this.lastState != blockstate
               && this.level().noCollision((new AABB(this.position(), this.position())).inflate(0.06D))) {
            // 从地面弹出
            this.inGround = false;
            // 随机化新运动向量
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(vec31.multiply(
                  this.random.nextFloat() * 0.2F,
                  this.random.nextFloat() * 0.2F,
                  this.random.nextFloat() * 0.2F));
            // 重置生命周期
            this.life = 0;
         } else if (!this.level().isClientSide) {
            // 服务端处理消失逻辑
            this.tickDespawn();
         }

         // 增加地面停留时间
         ++this.inGroundTime;
      } else {
         // 重置地面停留时间
         this.inGroundTime = 0;
         // 计算移动路径
         Vec3 vec32 = this.position();
         Vec3 vec33 = vec32.add(vec3);

         // 射线检测：从当前位置到目标位置
         HitResult hitresult = this.level().clip(new ClipContext(
               vec32, vec33,
               ClipContext.Block.COLLIDER,
               ClipContext.Fluid.NONE,
               this));

         // 如果检测到碰撞，更新目标位置为碰撞点
         if (hitresult.getType() != HitResult.Type.MISS) {
            vec33 = hitresult.getLocation();
         }

         // 实体碰撞检测循环
         while (!this.isRemoved()) {
            // 查找路径上的实体
            EntityHitResult entityhitresult = this.findHitEntity(vec32, vec33);
            if (entityhitresult != null) {
               hitresult = entityhitresult;
            }

            // 玩家伤害保护检测
            if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
               Entity entityhit = ((EntityHitResult) hitresult).getEntity();
               // 检查攻击者是否有权限伤害目标
               if (entity instanceof Player && entityhit instanceof Player
                     && !((Player) entity).canHarmPlayer((Player) entityhit)) {
                  // 忽略此碰撞结果
                  hitresult = null;
                  entityhitresult = null;
               }
            }

            // 碰撞事件处理
            if (hitresult != null && hitresult.getType() != HitResult.Type.MISS && !flag) {
               // 处理Forge的投射物碰撞事件
               switch (net.minecraftforge.event.ForgeEventFactory.onProjectileImpactResult(this, hitresult)) {
                  case SKIP_ENTITY:
                     // 跳过实体碰撞但处理其他类型
                     if (hitresult.getType() != HitResult.Type.ENTITY) {
                        this.onHit(hitresult); // 处理碰撞
                        this.hasImpulse = true; // 标记有冲量
                        break;
                     }
                     // 添加实体到忽略列表

                     entityhitresult = null; // 清除实体碰撞结果
                     break;
                  case STOP_AT_CURRENT_NO_DAMAGE:
                     // 立即消失且不造成伤害
                     this.discard();
                     entityhitresult = null;
                     break;
                  case STOP_AT_CURRENT:
                  case DEFAULT:
                     // 默认处理：碰撞并标记冲量
                     this.onHit(hitresult);
                     this.hasImpulse = true;
                     break;
               }
            }

            // 穿透检查：如果没有更多穿透次数则退出循环
            if (entityhitresult == null || this.getPierceLevel() <= 0) {
               break;
            }

            hitresult = null; // 重置碰撞结果继续检测
         }

         // 如果实体已移除则退出
         if (this.isRemoved())
            return;

         // 更新运动向量
         vec3 = this.getDeltaMovement();
         double d5 = vec3.x;
         double d6 = vec3.y;
         double d1 = vec3.z;

         // 暴击粒子效果
         if (this.isCritArrow()) {
            for (int i = 0; i < 4; ++i) {
               this.level().addParticle(
                     ParticleTypes.CRIT,
                     this.getX() + d5 * i / 4.0D,
                     this.getY() + d6 * i / 4.0D,
                     this.getZ() + d1 * i / 4.0D,
                     -d5, -d6 + 0.2D, -d1);
            }
         }

         // 计算新位置
         double d7 = this.getX() + d5;
         double d2 = this.getY() + d6;
         double d3 = this.getZ() + d1;
         double d4 = vec3.horizontalDistance(); // 水平移动距离

         // 旋转更新
         if (flag) {
            // 无物理状态下的旋转计算
            this.setYRot((float) (Mth.atan2(-d5, -d1) * (180F / (float) Math.PI)));
         } else {
            // 正常状态下的旋转计算
            this.setYRot((float) (Mth.atan2(d5, d1) * (180F / (float) Math.PI)));
         }

         // 计算俯仰角
         this.setXRot((float) (Mth.atan2(d6, d4) * (180F / (float) Math.PI)));
         // 平滑旋转过渡
         this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
         this.setYRot(lerpRotation(this.yRotO, this.getYRot()));

         // 阻力系数
         float f = 1 - ToolStack.from(getItem()).getStats().get(Stats.AIRDRAG); // 空气阻力
         float f1 = ToolStack.from(getItem()).getStats().get(Stats.G) / 20; // 重力系数

         // 水中处理
         if (this.isInWater()) {
            // 生成气泡粒子
            for (int j = 0; j < 4; ++j) {
               this.level().addParticle(
                     ParticleTypes.BUBBLE,
                     d7 - d5 * 0.25D,
                     d2 - d6 * 0.25D,
                     d3 - d1 * 0.25D,
                     d5, d6, d1);
            }
            // 使用水中惯性系数
            f = 1 - ToolStack.from(getItem()).getStats().get(Stats.WATERDRAG);
         }

         // 应用阻力
         this.setDeltaMovement(vec3.scale(f));

         // 重力处理
         if (!this.isNoGravity() && !flag) {
            Vec3 vec34 = this.getDeltaMovement();
            this.setDeltaMovement(vec34.x, vec34.y - 0.05F, vec34.z);
         }

         // 更新位置
         this.setPos(d7, d2, d3);
         // 检查是否进入方块内部
         this.checkInsideBlocks();
      }
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
      ItemStack stack = getItem().copy();
      stack.getTag().remove("thrownTempData");
      return stack;
   }

   @Nullable
   public EntityHitResult findHitEntity(Vec3 p_37575_, Vec3 p_37576_) {
      return this.dealtDamage ? null : super.findHitEntity(p_37575_, p_37576_);
   }

   /*
    * @Override
    * protected void onHit(HitResult result) {
    * ProjectileImpactEvent event = new ProjectileImpactEvent(this, result);
    * try {
    * // 获取方法对象
    * Method method = ToolEvents.class.getDeclaredMethod("projectileHit",
    * ProjectileImpactEvent.class);
    * method.setAccessible(true); // 突破访问限制
    * 
    * // 调用方法（假设 event 是构造好的 ProjectileImpactEvent 实例）
    * method.invoke(null, event); // 静态方法，第一个参数传 null
    * } catch (Exception e) {
    * e.printStackTrace();
    * }
    * if (event.isCanceled()) {
    * this.dealtDamage = true;
    * return;
    * }
    * super.onHit(result);
    * }
    */

   public void onHitEntity(EntityHitResult event) {
      Entity targetEntity = event.getEntity();
      LivingEntity attacker = null;
      Entity owner = this.getOwner();
      if (owner != null)
         attacker = ToolAttackUtil.getLivingEntity(owner);
      if (attacker == null)
         return;
      if (this.ignoredEntities.contains(targetEntity.getId()))
         return;
      ignoredEntities.add(targetEntity.getId());
      ItemStack itemStack = getItem();
      ToolStack tool = ToolStack.from(itemStack);
      SoundEvent soundevent = ForgeRegistries.SOUND_EVENTS
            .getValue(
                  new ResourceLocation(itemStack.getTag().getCompound("thrownTempData").getString("hitEntitySound")));
      if (itemStack.getTag().getCompound("thrownTempData").getString("attack_type") == "melee")
         ToolAttackUtil.attackEntity(tool, attacker, InteractionHand.MAIN_HAND, targetEntity, new DoubleSupplier() {
            public double getAsDouble() {
               return 1;
            }
         }, false);

      this.playSound(soundevent);
      if (itemStack.getTag().getCompound("thrownTempData").getByte("penetrate") == 0) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
         if (tool.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) == 0
               && tool.getModifierLevel(Modifiers.IS_DUPLICATE.getId()) != 0)
            this.discard();
      } else
         itemStack.getTag().getCompound("thrownTempData").putByte("penetrate",
               (byte) (itemStack.getTag().getByte("penetrate") - 1));
   }

   public static boolean tryMergeToolStacks(ToolStack origin, ToolStack thrown) {
      if (origin.getItem() == thrown.getItem()
            && (ToolUUIDProviderModifier.getUUID(origin).toString().equals(ToolUUIDProviderModifier
                  .getUUID(thrown).toString()))) {
         FluidStack originF = ToolTankHelper.TANK_HELPER.getFluid(origin);
         FluidStack thrownF = ToolTankHelper.TANK_HELPER.getFluid(thrown);
         // 开始处理流体
         if (originF == null || thrownF == null) {
            ((ExtendedLogger) QikasTconlibMod.LOGGER).debug(
                  "Fail to merge thrown tool to origin stack: get null fluid stack; Cancel merging and hope game wont break;");
            return false;
         }
         if (originF == FluidStack.EMPTY) { // 如果原工具不含流体
            ToolTankHelper.TANK_HELPER.setFluid(origin, thrownF);
            ToolTankHelper.TANK_HELPER.setFluid(thrown, FluidStack.EMPTY);
         } else if (originF.getFluid().equals(thrownF.getFluid())) {
            int originA = originF.getAmount();
            int thrownA = thrownF.getAmount();
            int limit = ToolTankHelper.TANK_HELPER.getCapacity(origin);
            if (originA + thrownA > limit) {
               int delta = limit - originA;
               originF.setAmount(limit);
               thrownF.setAmount(delta);
               return false;
            }
            originF.setAmount(originA + thrownA);
            ToolTankHelper.TANK_HELPER.setFluid(thrown, FluidStack.EMPTY);
         } // 流体处理完毕
           // 暂时不考虑处理物品栏（会在掷出时清空目标的（如果需要））
           // 开始处理工具副本
         if (origin.getModifierLevel(Modifiers.TOOL_DUPLICATE_MANAGER.getId()) > 0) {
            ToolDuplicateManagerModifier.setDupCount(origin,
                  ToolDuplicateManagerModifier.getDupCount(origin) +
                        ToolDuplicateManagerModifier.getDupCount(thrown));
         } // 完毕
           // 处理耐久度
         ToolDamageUtil.repair(origin, thrown.getCurrentDurability());
      }
      return true;
   }

   public boolean tryPickup(Player player) {
      if (this.pickup == Pickup.ALLOWED) {
         ItemStack itemStack = this.getPickupItem();
         ToolStack toolStack = ToolStack.from(itemStack);
         // System.out.println(toolStack.getModifierLevel(Modifiers.IS_DUPLICATE.getId()));
         if (toolStack.getModifierLevel(Modifiers.IS_DUPLICATE.getId()) != 0) {
            for (ItemStack stack : player.getInventory().items) {
               if (stack.getItem() == itemStack.getItem()) {
                  ToolStack toolStack2 = ToolStack.from(stack);
                  // System.out.println(ToolUUIDProviderModifier.getUUID(toolStack) + " and "
                  // + ToolUUIDProviderModifier.getUUID(toolStack2));
                  if (ToolUUIDProviderModifier.getUUID(toolStack).toString().equals(ToolUUIDProviderModifier
                        .getUUID(toolStack2).toString())) {
                     // System.out.println("find same item");
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
                  // System.out.println(ToolUUIDProviderModifier.getUUID(toolStack) + " and "
                  // + ToolUUIDProviderModifier.getUUID(toolStack2));
                  if (ToolUUIDProviderModifier.getUUID(toolStack).toString().equals(ToolUUIDProviderModifier
                        .getUUID(toolStack2).toString())) {
                     // System.out.println("find same item");
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
            // System.out.println("for is over");
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
      Float i = ToolStack.from(getItem()).getStats().get(Stats.RETURN_SPEED);
      if (this.pickup != AbstractArrow.Pickup.ALLOWED || i <= 0) {
         super.tickDespawn();
      }

   }

   public void setItem(ItemStack p_37447_) {
      // System.out.println("Set item tag" + p_37447_.getTag().getAsString());
      this.toolItem = p_37447_.copy();
      this.getEntityData().set(DATA_ITEM_STACK, toolItem);
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