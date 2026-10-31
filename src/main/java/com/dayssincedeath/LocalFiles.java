package com.dayssincedeath;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.StandardOpenOption;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import net.runelite.client.util.Filepath;

final class LocalFiles
{
    static Filepath choose(Component parent, boolean save, String name, String extension)
    {
        Filepath.Chooser chooser = new Filepath.Chooser().setAcceptsFiles()
                .setDialogTitle(save ? "Save " + name : "Open Days Since Death backup")
                .addExtensionFilter(extension.toUpperCase() + " file", extension)
                .setDefaultExtension(extension);
        if (save) { chooser.setIsSave().setFileName(name + "." + extension); }
        else { chooser.setIsOpen(); }
        List<Filepath> paths = chooser.showDialog(parent);
        if (paths == null || paths.isEmpty()) { return null; }
        Filepath file = paths.get(0);
        if (save && file.exists() && JOptionPane.showConfirmDialog(parent,
                "Replace " + file.getFileName() + "?", "Replace file", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
        { return null; }
        return file;
    }

    static String readBackup(Filepath file) throws Exception
    {
        if (file.size() > BackupCodec.MAX_CHARS) { throw new IllegalArgumentException("Backup exceeds 1 MB"); }
        StringBuilder text = new StringBuilder();
        try (var reader = file.openReader())
        {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1)
            {
                text.append(buffer, 0, read);
                if (text.length() > BackupCodec.MAX_CHARS) { throw new IllegalArgumentException("Backup exceeds 1 MB"); }
            }
        }
        return text.toString();
    }

    static void writeText(Filepath file, String text) throws Exception
    {
        file.write(text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static void writeCard(Filepath file, BufferedImage card) throws Exception
    {
        try (var stream = file.openOutputStream(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
        {
            if (!ImageIO.write(card, "png", stream)) { throw new IllegalStateException("PNG encoder unavailable"); }
        }
    }
}
