package com.dayssincedeath;

import com.google.gson.Gson;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Offline sample UI: never starts RuneLite or reads/writes a character profile. */
public final class PreviewLauncher
{
    static final class Demo implements DaysSinceDeathPanel.Actions
    {
        TrackerState state = sample(System.currentTimeMillis());
        ViewSettings settings = new ViewSettings();
        DaysSinceDeathPanel panel;
        TrackerState recovery;

        TrackerView view()
        { return new TrackerView("sample", "Sample Adventurer", state.copy(), System.currentTimeMillis(), "", settings); }
        void refresh() { panel.update(view()); }
        public void undo(String profile) { apply(() -> state.undoDeath()); }
        public void manualDate(String profile, long at) { apply(() -> state.setManualDate(at, System.currentTimeMillis())); }
        public void note(String profile, String id, String text) { apply(() -> state.setNote(id, text)); }
        public void restore(String profile, TrackerState restored)
        {
            apply(() -> {
                restored.validate(System.currentTimeMillis()); recovery = state.copy();
                state = restored.copy(); state.undo = null; state.corrected = true; state.resumed(System.currentTimeMillis());
            });
        }
        public void recover(String profile)
        {
            apply(() -> {
                if (recovery == null) { throw new IllegalStateException("No pre-restore snapshot yet"); }
                TrackerState prior = recovery; recovery = state.copy(); state = prior.copy(); state.corrected = true;
            });
        }
        public void settings(ViewSettings options) { settings = options; refresh(); panel.message("Sample options applied"); }
        void apply(Runnable action)
        {
            try { action.run(); refresh(); panel.message("Sample updated in memory only"); }
            catch (RuntimeException e) { panel.message(e.getMessage()); }
        }
    }

    static TrackerState sample(long now)
    {
        long day = 86_400_000L;
        TrackerState s = TrackerState.start(now - 30 * day);
        s.died(now - 20 * day, "Lumbridge · Region 12850", null);
        s.addPlaytime(3000);
        s.died(now - 8 * day, "Varrock · Region 12853", null);
        s.died(now - 4 * day, "Last Man Standing", "Excluded: Last Man Standing world");
        s.setNote(s.history.get(0).id, "Practice round; does not reset my streak.");
        s.loggedMillis = 42 * 3_600_000L;
        s.currentPlayMillis = 7 * 3_600_000L;
        s.bestPlayMillis = 25 * 3_600_000L;
        s.undo.currentPlayMillis = 19 * 3_600_000L;
        s.lastSeen = now - 2 * 3_600_000L;
        s.resumed(now);
        s.validate(now);
        return s;
    }

    public static void main(String[] args) throws Exception
    {
        boolean render = args.length > 0 && args[0].startsWith("--render=");
        if (render) { System.setProperty("java.awt.headless", "true"); }
        SwingUtilities.invokeAndWait(() -> {
            try
            {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
                UIManager.put("Panel.background", new Color(38, 42, 45));
                UIManager.put("Label.foreground", new Color(232, 234, 232));
                UIManager.put("TextArea.foreground", new Color(232, 234, 232));
                UIManager.put("List.background", new Color(38, 42, 45));
                UIManager.put("List.foreground", new Color(232, 234, 232));
                Demo demo = new Demo();
                demo.panel = new DaysSinceDeathPanel(demo, new Gson()); demo.refresh();
                if (render)
                {
                    Path folder = Path.of(args[0].substring("--render=".length())); Files.createDirectories(folder);
                    demo.panel.setSize(300, 860);
                    JTabbedPane tabs = tabs(demo.panel);
                    for (int i = 0; i < tabs.getTabCount(); i++)
                    {
                        tabs.setSelectedIndex(i); layout(demo.panel);
                        BufferedImage image = new BufferedImage(300, 860, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics(); demo.panel.printAll(g); g.dispose();
                        ImageIO.write(image, "png", folder.resolve(tabs.getTitleAt(i).toLowerCase() + ".png").toFile());
                    }
                    ImageIO.write(ShareCard.render(demo.view(), false), "png", folder.resolve("card-private.png").toFile());
                    ImageIO.write(ShareCard.render(demo.view(), true), "png", folder.resolve("card-named.png").toFile());
                    BufferedImage counters = new BufferedImage(520, 200, BufferedImage.TYPE_INT_RGB);
                    Graphics2D cg = counters.createGraphics();
                    cg.setColor(new Color(38, 42, 45)); cg.fillRect(0, 0, 520, 200);
                    cg.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 13));
                    ViewSettings counterSettings = new ViewSettings();
                    for (int i = 0; i < 5; i++)
                    {
                        counterSettings.showLabel = i < 4;
                        counterSettings.labelStyle = LabelStyle.values()[Math.min(i, 3)];
                        Graphics2D row = (Graphics2D) cg.create(12, 8 + i * 38, 500, 32);
                        CounterRenderer.render(row, counterSettings, "8d 2h 3m 9s");
                        row.setColor(Color.LIGHT_GRAY); row.drawString(i == 4 ? "No label" : counterSettings.labelStyle.name(), 320, 18);
                        row.dispose();
                    }
                    cg.dispose(); ImageIO.write(counters, "png", folder.resolve("counter-styles.png").toFile());
                    System.out.println("Preview renders: " + folder.toAbsolutePath());
                    return;
                }
                JFrame frame = new JFrame("Days Since Death · Offline sample preview");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout()); frame.add(demo.panel, BorderLayout.CENTER);
                JPanel controls = new JPanel();
                JButton counted = new JButton("Sample death");
                counted.addActionListener(e -> demo.apply(() -> demo.state.died(System.currentTimeMillis(), "Sample location", null)));
                JButton ignored = new JButton("Sample LMS death");
                ignored.addActionListener(e -> demo.apply(() -> demo.state.died(System.currentTimeMillis(), "Sample LMS world",
                        demo.settings.ignoreLms ? "Excluded: Last Man Standing world" : null)));
                JButton reset = new JButton("Reset samples");
                reset.addActionListener(e -> demo.apply(() -> demo.state = sample(System.currentTimeMillis())));
                controls.add(counted); controls.add(ignored); controls.add(reset);
                frame.add(controls, BorderLayout.SOUTH);
                frame.setSize(540, 850); frame.setLocationByPlatform(true); frame.setVisible(true);
                new Timer(1000, e -> { demo.state.addPlaytime(1000); demo.state.lastSeen = System.currentTimeMillis(); demo.refresh(); }).start();
            }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    private static JTabbedPane tabs(Container parent)
    {
        for (Component c : parent.getComponents())
        {
            if (c instanceof JTabbedPane) { return (JTabbedPane) c; }
            if (c instanceof Container)
            {
                JTabbedPane found = tabs((Container) c); if (found != null) { return found; }
            }
        }
        return null;
    }
    private static void layout(Container parent)
    { parent.doLayout(); for (Component c : parent.getComponents()) { if (c instanceof Container) { layout((Container) c); } } }
}
