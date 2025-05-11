package tk.qikahome.tconlib.modifiers;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.modifiers.IncrementalModifierEntry;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.context.ToolRebuildContext;
import slimeknights.tconstruct.library.tools.definition.module.build.ToolStatsHook;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.library.tools.stat.IToolStat;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStatId;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import tk.qikahome.tconlib.QikasTconlibMod;

public class ToolUUIDProviderModifier extends Modifier {// implements ToolStatsModifierHook {
    public static String localId = "tool_uuid_provider";

    /*
     * @Override
     * public void registerHooks(Builder hookBuilder) {
     * super.registerHooks(hookBuilder);
     * hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
     * }
     * 
     * public static class UUIDToolStat implements IToolStat<UUID> {
     * 
     * public static class Builder {
     * UUID uuid;
     * 
     * public Builder(UUID id) {
     * this.uuid = id;
     * }
     * }
     * 
     * public final ToolStatId name;
     * 
     * public final TextColor color;
     * 
     * public final TagKey<Item> tag;
     * 
     * public UUIDToolStat(ToolStatId name, int color, @Nullable TagKey<Item> tag) {
     * this.name = name;
     * this.color = TextColor.fromRgb(color);
     * this.tag = tag;
     * }
     * 
     * @Override
     * public ToolStatId getName() {
     * return name;
     * }
     * 
     * @Override
     * public UUID getDefaultValue() {
     * return UUID.randomUUID();
     * }
     * 
     * @Override
     * public Object makeBuilder() {
     * return new Builder(getDefaultValue());
     * }
     * 
     * @Override
     * public UUID build(ModifierStatsBuilder parent, Object builder) {
     * return ((Builder) builder).uuid;
     * }
     * 
     * @Override
     * public void update(ModifierStatsBuilder builder, UUID value) {
     * builder.<Builder>updateStat(this, b -> {
     * b.uuid = value;
     * });
     * }
     * 
     * @Override
     * 
     * @Nullable
     * public UUID read(Tag tag) {
     * if (tag.getId() == Tag.TAG_STRING)
     * return safeStringToUUID(((StringTag) tag).getAsString());
     * return null;
     * }
     * 
     * @Override
     * 
     * @Nullable
     * public Tag write(UUID value) {
     * return StringTag.valueOf(value.toString());
     * }
     * 
     * @Override
     * public UUID deserialize(JsonElement json) {
     * return safeStringToUUID(GsonHelper.convertToString(json, ""));
     * }
     * 
     * @Override
     * public JsonElement serialize(UUID value) {
     * return new JsonPrimitive(value.toString());
     * }
     * 
     * @Override
     * public UUID fromNetwork(FriendlyByteBuf buffer) {
     * return buffer.readUUID();
     * }
     * 
     * @Override
     * public void toNetwork(FriendlyByteBuf buffer, UUID value) {
     * buffer.writeUUID(value);
     * }
     * 
     * @Override
     * public Component formatValue(UUID value) {
     * return Component.translatable(getTranslationKey())
     * .append(Component.literal(Util.COMMA_FORMAT.format(value.toString()))
     * .withStyle(style -> style.withColor(color)));
     * }
     * 
     * }
     */
    /**
     * 安全地将字符串转换为 UUID（支持无连字符、带连字符、带花括号等格式）
     * 
     * @param string 输入字符串（允许 null）
     * @return 转换成功的 UUID，失败返回 null
     */
    public static UUID safeStringToUUID(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        // 预处理字符串：去除前后空格、花括号等冗余字符
        String trimmed = string.trim()
                .replace("{", "")
                .replace("}", "")
                .replace("(", "")
                .replace(")", "");

        try {
            // 标准 UUID 格式（带连字符）
            if (trimmed.length() == 36) {
                return UUID.fromString(trimmed);
            }

            // 无连字符格式（32字符）
            if (trimmed.length() == 32) {
                // 手动插入连字符
                String formatted = new StringBuilder(trimmed)
                        .insert(20, "-")
                        .insert(16, "-")
                        .insert(12, "-")
                        .insert(8, "-")
                        .toString();
                return UUID.fromString(formatted);
            }

        } catch (IllegalArgumentException e) {
            // 格式错误时捕获异常
        }

        // 其他情况（如长度不足、非法字符）返回 null
        return null;
    }

    public static ResourceLocation UUIDLocation = new ResourceLocation("tool_uuid");

    public static Boolean hasSameUUID(ToolStack a, ToolStack b) {
        return getUUID(a).toString() == getUUID(b).toString();
    }

    /**
     * 
     * @param tool 输入的工具栈
     * @return 工具的（唯一）UUID
     */
    public static UUID getUUID(IToolStackView tool) {
        UUID uuid = safeStringToUUID(tool.getPersistentData().getString(UUIDLocation));
        if (uuid == null) {
            tool.getPersistentData().putString(UUIDLocation, UUID.randomUUID().toString());
            return getUUID(tool);
        }
        return uuid;
    }

    /**
     * 
     * @param tool 要被设定UUID的工具栈
     * @param uuid 要被设定的UUID(字符串)
     * @return 是否成功设定
     */
    public static Boolean setUUID(IToolStackView tool, String uuid) {
        tool.getPersistentData().putString(UUIDLocation, uuid);
        return true;
    }

    /**
     * 
     * @param tool 要被设定UUID的工具栈
     * @param uuid 要被设定的UUID
     * @return 是否成功设定
     */
    public static Boolean setUUID(IToolStackView tool, UUID uuid) {
        return setUUID(tool, uuid.toString());
    }

    // public static final UUIDToolStat TOOL_UUID = ToolStats
    // .register(new UUIDToolStat(new ToolStatId("tool_uuid"), 0xFF71DC85, null));

    @Override
    public Component getDisplayName(IToolStackView tool, ModifierEntry entry, @Nullable RegistryAccess access) {
        return Component.literal("UUID: " + getUUID(tool)).withStyle(style -> style.withColor(getTextColor()));
    }

    public Component getDisplayName(int level) {
        return getDisplayName();
    }

    public Component getDisplayName() {
        return super.getDisplayName();
    }
}
