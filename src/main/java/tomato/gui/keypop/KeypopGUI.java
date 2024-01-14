package tomato.gui.keypop;

import assets.AssetMissingException;
import assets.IdToAsset;
import packets.data.enums.NotificationEffectType;
import packets.incoming.NotificationPacket;
import tomato.gui.TomatoGUI;
import tomato.realmshark.enums.CharacterStatistics;
import util.PropertiesManager;
import util.Util;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//message={"k":"s.dungeon_opened_by","t":{"player":"PLAYERNAME",}}   key pop
//message={"k":"s.something_by_player","t":{"name":"The Shield Monument has been activated","player":"PLAYERNAME",}}  rune pop
//message={"k":"s.dungeon_unlocked_by","t":{"name":"The Void","player":"PLAYERNAME",}}   vial pop
//message={"k":"s.dungeon_unlocked_by","t":{"name":"Wine Cellar","player":"PLAYERNAME",}}   Inc pop

/**
 * GUI class for popping dungeons.
 */
public class KeypopGUI extends JPanel {

    private static JTextArea textAreaKeypop;

    private static final Pattern keypopParse = Pattern.compile("[^ ]*\"player\":\"([A-Za-z]*)[^ ]*");
    private static final Pattern nonkeypopParse = Pattern.compile("[^ ]*\"name\":\"([A-Za-z ]*)\",\"player\":\"([A-Za-z]*)[^ ]*");

    private static final String NOTIFICATION_SOUND_FILE = "/sound/Notification.wav";
    private static HashSet<String> selectedDungeons = new HashSet<>();
    private static Clip notificationSoundClip;

    public KeypopGUI() {
        initAudioClip();
        loadDungeonChoices();

        setLayout(new BorderLayout());
        textAreaKeypop = new JTextArea();
        add(TomatoGUI.createTextArea(textAreaKeypop));

        JPanel south = new JPanel(new GridLayout());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> textAreaKeypop.setText(""));
        south.add(clearButton);

        JButton notificationButton = new JButton("Notifications");
        notificationButton.addActionListener(e -> showConfigureDialog());

