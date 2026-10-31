/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.formdev.flatlaf.util.SystemInfo;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class SystemTheme {
  private static final long TIMEOUT_MS = 2000;
  private static final Pattern WINDOWS_VALUE =
      Pattern.compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x([0-9a-fA-F]{1,8})(?![0-9a-fA-F])");
  private static final Pattern PORTAL_VALUE = Pattern.compile("uint32\\s+(\\d+)");

  private SystemTheme() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  /** Returns true if the system is set to dark. Any failure counts as light. */
  public static boolean isDark() {
    if (SystemInfo.isMacOS) {
      return parseMac(run("defaults", "read", "-g", "AppleInterfaceStyle"));
    } else if (SystemInfo.isWindows) {
      return parseWindows(run("reg", "query",
          "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
          "/v", "AppsUseLightTheme"));
    } else if (SystemInfo.isLinux) {
      return parseLinux(run("gdbus", "call", "--session",
          "--dest", "org.freedesktop.portal.Desktop",
          "--object-path", "/org/freedesktop/portal/desktop",
          "--method", "org.freedesktop.portal.Settings.Read",
          "org.freedesktop.appearance", "color-scheme"));
    }
    return false;
  }

  // The key is missing in light mode, so the command fails and the output is empty.
  static boolean parseMac(String output) {
    return output != null && output.trim().equalsIgnoreCase("Dark");
  }

  static boolean parseWindows(String output) {
    if (output == null) return false;
    final var matcher = WINDOWS_VALUE.matcher(output);
    return matcher.find() && Long.parseLong(matcher.group(1), 16) == 0;
  }

  // The portal returns 0 (no preference), 1 (dark) or 2 (light).
  static boolean parseLinux(String output) {
    if (output == null) return false;
    final var matcher = PORTAL_VALUE.matcher(output);
    return matcher.find() && matcher.group(1).equals("1");
  }

  private static String run(String... command) {
    try {
      final var process = new ProcessBuilder(command)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
      if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        return "";
      }
      if (process.exitValue() != 0) return "";
      return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return "";
    } catch (Exception e) {
      // The command is missing or failed to start, so treat it as light.
      return "";
    }
  }
}
