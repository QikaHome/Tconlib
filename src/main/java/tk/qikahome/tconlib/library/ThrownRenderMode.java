package tk.qikahome.tconlib.library;

import net.minecraft.nbt.CompoundTag;
import slimeknights.mantle.data.loadable.primitive.BooleanLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

public record ThrownRenderMode(boolean faceTowards, RenderingParameters premove, RenderingParameters flying,
    RenderingParameters on_ground, RenderingParameters postmove) {

  /** Loadable instance for parsing */
  public static final RecordLoadable<ThrownRenderMode> LOADABLE = RecordLoadable.create(
      BooleanLoadable.DEFAULT.defaultField("faceTowards", false, ThrownRenderMode::faceTowards),
      RenderingParameters.LOADABLE.nullableField("premove", ThrownRenderMode::premove),
      RenderingParameters.LOADABLE.nullableField("flying", ThrownRenderMode::flying),
      RenderingParameters.LOADABLE.nullableField("on_ground", ThrownRenderMode::on_ground),
      RenderingParameters.LOADABLE.nullableField("postmove", ThrownRenderMode::postmove),
      ThrownRenderMode::new);

  /**
   * 序列化为 NBT 标签
   */
  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putBoolean("faceTowards", faceTowards);
    // 处理 premove 可能为 null 的情况
    if (premove != null)
      tag.put("premove", premove.toNBT());

    // 处理 flying 可能为 null 的情况
    if (flying != null)
      tag.put("flying", flying.toNBT());

    // 处理 on_ground 可能为 null 的情况
    if (on_ground != null)
      tag.put("on_ground", on_ground.toNBT());

    // 处理 postmove 可能为 null 的情况
    if (postmove != null)
      tag.put("postmove", postmove.toNBT());

    return tag;
  }

  /**
   * 从 NBT 标签反序列化（静态工厂方法）
   */
  public static ThrownRenderMode fromNBT(CompoundTag tag) {
    boolean faceTowards = tag.getBoolean("faceTowards");
    // 读取 premove（允许 null）
    RenderingParameters premove = RenderingParameters.fromNBT(tag.getCompound("premove"));
    // 读取 flying（允许 null）
    RenderingParameters flying = RenderingParameters.fromNBT(tag.getCompound("flying"));
    // 读取 premove（允许 null）
    RenderingParameters on_ground = RenderingParameters.fromNBT(tag.getCompound("on_ground"));
    // 读取 flying（允许 null）
    RenderingParameters postmove = RenderingParameters.fromNBT(tag.getCompound("postmove"));
    return new ThrownRenderMode(faceTowards, premove, flying, on_ground, postmove);
  }
}