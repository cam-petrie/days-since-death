package com.dayssincedeath;

import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.Filepath;

final class DaysSinceDeathPanel extends PluginPanel
{
    interface Actions
    {
        void undo(String profile);
        void manualDate(String profile, long at);
        void note(String profile, String id, String note);
        void restore(String profile, TrackerState state);
        void recover(String profile);
        void settings(ViewSettings settings);
    }

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private final Actions actions;
    private final Gson gson;
    private TrackerView view;
    private final JLabel title = new JLabel("Days Since Death");
    private final JLabel counter = new JLabel("Log in to begin");
    private final CounterPreview overlayPreview = new CounterPreview();
    private final JTextArea summary = text();
    private final JTextArea coverage = text();
    private final JLabel status = new JLabel(" ");
    private final JButton undo = new JButton("Undo last counted death");
    private final JButton manual = new JButton("Set last-death date…");
    private final JButton export = new JButton("Export backup…");
    private final JButton restore = new JButton("Restore backup…");
    private final JButton recover = new JButton("Recover pre-restore record…");
    private final JButton share = new JButton("Create streak card…");
    private final DefaultListModel<TrackerState.Entry> historyModel = new DefaultListModel<>();
    private final JList<TrackerState.Entry> history = new JList<>(historyModel);
    private final JTextArea historyDetails = text();
    private final JTextArea note = new JTextArea(4, 15);
    private final JButton saveNote = new JButton("Save note");
    private String historySignature = "";
    private final JComboBox<DisplayUnit> unit = new JComboBox<>(DisplayUnit.values());
    private final JComboBox<TimeBasis> basis = new JComboBox<>(TimeBasis.values());
    private final JCheckBox showOverlay = new JCheckBox("Show counter");
    private final JCheckBox showLabel = new JCheckBox("Show Last Death label");
    private final JComboBox<LabelStyle> labelStyle = new JComboBox<>(LabelStyle.values());
    private final JCheckBox showSeconds = new JCheckBox("Show seconds");
    private final JCheckBox lms = new JCheckBox("Exclude LMS deaths");
    private final JCheckBox arena = new JCheckBox("Exclude PvP Arena deaths");
    private final JSlider opacity = new JSlider(0, 255, 160);
    private Color colour = Color.WHITE;
    private boolean optionsLoaded;

