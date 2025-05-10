package tk.qikahome.tconlib.modifiers;

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
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import slimeknights.tconstruct.library.modifiers.IncrementalModifierEntry;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
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

public class ToolUUIDProviderModifier extends Modifier implements ToolStatsHook {
    public static String localId = "tool_uuid_provider";

    public static class UUIDToolStat implements IToolStat<UUID> {

        public static class Builder {
            UUID uuid;

            public Builder(UUID id) {
                this.uuid = id;
            }
        }

        public final ToolStatId name;

        public final TextColor color;

        public final TagKey<Item> tag;

        public UUIDToolStat(ToolStatId name, int color, @Nullable TagKey<Item> tag) {
            this.name = name;
            this.color = TextColor.fromRgb(color);
            this.tag = tag;
        }

        @Override
        public ToolStatId getName() {
            return name;
        }

        @Override
        public UUID getDefaultValue() {
            return UUID.randomUUID();
        }

        @Override
        public Object makeBuilder() {
            return new Builder(getDefaultValue());
        }

        @Override
        public UUID build(ModifierStatsBuilder parent, Object builder) {
            return ((Builder) builder).uuid;
        }

        @Override
        public void update(ModifierStatsBuilder builder, UUID value) {
            builder.<Builder>updateStat(this, b -> {
                b.uuid = value;
            });
        }

        @Override
        @Nullable
        public UUID read(Tag tag) {
            if (tag.getId() == Tag.TAG_STRING)
                return UUID.fromString(((StringTag) tag).getAsString());
            return null;
        }

        @Override
        @Nullable
        public Tag write(UUID value) {
            return StringTag.valueOf(value.toString());
        }

        @Override
        public UUID deserialize(JsonElement json) {
            return UUID.fromString(GsonHelper.convertToString(json, name.toString()));
        }

        @Override
        public JsonElement serialize(UUID value) {
            return new JsonPrimitive(value.toString());
        }

        @Override
        public UUID fromNetwork(FriendlyByteBuf buffer) {
            return buffer.readUUID();
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, UUID value) {
            buffer.writeUUID(value);
        }

        @Override
        public Component formatValue(UUID value) {
            return Component.translatable(getTranslationKey())
                    .append(Component.literal(Util.COMMA_FORMAT.format(value.toString()))
                            .withStyle(style -> style.withColor(color)));
        }

    }
    public static Boolean hasSameUUID(ToolStack a,ToolStack b)
    {
        return getUUID(a)==getUUID(b);
    }
    public static UUID getUUID(ToolStack tool)
    {
        return tool.getStats().get(TOOL_UUID);
    }

    public static final UUIDToolStat TOOL_UUID = ToolStats
            .register(new UUIDToolStat(new ToolStatId(QikasTconlibMod.MODID, "uuid"), 0xFF71DC85, null));

    @Override
    public Component getDisplayName(IToolStackView tool, ModifierEntry entry, @Nullable RegistryAccess access) {
        return Component.literal("UUID: " + tool.getStats().get(TOOL_UUID).toString());
    }

    public Component getDisplayName(int level) {
        return getDisplayName();
    }

    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public void addToolStats(IToolContext context, ModifierStatsBuilder builder) {
        TOOL_UUID.update(builder,
                ((ToolStack) context).getStats().hasStat(TOOL_UUID) ? ((ToolStack) context).getStats().get(TOOL_UUID)
                        : UUID.randomUUID());
    }
}
