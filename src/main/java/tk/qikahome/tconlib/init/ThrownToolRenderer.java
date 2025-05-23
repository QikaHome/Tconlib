package tk.qikahome.tconlib.init;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import tk.qikahome.tconlib.entity.ThrownTool;
import tk.qikahome.tconlib.library.ThrownRenderMode;

//渲染ThrownTool实体
public class ThrownToolRenderer extends EntityRenderer<ThrownTool> {

    // 用于渲染物品的渲染器
    private final ItemRenderer itemRenderer;

    /**
     * 构造函数，初始化渲染器。
     *
     * @param context 渲染器提供程序的上下文。
     */
    public ThrownToolRenderer(EntityRendererProvider.Context context) {
        // 调用父类的构造函数
        super(context);
        // 获取物品渲染器
        this.itemRenderer = context.getItemRenderer();
    }

    /**
     * 渲染 TinkerShurikenEntity 实体的方法。
     *
     * @param entity        要渲染的实体。
     * @param entityYaw     实体的偏航角。
     * @param partialTicks  部分渲染帧的时间。
     * @param matrixStackIn 用于变换的矩阵堆栈。
     * @param buffIn        用于渲染的缓冲区源。
     * @param packedLightIn 打包的光照信息。
     */
    @Override
    public void render(ThrownTool entity, float entityYaw, float partialTicks, PoseStack matrixStackIn,
            MultiBufferSource buffIn, int packedLightIn) {
        // 如果实体的 tick 计数大于等于 2 或者相机距离实体的平方大于 12.25，则进行渲染
        if (entity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(entity) < 12.25D)) {
            // 将矩阵堆栈压入
            matrixStackIn.pushPose();
            // 获取物品渲染模式
            try {
                ThrownRenderMode renderMode = ThrownRenderMode.fromNBT(entity.getItem().getTagElement("render_mode"));
                if (renderMode.faceTowards()) {// 尖端朝前
                    // 旋转使尖端朝前进方向
                    matrixStackIn.mulPose(QuaternionUtils.fromLookDirection(entity.getLookAngle()));
                }
                // 添加预先旋转&位移
                renderMode.premove().applyParas(matrixStackIn, 1);
                // 如果未落地，添加空中的
                if (!entity.inGround())
                    renderMode.flying().applyParas(matrixStackIn, entity.tickCount + partialTicks);
                else // 否则添加落地的
                    renderMode.on_ground().applyParas(matrixStackIn, entity.tickCount + partialTicks);
                // 添加收尾
                renderMode.postmove().applyParas(matrixStackIn, 1);
            } catch (Exception e) {
                // Do Nothing
            }
            // 渲染静态物品
            this.itemRenderer.render(entity.getItem(), ItemDisplayContext.GROUND, false, matrixStackIn, buffIn,
                    packedLightIn, OverlayTexture.NO_OVERLAY,
                    this.itemRenderer.getModel(entity.getItem(), entity.level(), (LivingEntity) null, entity.getId()));
            // 弹出矩阵堆栈
            matrixStackIn.popPose();
            // 调用父类的渲染方法
            // super.render(entity, entityYaw, partialTicks, matrixStackIn, buffIn,
            // packedLightIn);
        }
    }

    /**
     * 获取实体的纹理位置。
     *
     * @param entity 要获取纹理的实体。
     * @return 纹理的资源位置。
     */
    @Override
    public ResourceLocation getTextureLocation(ThrownTool entity) {
        // 返回物品栏的纹理图集
        return InventoryMenu.BLOCK_ATLAS;
    }
}