    DaysSinceDeathPanel(Actions actions, Gson gson)
    {
        this.actions = actions; this.gson = gson;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        setPreferredSize(new Dimension(240, 690));
        summary.setRows(16);
        basis.setRenderer(new DefaultListCellRenderer()
        {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
            {
                return super.getListCellRendererComponent(list,
                        value == TimeBasis.LOGGED_IN ? "Observed logged-in time" : "Real-world time", index, selected, focus);
            }
        });
        JPanel header = vertical();
        JLabel preview = new JLabel("LOCAL PREVIEW");
        preview.setForeground(new Color(213, 185, 119));
        counter.setFont(counter.getFont().deriveFont(Font.BOLD, 22f));
        header.add(preview); header.add(title); header.add(counter);
        overlayPreview.setToolTipText("Preview of the compact in-game counter");
        header.add(overlayPreview);
        add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        JPanel record = vertical();
        record.add(summary); record.add(undo); record.add(manual);
        record.add(export); record.add(restore); record.add(recover); record.add(share);
        tabs.addTab("Record", new JScrollPane(record));
        tabs.addTab("Log", historyPanel());
        tabs.addTab("Gaps", new JScrollPane(coverage));
        tabs.addTab("Setup", new JScrollPane(optionsPanel()));
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        undo.addActionListener(e -> {
            TrackerView captured = view;
            if (captured != null && JOptionPane.showConfirmDialog(this,
                    "Restore the streak before the last counted death?\nThe entry stays in history as UNDONE.",
                    "Undo death", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            { actions.undo(captured.profile); }
        });
        manual.addActionListener(e -> manualDate());
        export.addActionListener(e -> exportBackup());
        restore.addActionListener(e -> restoreBackup());
        recover.addActionListener(e -> {
            TrackerView captured = view;
            if (captured != null && JOptionPane.showConfirmDialog(this,
                    "Restore the recovery snapshot saved immediately before the last backup restore?",
                    "Recover prior record", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            { actions.recover(captured.profile); }
        });
        share.addActionListener(e -> shareCard());
    }

    private JPanel historyPanel()
    {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        history.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        history.setVisibleRowCount(6);
        history.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(stamp(value.at) + " · " + value.type);
            label.setOpaque(true);
            label.setBackground(selected ? new Color(81, 75, 56) : list.getBackground());
            label.setForeground(list.getForeground());
            label.setBorder(BorderFactory.createEmptyBorder(5, 4, 5, 4));
            return label;
        });
        history.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) { return; }
            TrackerState.Entry entry = history.getSelectedValue();
            historyDetails.setText(entry == null ? "Select a recorded event." : entry.type + "\n" + stamp(entry.at)
                    + "\n" + entry.location + "\nPrevious streak: " + DisplayUnit.AUTOMATIC.format(entry.streakMillis)
                    + "\nLogged-in time: " + DisplayUnit.AUTOMATIC.format(entry.playMillis));
            note.setText(entry == null ? "" : entry.note);
            saveNote.setEnabled(entry != null);
        });
        p.add(new JScrollPane(history), BorderLayout.NORTH);
        JPanel details = vertical();
        historyDetails.setRows(8);
        details.add(historyDetails); details.add(new JLabel("Personal note (max. 500 characters)"));
        note.setLineWrap(true); note.setWrapStyleWord(true);
        details.add(new JScrollPane(note)); details.add(saveNote);
        p.add(new JScrollPane(details), BorderLayout.CENTER);
        saveNote.setEnabled(false);
        saveNote.addActionListener(e -> {
            TrackerState.Entry selected = history.getSelectedValue();
            if (view != null && selected != null) { actions.note(view.profile, selected.id, note.getText()); }
        });
        return p;
    }

    private JPanel optionsPanel()
    {
        JPanel p = vertical();
        p.add(new JLabel("Time units")); p.add(unit);
        p.add(new JLabel("Counter basis")); p.add(basis);
        p.add(showOverlay); p.add(showLabel);
        p.add(new JLabel("Label style")); p.add(labelStyle); p.add(showSeconds);
        labelStyle.setRenderer(new DefaultListCellRenderer()
        {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
            {
                String text = value == LabelStyle.LD ? "LD" : value == LabelStyle.CLOCK ? "Clock icon"
                        : value == LabelStyle.CLOCK_LD ? "Clock icon + LD" : "Last Death";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        showSeconds.setToolTipText("Applies to Automatic format at every duration. Total-unit formats stay numeric.");
        p.add(lms); p.add(arena);
        JTextArea safe = text();
        safe.setRows(6);
        safe.setText("Exclusions recognise dedicated LMS and PvP Arena worlds only. All other deaths count. Undo is available for corrections.");
        p.add(safe);
        p.add(new JLabel("Background opacity")); p.add(opacity);
        JButton chooseColour = new JButton("Choose counter colour…");
        chooseColour.addActionListener(e -> {
            Color picked = JColorChooser.showDialog(this, "Counter colour", colour);
            if (picked != null) { colour = picked; }
        });
        p.add(chooseColour);
        JButton apply = new JButton("Apply options");
        apply.addActionListener(e -> sendOptions());
        java.awt.event.ActionListener instant = e -> {
            showSeconds.setEnabled(unit.getSelectedItem() == DisplayUnit.AUTOMATIC);
            labelStyle.setEnabled(showLabel.isSelected());
            if (optionsLoaded) { sendOptions(); }
        };
        for (JCheckBox check : new JCheckBox[]{showOverlay, showLabel, showSeconds, lms, arena}) { check.addActionListener(instant); }
        unit.addActionListener(instant); basis.addActionListener(instant); labelStyle.addActionListener(instant);
        p.add(apply);
        return p;
    }

    private void sendOptions()
    {
        ViewSettings s = new ViewSettings();
        s.unit = (DisplayUnit) unit.getSelectedItem(); s.basis = (TimeBasis) basis.getSelectedItem();
        s.showOverlay = showOverlay.isSelected(); s.showLabel = showLabel.isSelected();
        s.labelStyle = (LabelStyle) labelStyle.getSelectedItem();
        s.showSeconds = showSeconds.isSelected(); s.ignoreLms = lms.isSelected(); s.ignoreArena = arena.isSelected();
        s.opacity = opacity.getValue(); s.colour = colour; actions.settings(s);
    }

    void update(TrackerView next)
    {
        String oldProfile = view == null ? null : view.profile;
        view = next;
        if (!optionsLoaded)
        {
            ViewSettings s = next.settings;
            unit.setSelectedItem(s.unit); basis.setSelectedItem(s.basis);
            showOverlay.setSelected(s.showOverlay); showLabel.setSelected(s.showLabel); showSeconds.setSelected(s.showSeconds);
            labelStyle.setSelectedItem(s.labelStyle); labelStyle.setEnabled(s.showLabel);
            showSeconds.setEnabled(s.unit == DisplayUnit.AUTOMATIC);
            lms.setSelected(s.ignoreLms); arena.setSelected(s.ignoreArena); opacity.setValue(s.opacity); colour = s.colour;
            optionsLoaded = true;
        }
        TrackerState s = next.state;
        boolean available = s != null;
        title.setText(available ? next.character : "Days Since Death");
        counter.setText(available ? next.settings.counter(s, next.now) : next.message);
        counter.setToolTipText(counter.getText());
        overlayPreview.update(next.settings, available ? next.settings.counter(s, next.now) : "Unknown");
        overlayPreview.setVisible(next.settings.showOverlay);
        overlayPreview.setMaximumSize(overlayPreview.getPreferredSize());
        undo.setEnabled(available && s.undo != null);
        manual.setEnabled(available); export.setEnabled(available); share.setEnabled(available);
        restore.setEnabled(next.profile != null);
        recover.setEnabled(next.profile != null);
        if (!available)
        {
            summary.setText(next.message + "\n\nPreview records are separate from the published plugin.");
            coverage.setText("Log in to view character coverage, or use the offline sample preview.");
            historyModel.clear(); historySignature = "";
            note.setText(""); saveNote.setEnabled(false);
            return;
        }
        summary.setText("Real-world: " + DisplayUnit.AUTOMATIC.format(s.elapsed(next.now))
                + (s.lastDeath == 0 ? " (since tracking began)" : "")
                + "\nLogged-in streak: " + DisplayUnit.AUTOMATIC.format(s.currentPlayMillis) + (s.playtimePartial ? "+ (partial)" : "")
                + "\nBest real-world: " + (s.lastDeath == 0 ? "Unknown" : DisplayUnit.AUTOMATIC.format(s.best(next.now)))
                + "\nBest logged-in: " + DisplayUnit.AUTOMATIC.format(s.bestPlay())
                + "\nCounted deaths: " + s.deaths + "\nExcluded deaths: " + s.ignoredDeaths
                + "\nLast death: " + (s.lastDeath == 0 ? "Unknown" : stamp(s.lastDeath))
                + "\nDate source: " + (s.lastDeath == 0 ? "Not established" : s.manualDate ? "Manually supplied" : "Observed")
                + (s.corrected ? "\nRecord has user corrections." : "")
                + "\n\nLogged-in time is measured only while this preview runs. Offline time does not count toward it.");
        StringBuilder gaps = new StringBuilder("Tracking began: " + stamp(s.trackingStarted)
                + "\nLast observed online: " + stamp(s.lastSeen)
                + "\nTotal observed playtime: " + DisplayUnit.AUTOMATIC.format(s.loggedMillis)
                + "\n\nUnobserved intervals include ordinary logged-out time. They do not prove a death occurred. Deaths on mobile or another client remain unknown.\n\nRecent unobserved intervals:\n");
        if (s.gaps.isEmpty()) { gaps.append("None recorded yet."); }
        s.gaps.forEach(g -> gaps.append(stamp(g.from)).append(" → ").append(stamp(g.to)).append('\n'));
        coverage.setText(gaps.toString());
        StringBuilder signature = new StringBuilder(next.profile);
        s.history.forEach(entry -> signature.append(entry.id).append(entry.type).append(entry.note));
        if (!historySignature.equals(signature.toString()))
        {
            String selected = history.getSelectedValue() == null ? null : history.getSelectedValue().id;
            historySignature = signature.toString(); historyModel.clear();
            for (TrackerState.Entry entry : s.history)
            {
                historyModel.addElement(entry);
                if (entry.id.equals(selected) && java.util.Objects.equals(oldProfile, next.profile))
                { history.setSelectedIndex(historyModel.size() - 1); }
            }
            if (history.getSelectedIndex() < 0 && !historyModel.isEmpty()) { history.setSelectedIndex(0); }
        }
    }

    void message(String message)
    {
        status.setText(message); status.setToolTipText(message);
    }

    private void manualDate()
    {
        TrackerView captured = view;
        if (captured == null || captured.state == null) { return; }
        String result = JOptionPane.showInputDialog(this,
                "Last-death date in your local timezone:\nyyyy-MM-dd HH:mm\nThis is labelled manually supplied.",
                stamp(captured.state.lastDeath == 0 ? captured.now : captured.state.lastDeath));
        if (result == null) { return; }
        try
        {
            long at = LocalDateTime.parse(result.trim(), DATE).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            if (at > captured.now || at <= 0) { throw new IllegalArgumentException("Use a date in the past"); }
            if (JOptionPane.showConfirmDialog(this, "Replace the recorded date with " + result + "?\nLogged-in streak starts at zero; old playtime is unknown.",
                    "Set manual date", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            { actions.manualDate(captured.profile, at); }
        }
        catch (RuntimeException e) { error("Use a valid past date: yyyy-MM-dd HH:mm"); }
    }

    private void exportBackup()
    {
        TrackerView captured = view;
        if (captured == null || captured.state == null) { return; }
        Filepath file = LocalFiles.choose(this, true, "days-since-death-backup", "json");
        if (file == null) { return; }
        String json = BackupCodec.encode(gson, captured.state, captured.character, captured.now);
        background(() -> { LocalFiles.writeText(file, json); return "Backup saved"; }, this::message);
    }

    private void restoreBackup()
    {
        TrackerView captured = view;
        if (captured == null || captured.profile == null) { return; }
        Filepath file = LocalFiles.choose(this, false, "backup", "json");
        if (file == null) { return; }
        background(() -> BackupCodec.decode(gson, LocalFiles.readBackup(file), System.currentTimeMillis()), backup -> {
            String preview = "Backup character: " + backup.character + "\nTarget character: " + captured.character
                    + "\nLast death: " + (backup.state.lastDeath == 0 ? "Unknown" : stamp(backup.state.lastDeath))
                    + "\nCounted deaths: " + backup.state.deaths + "\nHistory entries: " + backup.state.history.size()
                    + "\n\nReplace this character's LOCAL PREVIEW record?\nA recovery snapshot of the current preview record is kept.";
            if (JOptionPane.showConfirmDialog(this, preview, "Preview backup restore", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            { actions.restore(captured.profile, backup.state); }
        });
    }

    private void shareCard()
    {
        TrackerView captured = view;
        if (captured == null || captured.state == null) { return; }
        JCheckBox includeName = new JCheckBox("Include character name", false);
        JLabel image = new JLabel();
        Runnable render = () -> image.setIcon(new ImageIcon(ShareCard.render(captured, includeName.isSelected())
                .getScaledInstance(500, 280, java.awt.Image.SCALE_SMOOTH)));
        includeName.addActionListener(e -> render.run()); render.run();
        JPanel p = new JPanel(new BorderLayout(0, 8)); p.add(image, BorderLayout.CENTER); p.add(includeName, BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(this, p, "Streak card preview", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) { return; }
        Filepath file = LocalFiles.choose(this, true, "days-since-death-card", "png");
        if (file == null) { return; }
        BufferedImage card = ShareCard.render(captured, includeName.isSelected());
        background(() -> { LocalFiles.writeCard(file, card); return "Card saved; nothing was uploaded"; }, this::message);
    }

    private <T> void background(Callable<T> task, Consumer<T> complete)
    {
        message("Working…");
        new SwingWorker<T, Void>()
        {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done()
            {
                try { complete.accept(get()); }
                catch (Exception e)
                {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    error(cause.getMessage() == null ? "Operation failed" : cause.getMessage());
                }
            }
        }.execute();
    }

    private void error(String message)
    {
        message(message);
        JOptionPane.showMessageDialog(this, message, "Days Since Death", JOptionPane.ERROR_MESSAGE);
    }

    private static JPanel vertical() { return new StackPanel(); }

    private static final class StackPanel extends JPanel implements Scrollable
    {
        StackPanel()
        {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        }
        @Override protected void addImpl(Component component, Object constraints, int index)
        {
            if (getComponentCount() > 0) { super.addImpl(Box.createVerticalStrut(7), null, -1); }
            if (component instanceof JComponent)
            {
                JComponent c = (JComponent) component;
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                c.setMinimumSize(new Dimension(0, c.getPreferredSize().height));
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            }
            super.addImpl(component, constraints, index);
        }
        public Dimension getPreferredScrollableViewportSize() { return new Dimension(210, 500); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return 120; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private static JTextArea text()
    {
        JTextArea a = new JTextArea(); a.setEditable(false); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setOpaque(false); a.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4)); return a;
    }
    static String stamp(long at) { return DATE.format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())); }

    private static final class CounterPreview extends JComponent
    {
        private ViewSettings settings = new ViewSettings();
        private String counter = "Unknown";
        CounterPreview() { setFont(UIManager.getFont("Label.font")); setOpaque(false); }
        void update(ViewSettings settings, String counter)
        { this.settings = settings; this.counter = counter; revalidate(); repaint(); }
        @Override public Dimension getPreferredSize()
        { return CounterRenderer.size(getFontMetrics(getFont()), settings, counter); }
        @Override protected void paintComponent(java.awt.Graphics graphics)
        {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            try { CounterRenderer.render(g, settings, counter); } finally { g.dispose(); }
        }
    }
}
