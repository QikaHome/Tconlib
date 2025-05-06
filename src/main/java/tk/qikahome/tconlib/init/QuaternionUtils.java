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
        // 1. 定义初始方向：尖端默认指向 X+ 向上 45 度（非单位向量，需归一化）
        Vec3 defaultTipDirection = new Vec3(1, 1, 0).normalize();
    
        // 2. 归一化目标方向
        Vec3 targetDirection =  new Vec3(lookDirection.x*-1, lookDirection.y*-1, lookDirection.z).normalize();
    
        // 3. 计算旋转轴和角度
        Vec3 axis = defaultTipDirection.cross(targetDirection);
        double dot = defaultTipDirection.dot(targetDirection);
    
        // 4. 处理共线情况
        if (axis.lengthSqr() < 1e-6) {
            if (dot < 0) {
                // 方向相反：绕垂直轴旋转 180 度（此处假设 Z 轴为垂直轴）
                return new Quaternionf().rotationZ((float) Math.PI);
            } else {
                return new Quaternionf(); // 无旋转
            }
        }
    
        // 5. 计算旋转四元数
        axis = axis.normalize();
        double angle = Math.acos(dot);
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