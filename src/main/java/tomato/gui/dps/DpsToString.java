package tomato.gui.dps;

import assets.IdToAsset;
import packets.incoming.MapInfoPacket;
import packets.incoming.NotificationPacket;
import tomato.backend.data.*;
import tomato.realmshark.enums.CharacterClass;
import util.Pair;

import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class DpsToString {

    private TomatoData data;
    private static final DecimalFormat df = new DecimalFormat("#,###,###");

    public DpsToString(TomatoData data) {
        this.data = data;
    }

    /**
     * Real time string display.
     *
     * @return logged dps output as a string.
     */
    public static String stringDmgRealtime(MapInfoPacket map, List<Entity> sortedEntityHitList, ArrayList<NotificationPacket> notifications, Entity player, long totalDungeonPcTime) {
        StringBuilder sb = new StringBuilder();

        if(DpsDisplayOptions.equipmentOption == 3) sb.append("Icons are not visible in live tab. Use \"<\" to see icons.\n\n");

        if (map != null) {
            sb.append(map.name).append(" ").append(DpsGUI.systemTimeToString(totalDungeonPcTime)).append("\n\n");
        }
        ArrayList<Pair<String, Integer>> deaths = new ArrayList<>();
        for (NotificationPacket n : notifications) {
            String name = n.message.split("\"")[9];
            int graveIcon = n.pictureType;
            deaths.add(new Pair<>(name, graveIcon));
        }
        for (Entity e : sortedEntityHitList) {
            if (e.maxHp() <= 0) continue;
            if (CharacterClass.isPlayerCharacter(e.objectType)) continue;
            sb.append(display(e, deaths, player)).append("\n");
        }

        return sb.toString();
    }

    public static String showInv(int equipmentFilter, Entity owner, Entity entity) {
        if (equipmentFilter == 0 || owner.getStatName() == null) return "";

        HashMap<Integer, Equipment>[] inv = new HashMap[4];

        for (int i = 0; i < 4; i++) {
            AtomicInteger tot = new AtomicInteger(0);
            if (inv[i] == null) inv[i] = new HashMap<>();
            for (Damage d : entity.getDamageList()) {
                if (d.owner == null || d.owner.id != owner.id || d.ownerInvntory == null) continue;

                int finalI = i;
                Equipment equipment = inv[i].computeIfAbsent(d.ownerInvntory[i], id -> new Equipment(id, d.ownerEnchants[finalI], tot));
                equipment.add(d.damage);
            }
        }

        if (equipmentFilter == 1) {
            StringBuilder s = new StringBuilder();
            s.append("[");
            for (int i = 0; i < 4; i++) {
                Equipment max = inv[i].values().stream().max(Comparator.comparingInt(e -> e.dmg)).orElseThrow(NoSuchElementException::new);
                if (i != 0) s.append(" / ");
                s.append(IdToAsset.objectName(max.id));
            }
            s.append("]");
            return s.toString();
        } else if (equipmentFilter == 2) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                s.append("\n");
                Collection<Equipment> list = inv[i].values();
                s.append("       ");
                boolean first = true;
                for (Equipment e : list) {
                    if (list.size() > 1) {
                        if (!first) s.append(" /");
                        s.append(String.format(" %.1f%% ", 100f * e.dmg / e.totalDmg.get()));
                        s.append(IdToAsset.objectName(e.id));
                    } else {
                        s.append(" ");
                        s.append(IdToAsset.objectName(e.id));
                    }
                    first = false;
                }
            }
            return s.toString();
        }

        return "";
    }

    public static String display(Entity entity, ArrayList<Pair<String, Integer>> deaths, Entity player) {
        StringBuilder sb = new StringBuilder();
        sb.append(entity.name()).append(" HP: ").append(entity.maxHp()).append(entity.getFightTimerString()).append("\n");
        List<Damage> playerDamageList = entity.getPlayerDamageList();
        int counter = 0;
        for (Damage dmg : playerDamageList) {
            boolean highlight = false;
            counter++;
            int filter = Filter.filter(dmg.owner, player);
            if (Filter.shouldFilter() && filter != 1) {
                continue;
            } else if (filter == 2) {
                highlight = true;
            }
            String name = dmg.owner.getStatName();
            String extra = "    ";
            String isMe = (dmg.owner.isUser() && DpsDisplayOptions.showMe) ? " ->" : (highlight ? ">>>" : "   ");
            int index = name.indexOf(',');
            if (index != -1) name = name.substring(0, index);
            float pers = ((float) dmg.damage * 100 / (float) entity.maxHp());
            if (dmg.oryx3GuardDmg) {
                extra = String.format("[Guarded Hits:%d Dmg:%d]", dmg.counterHits, dmg.counterDmg);
            } else if (entity.dammahCountered && dmg.chancellorDammahDmg) {
                extra = String.format("[Dammah Hits:%d Dmg:%d]", dmg.counterHits, dmg.counterDmg);
            } else if (dmg.walledGardenReflectors) {
                extra = String.format("[Garden Hits:%d Dmg:%d]", dmg.counterHits, dmg.counterDmg);
            }
            for (int id : entity.playerDropped.keySet()) {
                if (dmg.owner.id == id) {
                    PlayerRemoved pr = entity.playerDropped.get(id);
                    boolean dead = isDeadPlayer(name, deaths);
                    extra += String.format("%s %.2f%% [%s / %s]", dead ? "Died" : "Nexus", ((float) pr.hp / pr.max) * 100, df.format(pr.hp).replaceAll(",", " "), df.format(pr.max).replaceAll(",", " "));
                }
            }
            String inv = showInv(DpsDisplayOptions.equipmentOption, dmg.owner, entity);
            sb.append(String.format("%s %3d %10s DMG: %7d %6.3f%% %s %s\n", isMe, counter, name, dmg.damage, pers, extra, inv));
        }
        sb.append("\n");
        return sb.toString();
    }

    private static boolean isDeadPlayer(String name, ArrayList<Pair<String, Integer>> deaths) {
        for (Pair<String, Integer> p : deaths) {
            if (p.left().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
