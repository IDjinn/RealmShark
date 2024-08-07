package tomato.gui.dungeons.bossess;

import sun.awt.Mutex;

public class Boss {
    private final Mutex mutex;
    
    private String name;
    private long health;

    public Boss() {
        this.mutex = new Mutex();
    }
}
