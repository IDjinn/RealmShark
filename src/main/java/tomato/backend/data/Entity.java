package tomato.backend.data;

import assets.IdToAsset;
import packets.data.ObjectStatusData;
import packets.data.StatData;
import packets.data.WorldPosData;
import packets.data.enums.ConditionBits;
import packets.data.enums.ConditionNewBits;
import packets.data.enums.StatType;
import tomato.backend.StasisCheck;
import tomato.gui.character.CharacterStatMaxingGUI;
import tomato.gui.dps.DpsGUI;
import tomato.gui.myinfo.MyInfoGUI;
import tomato.gui.security.ParsePanelGUI;
import tomato.realmshark.RealmCharacter;
import tomato.realmshark.enums.CharacterClass;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Entity implements Serializable {
    private boolean isUser;
    public final Stat stat;
    transient private final TomatoData tomatoData;
    public final int id;
    public int objectType;
    private long creationTime;
    public WorldPosData pos;
    public final ArrayList<ObjectStatusData> statUpdates;
    private final ArrayList<Damage> damageList;
    private final HashMap<Integer, Damage> damagePlayer;
    public final HashMap<Integer, PlayerRemoved> playerDropped;
    public final HashMap<Long, Damage> playerHits;
    private String name;
    private long firstDamageTaken = -1;
    private long lastDamageTaken = -1;
    private int charId;
    public int[] baseStats;
    private boolean isPlayer;
    public long stasisCounter;
    public boolean dammahCountered;

    private final static int ORYX_THE_MAD_GOD = 45363;
    private final static int ORYX_THE_MAD_GOD_GUARD_ANIMATION = -935464302;
    private final static int ORYX_THE_MAD_GOD_GUARD_EXALTED_ANIMATION = -918686683;
    private final static int CHANCELLOR_DAMMAH = 9635;
    private final static int FORGOTTEN_KING = 29039;
    private final static int FORGOTTEN_KING_REFLECTOR_ANIMATION = -123818367;
    private long lootDropTime;
    private long lootTierTime;

    public Entity(TomatoData tomatoData, int id, long time) {
        this.tomatoData = tomatoData;
        this.id = id;
        creationTime = time;
        damageList = new ArrayList<>();
        statUpdates = new ArrayList<>();
        stat = new Stat();
        damagePlayer = new HashMap<>();
        playerDropped = new HashMap<>();
        playerHits = new HashMap<>();
    }

    public void entityUpdate(int type, ObjectStatusData status, long timePC) {
        updateStats(status, timePC);
        this.objectType = type;
        try {
            if (type != -1) {
                name = IdToAsset.objectName(type);
            }
        } catch (Exception e) {
        }
    }

    // TODO fix timePC
    public void updateStats(ObjectStatusData status, long timePC) {
        statUpdates.add(status);
        StasisCheck.checkManaFromStasis(this, status.stats);
        stat.setStats(status.stats);
        pos = status.pos;

        if (status.stats.length > 0) {
            lootTimers(status);
            if (isUser) {
                fame(timePC);
                tomatoData.player.charStat(charId, calculateBaseStats());
                MyInfoGUI.updatePlayer(this);
            } else if (isPlayer) {
                baseStats = calculateBaseStats();
            }
        }
        ParsePanelGUI.update(id, this);
    }

    private void lootTimers(ObjectStatusData status) {
        long time = System.currentTimeMillis();
        for (StatData sd : status.stats) {
            if (sd.statType == StatType.LD_TIMER_STAT) {
                lootDropTime = 0;
                int ld = sd.statValue;
                if (ld > 0) {
                    lootDropTime = ld * 1000L + time;
                }
            } else if (sd.statType == StatType.LT_TIMER_STAT) {
                lootTierTime = 0;
                int lt = sd.statValue;
                if (lt > 0) {
                    lootTierTime = lt * 1000L + time;
                }
            }
        }
    }

    public int maxHp() {
        if (stat.get(StatType.MAX_HP_STAT) == null) return 0;
        return stat.get(StatType.MAX_HP_STAT).statValue;
    }

    public int hp() {
        if (stat.get(StatType.HP_STAT) == null) return 0;
        return stat.get(StatType.HP_STAT).statValue;
    }

    public long getLastDamageTaken() {
        return lastDamageTaken;
    }

    public long getFirstDamageTaken() {
        return firstDamageTaken;
    }

    public String getFightTimerString() {
        long time = lastDamageTaken - firstDamageTaken;
        return DpsGUI.systemTimeToString(time);
    }

    public long getFightTimer() {
        return lastDamageTaken - firstDamageTaken;
    }

    public void entityDropped(long time) {
//        updates.add(status); // TODO fix time
    }

    /**
     * Player entity stats multiplier such as attack, exalts and other buffs.
     *
     * @return damage multiplier from player stats.
     */
    public float playerStatsMultiplier() {
        int condition = stat.get(StatType.CONDITION_STAT).statValue;
        boolean weak = (condition & ConditionBits.WEAK.value()) != 0;
        boolean damaging = (condition & ConditionBits.DAMAGING.value()) != 0;
        int attack = stat.get(StatType.ATTACK_STAT).statValue;
        float exaltDmgBonus = (float) stat.get(StatType.EXALTATION_BONUS_DAMAGE).statValue / 1000;

        if (weak) {
            return 0.5f;
        }
        float number = (attack + 25) * 0.02f;
        if (damaging) {
            number *= 1.25;
        }
        return number * exaltDmgBonus;
    }

    public void userProjectileHit(Entity attacker, Projectile projectile, long timePc) {
        if (projectile == null || projectile.getDamage() == 0) return;

        int[] conditions = new int[2];

        conditions[0] = stat.get(StatType.CONDITION_STAT) == null ? 0 : stat.get(StatType.CONDITION_STAT).statValue;
        conditions[1] = stat.get(StatType.NEW_CON_STAT) == null ? 0 : stat.get(StatType.NEW_CON_STAT).statValue;
        int defence = stat.get(StatType.DEFENSE_STAT) == null ? 0 : stat.get(StatType.DEFENSE_STAT).statValue;

        int dmg = Projectile.damageWithDefense(projectile.getDamage(), projectile.isArmorPiercing(), defence, conditions);

        if (dmg > 0) {
            Damage damage = new Damage(attacker, projectile, timePc, dmg);
            bossPhaseDamage(damage);
            addPlayerDmg(damage);
        }
    }

    public void genericDamageHit(Entity attacker, Projectile projectile, long time) {
        if (projectile == null || projectile.getDamage() == 0) return;
        Damage damage = new Damage(attacker, projectile, time);
        bossPhaseDamage(damage);
        addPlayerDmg(damage);
    }

    private void addPlayerDmg(Damage damage) {
        damageList.add(damage);
        if (damage.owner != null) {
            int id = damage.owner.id;
            Damage dmg = damagePlayer.computeIfAbsent(id, a -> new Damage(damage.owner));
            dmg.add(damage);
        }
    }

    public void userDamageTaken(Entity e, long timePc, Projectile p) {
        int dmg = p.getDamage();
        dmg = calculatePlayerDmg(dmg, p.isArmorPiercing());
        Damage d = new Damage(e, timePc, dmg);
        damageList.add(d);
    }

    private int calculatePlayerDmg(int dmg, boolean ap) {
        int def = stat.get(StatType.DEFENSE_STAT).statValue;
        int condition = stat.get(StatType.CONDITION_STAT).statValue;
        boolean invulnerable = (condition & ConditionBits.INVULNERABLE.value()) != 0;

        if (invulnerable) {
            return 0;
        }

        boolean armorBroken = (condition & ConditionBits.ARMORBROKEN.value()) != 0;
        boolean armored = (condition & ConditionBits.ARMORED.value()) != 0;
        boolean exposed = (condition & ConditionNewBits.EXPOSED.value()) != 0;
        boolean petrified = (condition & ConditionNewBits.PETRIFIED.value()) != 0;
        boolean cursed = (condition & ConditionNewBits.CURSE.value()) != 0;

        if (ap || armorBroken) {
            def = 0;
        } else if (armored) {
            def = (int) (def * 1.5);
        }
        if (exposed) {
            def -= 20;
        }

        float damage = Math.max(dmg * 0.1f, dmg - def);

        if (petrified) {
            damage = (int) (damage * 0.9f);
        }
        if (cursed) {
            damage = (int) (damage * 1.25f);
        }

        return (int) damage;
    }

    private void bossPhaseDamage(Damage damage) {
        damage.oryx3GuardDmg = objectType == ORYX_THE_MAD_GOD && stat.get(StatType.ANIMATION_STAT) != null && (stat.get(StatType.ANIMATION_STAT).statValue == ORYX_THE_MAD_GOD_GUARD_ANIMATION || stat.get(StatType.ANIMATION_STAT).statValue == ORYX_THE_MAD_GOD_GUARD_EXALTED_ANIMATION);
        damage.walledGardenReflectors = objectType == FORGOTTEN_KING && stat.get(StatType.ANIMATION_STAT) != null && (stat.get(StatType.ANIMATION_STAT).statValue == FORGOTTEN_KING_REFLECTOR_ANIMATION && tomatoData.floorPlanCrystals() == 12);
        damage.chancellorDammahDmg = objectType == CHANCELLOR_DAMMAH && !dammahCountered;
    }

    public String name() {
        if (CharacterClass.isPlayerCharacter(objectType) && stat.get(StatType.NAME_STAT) != null) {
            return stat.get(StatType.NAME_STAT).stringStatValue.split(",")[0];
        }
        return name;
    }

    public String getStatName() {
        if (stat.get(StatType.NAME_STAT) == null) return null;
        return stat.get(StatType.NAME_STAT).stringStatValue;
    }

    public String getStatGuild() {
        if (stat.get(StatType.GUILD_NAME_STAT) == null) return null;
        return stat.get(StatType.GUILD_NAME_STAT).stringStatValue;
    }

    public boolean isSeasonal() {
        if (stat.get(StatType.SEASONAL) == null) return false;
        return stat.get(StatType.SEASONAL).statValue == 1;
    }

    public boolean isCrucible() {
        if (stat.get(StatType.CRUCIBLE_STAT) == null) return false;
        return !stat.get(StatType.CRUCIBLE_STAT).stringStatValue.isEmpty();
    }

    public ArrayList<Damage> getDamageList() {
        return damageList;
    }

    public List<Damage> getPlayerDamageList() {
        return Arrays.stream(damagePlayer.values().toArray(new Damage[0])).sorted(Comparator.comparingInt(Damage::getDamage).reversed()).collect(Collectors.toList());
    }

    public int playersRemainAtKill() {
        int count = 0;
        for (int id : damagePlayer.keySet()) {
            if (!playerDropped.containsKey(id)) {
                count++;
            }
        }
        return count;
    }

    public boolean isUser() {
        return isUser;
    }

    public void isPlayer() {
        isPlayer = true;
        baseStats = calculateBaseStats();
    }

    public void setUser(int charId) {
        isUser = true;
        this.charId = charId;
        baseStats = calculateBaseStats();
    }

    private int[] calculateBaseStats() {
        int[] base = new int[8];

        base[0] = stat.get(StatType.MAX_HP_STAT).statValue - stat.get(StatType.MAX_HP_BOOST_STAT).statValue;
        base[1] = stat.get(StatType.MAX_MP_STAT).statValue - stat.get(StatType.MAX_MP_BOOST_STAT).statValue;
        base[2] = stat.get(StatType.ATTACK_STAT).statValue - stat.get(StatType.ATTACK_BOOST_STAT).statValue;
        base[3] = stat.get(StatType.DEFENSE_STAT).statValue - stat.get(StatType.DEFENSE_BOOST_STAT).statValue;
        base[4] = stat.get(StatType.SPEED_STAT).statValue - stat.get(StatType.SPEED_BOOST_STAT).statValue;
        base[5] = stat.get(StatType.DEXTERITY_STAT).statValue - stat.get(StatType.DEXTERITY_BOOST_STAT).statValue;
        base[6] = stat.get(StatType.VITALITY_STAT).statValue - stat.get(StatType.VITALITY_BOOST_STAT).statValue;
        base[7] = stat.get(StatType.WISDOM_STAT).statValue - stat.get(StatType.WISDOM_BOOST_STAT).statValue;

        return base;
    }

    /**
     * Fame update from experience points.
     *
     * @param time
     */
    private void fame(long time) {
        long exp = Long.parseLong(stat.get(StatType.EXP_STAT).stringStatValue);
        FameTracker.trackFame(charId, exp, time);
        if (tomatoData.charMap != null) {
            long fame = (exp + 40071) / 2000;
            RealmCharacter r = tomatoData.charMap.get(charId);
            if (r != null) {
                r.fame = fame;
            }
        }
    }

    /**
     * Updates player stats when drinking potions.
     *
     * @param charId User character id that is loaded in.
     * @param stats  Current base stats of user character.
     */
    public void charStat(int charId, int[] stats) {
        if (tomatoData.charMap == null) return;
        RealmCharacter r = tomatoData.charMap.get(charId);

        if (r == null) return;

        if (r.hp != stats[0]) {
            r.hp = stats[0];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.mp != stats[1]) {
            r.mp = stats[1];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.atk != stats[2]) {
            r.atk = stats[2];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.def != stats[3]) {
            r.def = stats[3];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.spd != stats[4]) {
            r.spd = stats[4];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.dex != stats[5]) {
            r.dex = stats[5];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.vit != stats[6]) {
            r.vit = stats[6];
            CharacterStatMaxingGUI.updateRealmChars();
        } else if (r.wis != stats[7]) {
            r.wis = stats[7];
            CharacterStatMaxingGUI.updateRealmChars();
        }
    }

    public void addPlayerDrop(int dropId, long time) {
        int hp = hp();
        int max = maxHp();
        String name = name();
        PlayerRemoved pr = new PlayerRemoved(dropId, hp, max, name, time);
        playerDropped.put(dropId, pr);
    }

    public double distSqrd(WorldPosData p) {
        double x = pos.x - p.x;
        double y = pos.y - p.y;
        return x * x + y * y;
    }

    public long lootDropTime(long time) {
        if (lootDropTime == 0) return lootDropTime;
        return lootDropTime - time;
    }

    public long getFightDuration() {
        return lastDamageTaken - firstDamageTaken;
    }

    public int[] damageTaken(Entity entity) {
        int[] dmg = new int[2];
        long first = 0;
        long last = Long.MAX_VALUE;
        if (entity != null) {
            first = entity.firstDamageTaken;
            last = entity.lastDamageTaken;
        }
        for (Damage d : damageList) {
            long time = d.time;
            if (first < time && time < last) {
                dmg[0] += d.damage;
                dmg[1]++;
            }
        }
        return dmg;
    }

    public void updateDamageTaken(long timePc) {
        if (firstDamageTaken == -1) {
            firstDamageTaken = timePc;
        }
        lastDamageTaken = timePc;
    }

    public boolean isBossMob() {
        String label = IdToAsset.getIdLabel(objectType);
        if (label != null) {
            String[] split = label.split(",");
            for (String s : split) {
                if (s.equals("BOSS") || s.equals("MINIBOSS")) return true;
            }
        }
        return false;
    }
}
