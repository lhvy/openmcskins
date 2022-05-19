package net.zatrit.openmcskins.resolvers;

import com.google.gson.internal.LinkedTreeMap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.zatrit.openmcskins.Cache;
import net.zatrit.openmcskins.OpenMCSkins;
import net.zatrit.openmcskins.loader.CosmeticsLoader;
import net.zatrit.openmcskins.resolvers.handler.PlayerCosmeticsHandler;
import net.zatrit.openmcskins.util.io.NetworkUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;

public record OptifineResolver(
        String baseUrl) implements Resolver<OptifineResolver.PlayerSkinHandler>, PlayerCosmeticsResolver<OptifineResolver.PlayerSkinHandler> {
    public OptifineResolver(String baseUrl) {
        this.baseUrl = NetworkUtils.fixUrl(baseUrl);
    }

    @Contract("_ -> new")
    @Override
    public @NotNull OptifineResolver.PlayerSkinHandler resolvePlayer(GameProfile profile) {
        return new PlayerSkinHandler(profile);
    }

    @Override
    public boolean requiresUUID() {
        return false;
    }

    public class PlayerSkinHandler extends DirectResolver.PlayerHandler implements PlayerCosmeticsHandler {
        public static List<Identifier> texturesLoaded = new LinkedList<>();
        private final GameProfile profile;

        public PlayerSkinHandler(@NotNull GameProfile profile) {
            super(String.format("%s/capes/%s.png", baseUrl, profile.getName()), profile, MinecraftProfileTexture.Type.CAPE);
            this.profile = profile;
        }

        private static LinkedTreeMap<String, Object> mapFromReader(Reader reader) {
            return GSON.<LinkedTreeMap<String, Object>>fromJson(reader, LinkedTreeMap.class);
        }

        private static void loadTextureFromUrl(String url, Identifier id) throws Exception {
            if (texturesLoaded.contains(id)) return;

            NativeImage image = NativeImage.read(Cache.SKINS.getCache().getOrDownload(url, new URL(url)::openStream));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));

            texturesLoaded.add(id);
        }

        private String formatUrl(String pattern, String content) {
            return String.format(pattern, baseUrl, content);
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<CosmeticsLoader.CosmeticsItem> downloadCosmetics() {
            try {
                String urlString = formatUrl("%s/users/%s.cfg", profile.getName());
                URL realUrl = new URL(urlString);

                if (NetworkUtils.getResponseCode(urlString) == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(realUrl.openStream()));
                    LinkedTreeMap<String, Object> map = mapFromReader(in);
                    List<Map<String, Object>> items = (List<Map<String, Object>>) map.get("items");

                    List<CosmeticsLoader.CosmeticsItem> cosmetics = new ArrayList<>();

                    items.stream().filter(item -> Objects.equals(item.get("active"), "true")).forEach(item -> {
                        String modelType = (String) item.get("type");
                        Identifier textureId = new Identifier("cosmetics_texture", modelType);
                        Identifier modelId = new Identifier("cosmetics_model", modelType);
                        try {
                            loadTextureFromUrl(formatUrl("%s/%s", (String) item.get("texture")), textureId);
                            URL modelUrl = new URL(formatUrl("%s/%s", (String) item.get("model")));

                            LinkedTreeMap<String, Object> model = mapFromReader(new InputStreamReader(Cache.MODELS.getCache().getOrDownload(modelType, modelUrl::openStream)));
                            cosmetics.add(CosmeticsLoader.loadSingleCosmeticItem(textureId, modelId, model, modelType));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    return cosmetics;
                }
            } catch (Exception ex) {
                OpenMCSkins.handleError(ex);
            }
            return null;
        }
    }
}
