package dev.dragonslegacy;

import com.flowpowered.math.vector.Vector3d;
import com.technicjelle.BMUtils.BMCopy;
import dev.dragonslegacy.api.DragonEggAPI;
import dev.dragonslegacy.config.Config;
import dev.dragonslegacy.config.Data;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.util.Map;

import static dev.dragonslegacy.DragonsLegacyMod.*;

class BlueMapIntegration {
    private static final MarkerSet MARKER_SET = new MarkerSet(MOD_ID, true, false);
    private static final ExtrudeMarker AREA_MARKER = ExtrudeMarker.builder()
        .label(MOD_ID)
        .shape(Shape.createCircle(0, 0, 0, 3), 0, 0)
        .lineWidth(0)
        .build();
    private static final POIMarker POINT_MARKER = POIMarker
        .builder()
        .label(MOD_ID)
        .position(new Vector3d())
        .styleClasses(MOD_ID + "-poi-marker")
        .build();
    private static Config config;
    private static Data data;

    public static void init() {
        DragonEggAPI.onUpdate(BlueMapIntegration::onUpdate);
        BlueMapAPI.onEnable(api -> {
            try {
                BMCopy.jarResourceToWebApp(
                    api,
                    BlueMapIntegration.class.getClassLoader(),
                    "assets/style.css",
                    MOD_ID + ".css",
                    true
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void onUpdate(Data data) {
        if (CONFIG != config) {
            MARKER_SET.setLabel(CONFIG.markerName);

            AREA_MARKER.setLabel(CONFIG.markerName);
            AREA_MARKER.setDetail(CONFIG.areaMarkerDescription);
            AREA_MARKER.setFillColor(new Color(0x5f << 24 | CONFIG.markerColor));

            POINT_MARKER.setLabel(CONFIG.markerName);
            POINT_MARKER.setDetail(CONFIG.pointMarkerDescription);
            POINT_MARKER.setIcon(CONFIG.pointMarkerIcon, 24, 24);

            config = CONFIG;
        }

        BlueMapIntegration.data = data;
        BlueMapAPI.onEnable(BlueMapIntegration::updateBluemap);
    }

    private static void updateBluemap(BlueMapAPI api) {
        try {
            BlueMapAPI.unregisterListener(BlueMapIntegration::updateBluemap);
            Config.VisibilityType visibility = CONFIG.getVisibility(data.type);

            switch (visibility) {
                case HIDDEN:
                    api.getMaps().forEach(map -> map.getMarkerSets().remove("dragon_egg"));
                    return;
                case EXACT:
                    POINT_MARKER.setPosition(
                        data.getPosition().x(),
                        data.getPosition().y(),
                        data.getPosition().z()
                    );
                    MARKER_SET.put("dragon_egg", POINT_MARKER);
                    break;
                case RANDOMIZED:
                    BlockPos rPos = data.getRandomizedPosition();
                    AREA_MARKER.setShape(
                        Shape.createCircle(rPos.getX(), rPos.getZ(), CONFIG.searchRadius, 36),
                        rPos.getY() - CONFIG.searchRadius,
                        rPos.getY() + CONFIG.searchRadius
                    );
                    AREA_MARKER.setPosition(new Vector3d(rPos.getX(), rPos.getY(), rPos.getZ()));
                    MARKER_SET.put("dragon_egg", AREA_MARKER);
                    break;
            }

            api.getWorld(data.world).ifPresent(world ->
                api.getMaps().forEach(map -> {
                    Map<String, MarkerSet> markerSets = map.getMarkerSets();
                    if (map.getWorld().equals(world)) markerSets.put("dragon_egg", MARKER_SET);
                    else markerSets.remove("dragon_egg");
                })
            );
        } catch (Exception e) {
            LOGGER.error("Error during bluemap update", e);
        }

    }

}