        south.add(notificationButton);
        add(south, BorderLayout.SOUTH);
    }

    /**
     * Packet parser for notification packets that will be used to add key, vial, rune or inc pops.
     *
     * @param packet Notification packet containing info about who pops keys, vial, runes or inc pops.
     */
    public static void packet(NotificationPacket packet) {
        if (packet.effect == NotificationEffectType.PortalOpened) {
            String msg = packet.message;
            Matcher m = keypopParse.matcher(msg);
            if (m.matches()) {
                String playerName = m.group(1);
                try {
                    String dungeonName = IdToAsset.objectName(packet.pictureType);

                    appendTextAreaKeypop(String.format("%s [%s]: %s\n", Util.getHourTime(), playerName, dungeonName));

                    if (selectedDungeons.contains(dungeonName)) {
                        playNotificationSound();
                    }
                } catch (AssetMissingException e) {
                    e.printStackTrace();
                }
            }
        } else if (packet.effect == NotificationEffectType.ServerMessage && packet.message != null) {
            String msg = packet.message;
            Matcher m = nonkeypopParse.matcher(msg);
            if (m.matches()) {
                String type = m.group(1);
                String playerName = m.group(2);
                String pop = null;
                if (type.contains("Monument has been activated")) {
                    pop = type.split(" ")[1] + " Rune";
                } else if (type.equals("The Void")) {
                    pop = "Vial";
                } else if (type.equals("Wine Cellar")) {
                    pop = "Inc";
                }
                if (pop != null) {
                    appendTextAreaKeypop(String.format("%s [%s]: %s\n", Util.getHourTime(), playerName, pop));
                }
            }
        }
    }

    /**
     * Add text to the key pop text area.
     *
     * @param s The text to be added at the end of text area.
     */
    public static void appendTextAreaKeypop(String s) {
        if (textAreaKeypop != null) textAreaKeypop.append(s);
    }

    /**
     * Sets the font of the text area.
     *
     * @param font Font to be set.
     */
    public static void editFont(Font font) {
        textAreaKeypop.setFont(font);
    }

    /**
     * Loads auto clip to be played later
     */
    private void initAudioClip() {
        try {
            InputStream audioInputStream = KeypopGUI.class.getResourceAsStream(NOTIFICATION_SOUND_FILE);

            if (audioInputStream == null) {
                System.err.println("Error: Could not load audio file.");
            } else {
                InputStream bufferedIn = new BufferedInputStream(audioInputStream);
                AudioInputStream stream = AudioSystem.getAudioInputStream(bufferedIn);

                AudioFormat baseFormat = stream.getFormat();
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        44100,  // Sample rate (Hz)
                        16,     // Bit depth
                        1,      // Channels (1 for mono, 2 for stereo)
                        2,      // Frame size in bytes
                        44100,  // Frame rate (frames per second)
                        false   // Big-endian byte order
                );

                if (!AudioSystem.isConversionSupported(decodedFormat, baseFormat)) {
                    System.err.println("Error: Conversion not supported.");
                } else {
                    AudioInputStream decodedStream = AudioSystem.getAudioInputStream(decodedFormat, stream);
                    notificationSoundClip = AudioSystem.getClip();
                    notificationSoundClip.open(decodedStream);
                }
            }
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Dialog window used to display notification dungeons.
     */
    private static void showConfigureDialog() {
        JDialog configureDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(new JPanel()), "Configure Dungeons", true);
        configureDialog.setLayout(new BorderLayout());
        configureDialog.setResizable(false);

        JCheckBox[] checkboxes = createDungeonCheckboxes();

        JButton selectAllButton = new JButton("Select All");
        selectAllButton.addActionListener(e -> {
            for (JCheckBox checkbox : checkboxes) {
                checkbox.setSelected(true);
            }
        });

        JButton unselectAllButton = new JButton("Unselect All");
        unselectAllButton.addActionListener(e -> {
            for (JCheckBox checkbox : checkboxes) {
                checkbox.setSelected(false);
            }
        });

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            selectedDungeons.clear();
            for (JCheckBox checkbox : checkboxes) {
                if (checkbox.isSelected()) {
                    selectedDungeons.add(checkbox.getText());
                }
            }
            saveDungeonChoices();
            playNotificationSound();
            configureDialog.dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectAllButton);
        buttonPanel.add(unselectAllButton);
        buttonPanel.add(applyButton);

        JPanel checkboxPanel = new JPanel(new GridLayout(0, 3));
        for (JCheckBox checkbox : checkboxes) {
            checkboxPanel.add(checkbox);
        }

        configureDialog.add(checkboxPanel, BorderLayout.CENTER);
        configureDialog.add(buttonPanel, BorderLayout.SOUTH);
        configureDialog.setSize(700, 500);
        configureDialog.setLocationRelativeTo(null);
        configureDialog.setVisible(true);
    }

    /**
     * Notification selection list creation.
     *
     * @return Checkbox objects from dungeon list.
     */
    private static JCheckBox[] createDungeonCheckboxes() {
        JCheckBox[] checkboxes = new JCheckBox[CharacterStatistics.DUNGEON_NAMES.size()];
        for (int i = 0; i < CharacterStatistics.DUNGEON_NAMES.size(); i++) {
            String o = CharacterStatistics.DUNGEON_NAMES.get(i);
            checkboxes[i] = new JCheckBox(o);
            checkboxes[i].setSelected(selectedDungeons.contains(o));
        }
        return checkboxes;
    }

    /**
     * Saves selection to disk
     */
    private static void saveDungeonChoices() {
        StringBuilder sb = new StringBuilder();
        boolean first = false;
        for (String s : selectedDungeons) {
            if (first) sb.append(",");
            first = true;
            sb.append(s);
        }
        String string = sb.toString();
        PropertiesManager.setProperties("keypopSound", string);
    }

    /**
     * Loads selection from disk
     */
    private static void loadDungeonChoices() {
        String keySound = PropertiesManager.getProperty("keypopSound");

        if (keySound != null) {
            String[] list = keySound.split(",");
            selectedDungeons.addAll(Arrays.asList(list));
        }
    }

    /**
     * Plays loaded sound
     */
    private static void playNotificationSound() {
        if (notificationSoundClip != null) {
            notificationSoundClip.setFramePosition(0); // Rewind to the beginning
            notificationSoundClip.start();
        }
    }

    /**
     * Stops sound being played.
     */
    private static void stopNotificationSound() {
        if (notificationSoundClip != null && notificationSoundClip.isRunning()) {
            notificationSoundClip.stop();
        }
    }
}