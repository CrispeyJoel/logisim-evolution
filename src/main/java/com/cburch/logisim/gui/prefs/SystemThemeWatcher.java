/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.SystemTheme;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public final class SystemThemeWatcher {
  private static final long POLL_SECONDS = 3;

  // Kept in a field because preference listeners are held weakly.
  private static final PropertyChangeListener listener = event -> update();

  private static ScheduledExecutorService poller;
  private static volatile Boolean lastDark;

  private SystemThemeWatcher() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  /** Starts watching if the preference is on, and follows later changes to it. */
  public static void init() {
    AppPreferences.FollowSystemTheme.addPropertyChangeListener(listener);
    update();
  }

  /**
   * Switches the stored look and feel if it does not match the system.
   *
   * @return true if the look and feel was changed
   */
  public static boolean useSystemTheme(boolean dark) {
    if (AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) == dark) return false;
    AppPreferences.LookAndFeel.set(
        (dark ? FlatDarculaLaf.class : FlatIntelliJLaf.class).getName());
    return true;
  }

  private static synchronized void update() {
    if (AppPreferences.FollowSystemTheme.get()) {
      if (poller == null) {
        lastDark = null;
        poller = Executors.newSingleThreadScheduledExecutor(task -> {
          final var thread = new Thread(task, "SystemThemeWatcher");
          thread.setDaemon(true);
          return thread;
        });
        poller.scheduleWithFixedDelay(SystemThemeWatcher::poll, 0, POLL_SECONDS, TimeUnit.SECONDS);
      }
    } else if (poller != null) {
      poller.shutdownNow();
      poller = null;
    }
  }

  // Runs on the poller thread, because detection starts a process.
  private static void poll() {
    // An exception that escapes would cancel all later polls of the scheduled task.
    try {
      final var dark = SystemTheme.isDark();
      if (lastDark != null && lastDark == dark) return;
      lastDark = dark;
      SwingUtilities.invokeLater(() -> apply(dark));
    } catch (RuntimeException e) {
      // Try again at the next poll.
    }
  }

  private static void apply(boolean dark) {
    if (!AppPreferences.FollowSystemTheme.get() || !useSystemTheme(dark)) return;
    AppPreferences.applyThemeColors();
    WindowOptions.applyLookAndFeelGlobally(AppPreferences.LookAndFeel.get());
  }
}
