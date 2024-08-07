package tomato.gui.dungeons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import packets.data.GroundTileData;
import packets.data.ObjectData;
import packets.incoming.MapInfoPacket;
import packets.incoming.UpdatePacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Dungeon{
    private final UUID uuid;
    private final MapInfo mapInfo;
    private final Map<Integer, ObjectData> objects;
    private final Map<Integer, GroundTileData> tiles;

    public Dungeon(MapInfo mapInfo) {
        this.uuid = UUID.randomUUID();
        this.mapInfo = mapInfo;
        
        this.objects = new HashMap<>();
        this.tiles = new HashMap<>();
    }

    public void onUpdate(final UpdatePacket updatePacket) {
        for (ObjectData objectData : updatePacket.newObjects) {
            this.objects.put(objectData.status.objectId, objectData);
        }

        for (GroundTileData groundTileData : updatePacket.tiles) {
            this.tiles.put(groundTileData.x + groundTileData.y * mapInfo.getWidth(), groundTileData);
        }
    }

    public MapInfo getMapInfo() {
        return mapInfo;
    }

    public Map<Integer, ObjectData> getObjects() {
        return objects;
    }

    public Map<Integer, GroundTileData> getTiles() {
        return tiles;
    }
    
    public String export(){
        final StringBuilder stringBuilder = new StringBuilder(1024 * 1024);
        final JsonObject jsonObject = new JsonObject();
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        jsonObject.add("mapInfo", gson.toJsonTree(this.mapInfo));
        jsonObject.add("objects", gson.toJsonTree(this.objects));
        jsonObject.add("tiles", gson.toJsonTree(this.tiles));
        
        return stringBuilder.append(gson.toJson(jsonObject)).toString();
    }

    public void dispose() {
        this.objects.clear();
        this.tiles.clear();
        
        System.out.println("[DUNGEON] dispose complete for " + this.getMapInfo().getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass()!= obj.getClass()) return false;
        Dungeon dungeon = (Dungeon) obj;
        return uuid.equals(dungeon.uuid);
    }

    public UUID getUuid() {
        return uuid;
    }
}
