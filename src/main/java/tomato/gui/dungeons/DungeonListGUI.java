package tomato.gui.dungeons;

import tomato.backend.data.TomatoData;
import tomato.gui.TomatoGUI;
import tomato.gui.security.ParsePanelGUI;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DungeonListGUI extends JPanel {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static DungeonListGUI INSTANCE;

    public static DungeonListGUI getInstance() {
        return INSTANCE;
    }

    private TomatoData data;
    
    private Component dungeonsPanel;
    private final FixedSizedArray<Dungeon> visible;
    private boolean isMissingCurrentDungeonInformation = true;
    
    public DungeonListGUI(TomatoData data) {
        INSTANCE = this;
        this.data = data;
        this.visible = new FixedSizedArray<>(5);
        
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
    
    
    public void updateRender(final List<Dungeon> dungeons) {
        final JPanel dungeonsPanel = new JPanel();
        dungeonsPanel.setLayout(new BoxLayout(dungeonsPanel, BoxLayout.Y_AXIS));

        if (this.isMissingCurrentDungeonInformation())
            this.renderMissedCurrentDungeonMap(dungeonsPanel);
        
        if (!dungeons.isEmpty()) {
            this.visible.add(dungeons.get(dungeons.size() - 1));

            this.removeAll();
            this.add(new JButton("clear"));

            for (final Dungeon dungeon : this.visible) {
                renderDungeon(dungeon, dungeonsPanel);
            }
        }

        final JScrollPane scrollPane = new JScrollPane(dungeonsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setModel(scrollPane.getVerticalScrollBar().getModel());
        scrollPane.getVerticalScrollBar().setUnitIncrement(9);

        this.add(scrollPane);
        this.revalidate();
        this.repaint();
    }

    private boolean isMissingCurrentDungeonInformation() {
        return this.isMissingCurrentDungeonInformation;
    }

    public void setMissingCurrentDungeonInformation(boolean missingCurrentDungeonInformation) {
        this.isMissingCurrentDungeonInformation = missingCurrentDungeonInformation;
    }

    public void renderDungeon(final Dungeon dungeon, final JPanel dungeonsPanel) {
        final JPanel dungeonPanel = new JPanel();
        dungeonPanel.setLayout(new BoxLayout(dungeonPanel, BoxLayout.Y_AXIS));
        dungeonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel dungeonNameLabel = new JLabel(String.format("<html><strong>%s</strong></html>", dungeon.getMapInfo().getName()));
        dungeonNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dungeonPanel.add(dungeonNameLabel);

        final JPanel values = new JPanel();
        values.setLayout(new BoxLayout(values, BoxLayout.X_AXIS));
        values.setAlignmentX(Component.LEFT_ALIGNMENT);

        values.add(Box.createHorizontalStrut(10));
        values.add(new JLabel("entered at: " + DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochSecond(dungeon.getMapInfo().getGameOpenedTime()), ZoneId.systemDefault()))));
        values.add(Box.createHorizontalStrut(10));
        values.add(new JLabel("total objects: " + dungeon.getObjects().size()));
        values.add(Box.createHorizontalStrut(10));
        values.add(new JLabel("total tiles: " + dungeon.getTiles().size()));
        values.add(Box.createHorizontalStrut(10)); 
        final JButton exportButton = new JButton("export");
        exportButton.addActionListener(e -> {
            ParsePanelGUI.copyToClipboard(dungeon.export());
            JOptionPane.showMessageDialog(TomatoGUI.getFrame(), "Dungeon exported to clipboard");
        });
        values.add(exportButton);

        dungeonPanel.add(values);
        dungeonPanel.add(Box.createVerticalStrut(10));
        dungeonsPanel.add(dungeonPanel);
    }
    
    public void renderMissedCurrentDungeonMap(final JPanel dungeonsPanel){
        final JPanel dungeonPanel = new JPanel();
        dungeonPanel.setLayout(new BoxLayout(dungeonPanel, BoxLayout.Y_AXIS));
        dungeonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel dungeonNameLabel = new JLabel("<html><strong>Error</strong></html>");
        dungeonNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dungeonPanel.add(dungeonNameLabel);

        final JPanel values = new JPanel();
        values.setLayout(new BoxLayout(values, BoxLayout.X_AXIS));
        values.setAlignmentX(Component.LEFT_ALIGNMENT);

        values.add(new JLabel("current dungeon info map couldn't be captured."));
        values.add(Box.createHorizontalStrut(10));

        dungeonPanel.add(values);
        dungeonPanel.add(Box.createVerticalStrut(10));
        dungeonsPanel.add(dungeonPanel);
    }
}
