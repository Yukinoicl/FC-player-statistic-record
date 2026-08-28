package com.fcdata.fcdataserver.config;

import java.awt.AWTException;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppTray {

    private static final Logger log = LoggerFactory.getLogger(AppTray.class);

    private final ConfigurableApplicationContext context;

    @Value("${server.port:11899}")
    private int port;

    private SystemTray tray;
    private TrayIcon trayIcon;
    private JDialog hiddenDialog;
    private JPopupMenu popup;

    public AppTray(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!LocalAppSupport.packaged()) {
            return;
        }
        EventQueue.invokeLater(this::install);
    }

    private void install() {
        if (!SystemTray.isSupported()) {
            log.warn("System tray is not supported");
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        boolean zh = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT).startsWith("zh");
        String appName = zh ? "FC26经理模式档案" : "FC26 Career Archive";
        String openLabel = zh ? "打开页面" : "Open";
        String exitLabel = zh ? "退出" : "Exit";
        String balloon = zh ? "已在后台运行。右键右下角图标可以退出。" : "Running in the background. Right-click the tray icon to quit.";

        popup = new JPopupMenu();
        popup.setLightWeightPopupEnabled(false);
        JMenuItem openItem = new JMenuItem(openLabel);
        openItem.addActionListener(e -> LocalAppSupport.openBrowser(port));
        JMenuItem exitItem = new JMenuItem(exitLabel);
        exitItem.addActionListener(e -> quit());
        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);
        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (hiddenDialog != null) {
                    hiddenDialog.setVisible(false);
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                if (hiddenDialog != null) {
                    hiddenDialog.setVisible(false);
                }
            }
        });

        trayIcon = new TrayIcon(loadIcon(), appName);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip(appName);
        trayIcon.addActionListener(e -> LocalAppSupport.openBrowser(port));
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    LocalAppSupport.openBrowser(port);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowMenu(e);
            }
        });

        tray = SystemTray.getSystemTray();
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            log.warn("Failed to add tray icon", e);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::removeIcon, "fcdata-tray-shutdown"));
        trayIcon.displayMessage(appName, balloon, TrayIcon.MessageType.INFO);
    }

    private void maybeShowMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        if (hiddenDialog == null) {
            hiddenDialog = new JDialog((Frame) null);
            hiddenDialog.setUndecorated(true);
            hiddenDialog.setAlwaysOnTop(true);
            hiddenDialog.setType(java.awt.Window.Type.UTILITY);
            hiddenDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            hiddenDialog.setSize(0, 0);
        }
        hiddenDialog.setLocation(e.getXOnScreen(), e.getYOnScreen());
        hiddenDialog.setVisible(true);
        popup.show(hiddenDialog, 0, 0);
    }

    private void quit() {
        removeIcon();
        int code = SpringApplication.exit(context, () -> 0);
        System.exit(code);
    }

    private void removeIcon() {
        try {
            if (tray != null && trayIcon != null) {
                tray.remove(trayIcon);
            }
        } catch (Exception ignored) {
        }
        if (hiddenDialog != null) {
            hiddenDialog.dispose();
        }
    }

    private static Image loadIcon() {
        try (InputStream in = AppTray.class.getResourceAsStream("/icons/app-icon.png")) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (image != null) {
                    return image;
                }
            }
        } catch (Exception ignored) {
        }
        BufferedImage fallback = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = fallback.createGraphics();
        g.setColor(new java.awt.Color(30, 30, 30));
        g.fillOval(2, 2, 28, 28);
        g.dispose();
        return fallback;
    }
}
