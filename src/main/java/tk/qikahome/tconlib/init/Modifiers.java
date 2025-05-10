package tk.qikahome.tconlib.init;

import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry;
import slimeknights.mantle.data.registry.NamedComponentRegistry;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.modifiers.modules.armor.MaxArmorAttributeModule;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.TConstruct;
import tk.qikahome.tconlib.QikasTconlibMod;
import tk.qikahome.tconlib.modifiers.IsDuplicateModifier;
import tk.qikahome.tconlib.modifiers.ToolDuplicateManagerModifier;
import tk.qikahome.tconlib.modifiers.ToolThrowingModule;
import tk.qikahome.tconlib.modifiers.ToolUUIDProviderModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.RegisterEvent;

public class Modifiers {
    public static ModifierDeferredRegister MODIFIERS = ModifierDeferredRegister.create(QikasTconlibMod.MODID);
    public static final StaticModifier<ToolDuplicateManagerModifier> TOOL_DUPLICATE_MANAGER = MODIFIERS
            .register(ToolDuplicateManagerModifier.localId, ToolDuplicateManagerModifier::new);
    public static final StaticModifier<ToolUUIDProviderModifier> TOOL_UUID_PROVIDER = MODIFIERS
            .register(ToolUUIDProviderModifier.localId, ToolUUIDProviderModifier::new);
    public static final StaticModifier<IsDuplicateModifier> IS_DUPLICATE = MODIFIERS
            .register(IsDuplicateModifier.localId, IsDuplicateModifier::new);

    @SubscribeEvent
    static void registerSerializers(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {
            ModifierModule.LOADER.register(new ResourceLocation(QikasTconlibMod.MODID, "tool_throwing"),
                    ToolThrowingModule.LOADER);
        }
    }
}
