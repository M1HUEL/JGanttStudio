package com.itson.jgantt.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.itson.jgantt.app.service.LinkService;
import com.itson.jgantt.app.service.ProjectService;
import com.itson.jgantt.app.service.TaskScheduler;
import com.itson.jgantt.app.service.TaskService;
import com.itson.jgantt.infrastructure.persistence.SQLiteProjectRepository;
import com.itson.jgantt.ui.MainFrame;
import com.itson.jgantt.ui.controller.ProjectController;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep default look and feel
        }

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