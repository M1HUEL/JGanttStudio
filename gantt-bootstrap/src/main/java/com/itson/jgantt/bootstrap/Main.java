package com.itson.jgantt.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import com.formdev.flatlaf.FlatLightLaf;
import com.itson.jgantt.app.service.LinkService;
import com.itson.jgantt.app.service.ProjectService;
import com.itson.jgantt.app.service.TaskScheduler;
import com.itson.jgantt.app.service.TaskService;
import com.itson.jgantt.infrastructure.persistence.SQLiteProjectRepository;
import com.itson.jgantt.ui.MainFrame;
import com.itson.jgantt.ui.controller.ProjectController;
import com.itson.jgantt.ui.util.UiFonts;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        installLookAndFeel();

        SQLiteProjectRepository repository = new SQLiteProjectRepository(databasePath());
        TaskScheduler scheduler = new TaskScheduler();
        ProjectService projectService = new ProjectService(repository);
        TaskService taskService = new TaskService(repository, scheduler);
        LinkService linkService = new LinkService(repository, scheduler);

        ProjectController controller = new ProjectController(projectService, taskService, linkService);
        MainFrame frame = new MainFrame(controller);
        frame.initializeProject();
        frame.setVisible(true);
    }

    private static void installLookAndFeel() {
        UiFonts.register();
        try {
            UIManager.put("defaultFont", new FontUIResource(UiFonts.regular(13)));
            applyTheme();
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception alsoIgnored) {
                // keep default look and feel
            }
        }
    }

    private static void applyTheme() {
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("@accentColor", "#2563EB");
        UIManager.put("ToolBar.background", "#FFFFFF");
        UIManager.put("ToolBar.borderColor", "#E0E6EF");
        UIManager.put("ScrollPane.smoothScrolling", true);
        UIManager.put("MenuItem.arc", 8);
        UIManager.put("PopupMenu.borderColor", "#D8E2F0");
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));
    }

    private static String databasePath() {
        Path directory = Path.of(System.getProperty("user.home"), ".jganttstudio");
        try {
            Files.createDirectories(directory);
        } catch (IOException ignored) {
            // fall back to the working directory
        }
        return directory.resolve("app.db").toString();
    }
}