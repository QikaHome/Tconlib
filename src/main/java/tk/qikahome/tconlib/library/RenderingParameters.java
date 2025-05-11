package tk.qikahome.tconlib.library;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.nbt.CompoundTag;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

public record RenderingParameters(int xRot, int yRot, int zRot, Float xTrans, Float yTrans, Float zTrans) {

  /** Loadable instance for parsing */
  public static final RecordLoadable<RenderingParameters> LOADABLE = RecordLoadable.create(
      IntLoadable.ANY_BYTE.defaultField("xRot", 0, RenderingParameters::xRot),
      IntLoadable.ANY_BYTE.defaultField("yRot", 0, RenderingParameters::yRot),
      IntLoadable.ANY_BYTE.defaultField("zRot", 0, RenderingParameters::zRot),
      FloatLoadable.ANY.defaultField("xTrans", 0f, RenderingParameters::xTrans),
      FloatLoadable.ANY.defaultField("yTrans", 0f, RenderingParameters::yTrans),
      FloatLoadable.ANY.defaultField("zTrans", 0f, RenderingParameters::zTrans),
      RenderingParameters::new);
  public void applyParas(PoseStack stack, float times) {
    stack.mulPose(Axis.XP.rotationDegrees(times * xRot % 360));
    stack.mulPose(Axis.YP.rotationDegrees(times * yRot % 360));
    stack.mulPose(Axis.ZP.rotationDegrees(times * zRot % 360));
    stack.translate(xTrans * times, yTrans * times, zTrans * times);
  }
/**
   * 将参数序列化为 NBT 标签
   */
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    // 写入旋转参数（int）
    tag.putInt("xRot", xRot);
    tag.putInt("yRot", yRot);
    tag.putInt("zRot", zRot);
    // 写入位移参数（float）
    tag.putFloat("xTrans", xTrans);
    tag.putFloat("yTrans", yTrans);
    tag.putFloat("zTrans", zTrans);
    return tag;
  }

  /**
   * 从 NBT 标签反序列化参数（静态工厂方法）
   */
  public static RenderingParameters fromNBT(CompoundTag tag) {
    // 读取旋转参数（int），默认值为 0
    int xRot = tag.getInt("xRot");
    int yRot = tag.getInt("yRot");
    int zRot = tag.getInt("zRot");
    // 读取位移参数（float），默认值为 0.0f
    float xTrans = tag.getFloat("xTrans");
    float yTrans = tag.getFloat("yTrans");
    float zTrans = tag.getFloat("zTrans");
    // 构造新对象
    return new RenderingParameters(xRot, yRot, zRot, xTrans, yTrans, zTrans);
  }
}
