package tk.qikahome.tconlib.init;

import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

public class QuaternionUtils {
    /**
     * 将视线方向向量转换为四元数（基于默认前方向 Z+）
     * @param lookDirection 单位化的视线方向向量（来自 getLookAngle()）
     * @return 表示旋转的四元数
     */
    public static Quaternionf fromLookDirection(Vec3 lookDirection) {
        // 0. 计算移动方向
        lookDirection=new Vec3(lookDirection.x*-1,lookDirection.y*-1,lookDirection.z);
        // 1. 默认前方向（Z+ 轴正方向）
        Vec3 forward = new Vec3(0, 0, 1);

        // 2. 计算旋转轴和角度
        Vec3 axis = forward.cross(lookDirection);
        double dot = forward.dot(lookDirection);

        // 3. 处理方向相同或相反的特殊情况
        if (axis.lengthSqr() < 1e-6) {
            if (dot < 0) {
                // 方向相反：绕 Y 轴旋转 180 度
                return new Quaternionf().rotationY((float) Math.PI);
            } else {
                // 方向相同：无旋转
                return new Quaternionf();
            }
        }

        // 4. 计算旋转角度（弧度）
        axis = axis.normalize();
        double angle = Math.acos(dot);

        // 5. 构造四元数（使用 JOML 的轴-角方法）
        return new Quaternionf().fromAxisAngleRad(
            (float) axis.x, 
            (float) axis.y, 
            (float) axis.z, 
            (float) angle
        );
    }

    /**
     * 通过俯仰角 (pitch) 和偏航角 (yaw) 生成四元数（输入为角度值）
     * @param pitch 俯仰角（角度制，范围：-90° ~ 90°）
     * @param yaw   偏航角（角度制，范围：0° ~ 360°）
     * @return 组合后的四元数
     */
    public static Quaternionf fromPitchYaw(float pitch, float yaw) {
        Quaternionf quat = new Quaternionf();
        // 将角度转换为弧度
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        // 旋转顺序：先绕 Y 轴（偏航），再绕 X 轴（俯仰）
        quat.rotateY(yawRad);
        quat.rotateX(pitchRad);
        return quat;
    }
}