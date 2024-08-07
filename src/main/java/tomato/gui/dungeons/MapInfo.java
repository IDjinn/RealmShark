package tomato.gui.dungeons;

import packets.incoming.MapInfoPacket;

public class MapInfo {
    private final int width;
    private final int height;
    private final String name;
    private final String displayName;
    private final String realmName;
    private final float difficulty;
    private final long seed;
    private final int background;
    private final boolean allowPlayerTeleport;
    private final boolean showDisplays;
    private final boolean noSave;
    private final short maxPlayers;
    private final long gameOpenedTime;
    private final String buildVersion;
    private final int unknownInt;
    private final int BGColor;
    private final String dungeonModifiers;
    private final String dungeonModifiers2;
    private final String dungeonModifiers3;
    private final int maxRealmScore;
    private final int currentRealmScore;
    
    public MapInfo(MapInfoPacket packet){
        this.width = packet.width;
        this.height = packet.height;
        this.name = packet.name;
            
        this.displayName = packet.displayName;
        this.realmName = packet.realmName;
        this.difficulty = packet.difficulty;
        this.seed = packet.seed;
        this.background = packet.background;
        this.allowPlayerTeleport = packet.allowPlayerTeleport;
        this.showDisplays = packet.showDisplays;
        this.noSave = packet.noSave;
            
        this.maxPlayers = packet.maxPlayers;
        this.gameOpenedTime = packet.gameOpenedTime;
        this.buildVersion = packet.buildVersion;
        this.unknownInt = packet.unknownInt;
        this.BGColor = packet.BGColor;
            
        this.dungeonModifiers = packet.dungeonModifiers;
        this.dungeonModifiers2 = packet.dungeonModifiers2;
        this.dungeonModifiers3 = packet.dungeonModifiers3;
            
        this.maxRealmScore = packet.maxRealmScore;
        this.currentRealmScore = packet.currentRealmScore;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRealmName() {
        return realmName;
    }

    public float getDifficulty() {
        return difficulty;
    }

    public long getSeed() {
        return seed;
    }

    public int getBackground() {
        return background;
    }

    public boolean isAllowPlayerTeleport() {
        return allowPlayerTeleport;
    }

    public boolean isShowDisplays() {
        return showDisplays;
    }

    public boolean isNoSave() {
        return noSave;
    }

    public short getMaxPlayers() {
        return maxPlayers;
    }

    public long getGameOpenedTime() {
        return gameOpenedTime;
    }

    public String getBuildVersion() {
        return buildVersion;
    }

    public int getUnknownInt() {
        return unknownInt;
    }

    public int getBGColor() {
        return BGColor;
    }

    public String getDungeonModifiers() {
        return dungeonModifiers;
    }

    public String getDungeonModifiers2() {
        return dungeonModifiers2;
    }

    public String getDungeonModifiers3() {
        return dungeonModifiers3;
    }

    public int getMaxRealmScore() {
        return maxRealmScore;
    }

    public int getCurrentRealmScore() {
        return currentRealmScore;
    }
}
