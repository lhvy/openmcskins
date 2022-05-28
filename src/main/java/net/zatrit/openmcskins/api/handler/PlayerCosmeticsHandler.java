package net.zatrit.openmcskins.api.handler;

import net.zatrit.openmcskins.io.skins.Cosmetics;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface PlayerCosmeticsHandler {
    @Nullable List<Cosmetics.CosmeticsItem> downloadCosmetics();
}
