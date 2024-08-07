package tomato.gui.dungeons;

import packets.incoming.MapInfoPacket;
import packets.incoming.UpdatePacket;
import tomato.backend.data.TomatoData;

import javax.swing.*;
import java.util.*;

public class DungeonGUI extends JPanel {
    private static final String DISABLE_FILTER = "Default";
    private static DungeonGUI INSTANCE;
    
    private final List<Dungeon> dungeons;

    public static DungeonGUI getInstance() {
        return INSTANCE;
    }

    private TomatoData data;
    private DungeonListGUI dungeonListGUI;
    
    private Dungeon currentDungeon;

    public DungeonGUI(TomatoData data) {
        INSTANCE = this;

        this.data = data;
        this.dungeons = new ArrayList<>();
        this.dungeonListGUI = new DungeonListGUI(data);
        add(this.dungeonListGUI);
        this.dungeonListGUI.setMissingCurrentDungeonInformation(true);
        this.updateRender();
    }
    
    private void updateRender(){
        this.dungeonListGUI.updateRender(this.dungeons);
    }
    
    private void onDungeonExit(){
        if (this.currentDungeon == null) return;
        
        this.dungeons.add(this.currentDungeon);
        this.currentDungeon = null;
        this.updateRender();
        System.out.println("[DUNGEON] Exited current dungeon.");
    }
    
    
    public void onUpdate(final UpdatePacket updatePacket){
        if (this.currentDungeon != null)
            this.currentDungeon.onUpdate(updatePacket);

        this.dungeonListGUI.setMissingCurrentDungeonInformation(this.currentDungeon == null);
    }
    
    public void onMapInfo(final MapInfoPacket mapInfoPacket){
        if (this.currentDungeon != null) 
            this.onDungeonExit();

        this.currentDungeon = new Dungeon(new MapInfo(mapInfoPacket));
        this.dungeonListGUI.setMissingCurrentDungeonInformation(false);
        System.out.println("[DUNGEON] updated to map: " + this.currentDungeon.getMapInfo().getName());
    }
}