package sh.creepercdn.dashloadingscreen;

// https://github.com/shedaniel/BetterLoadingScreen/blob/master/fabric/src/main/java/me/shedaniel/betterloadingscreen/fabric/MinecraftGraphicsImpl.java

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class MinecraftGraphicsUtils {
    public static ResourceManager createResourceManager(DefaultResourcePack pack) {
        MappingResolver resolver = FabricLoader.getInstance().getMappingResolver();
        try {
            Class<?> clazz = Class.forName(resolver.mapClassName("intermediary", "net.minecraft.class_6861"));
            Constructor<?> constructor = clazz.getDeclaredConstructor(ResourceType.class, List.class);
            return (ResourceManager) constructor.newInstance(ResourceType.CLIENT_RESOURCES, Collections.singletonList(pack));
        } catch (ClassNotFoundException ignored) {
            try {
                Class<?> clazz = Class.forName(resolver.mapClassName("intermediary", "net.minecraft.class_3304"));
                Constructor<?> constructor = clazz.getDeclaredConstructor(ResourceType.class);
                ResourceManager manager = (ResourceManager) constructor.newInstance(ResourceType.CLIENT_RESOURCES);
                Method add = clazz.getDeclaredMethod(resolver.mapMethodName("intermediary", "net.minecraft.class_3304", "method_14475",
                        "(Lnet/minecraft/class_3262;)V"), ResourcePack.class);
                add.setAccessible(true);
                add.invoke(manager, pack);
                return manager;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
