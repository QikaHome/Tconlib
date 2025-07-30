package tk.qikahome.tconlib.init;

import slimeknights.tconstruct.library.tools.stat.FloatToolStat;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import tk.qikahome.tconlib.QikasTconlibMod;

public class Stats {

    private static ToolStatId name(String name) {
        return new ToolStatId(QikasTconlibMod.MODID, name);
    }

    public static final FloatToolStat RETURN_SPEED = ToolStats.register(
            new FloatToolStat(name("return_speed"), 0xFF47CC47, 0, 0, Byte.MAX_VALUE));

    public static final FloatToolStat AIRDRAG = ToolStats.register(
            new FloatToolStat(name("air_drag"), 0xFF47CC47, 0.01f, 0f, 1f));

    public static final FloatToolStat WATERDRAG = ToolStats.register(
            new FloatToolStat(name("water_drag"), 0xFF47CC47, 0.4f, 0f, 1f));

    public static final FloatToolStat G = ToolStats.register(
            new FloatToolStat(name("gravity"), 0xFF47CC47, 1f, 0f, Float.MAX_VALUE));
}
