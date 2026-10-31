/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SystemThemeTest {
  @Test
  void macDark() {
    assertTrue(SystemTheme.parseMac("Dark\n"));
  }

  @Test
  void macLightOrFailed() {
    assertFalse(SystemTheme.parseMac(null));
    assertFalse(SystemTheme.parseMac(""));
    assertFalse(SystemTheme.parseMac("Light\n"));
    assertFalse(SystemTheme.parseMac("not a theme"));
  }

  @Test
  void windowsDark() {
    final var output = "\r\nHKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion"
        + "\\Themes\\Personalize\r\n    AppsUseLightTheme    REG_DWORD    0x0\r\n\r\n";
    assertTrue(SystemTheme.parseWindows(output));
  }

  @Test
  void windowsLightOrFailed() {
    assertFalse(SystemTheme.parseWindows("    AppsUseLightTheme    REG_DWORD    0x1\r\n"));
    assertFalse(SystemTheme.parseWindows(null));
    assertFalse(SystemTheme.parseWindows(""));
    assertFalse(SystemTheme.parseWindows("ERROR: The system was unable to find the key."));
    assertFalse(SystemTheme.parseWindows("not a theme"));
  }

  @Test
  void windowsValueTooLongDoesNotThrow() {
    assertFalse(SystemTheme.parseWindows("AppsUseLightTheme REG_DWORD 0xFFFFFFFFFFFFFFFFF"));
    assertFalse(SystemTheme.parseWindows("AppsUseLightTheme REG_DWORD 0x000000000"));
  }

  @Test
  void linuxDark() {
    assertTrue(SystemTheme.parseLinux("(<<uint32 1>>,)\n"));
  }

  @Test
  void linuxLightOrFailed() {
    assertFalse(SystemTheme.parseLinux(null));
    assertFalse(SystemTheme.parseLinux(""));
    assertFalse(SystemTheme.parseLinux("not a theme"));
    assertFalse(SystemTheme.parseLinux("(<<uint32 0>>,)\n"));
    assertFalse(SystemTheme.parseLinux("(<<uint32 2>>,)\n"));
    assertFalse(SystemTheme.parseLinux("(<<uint32 7>>,)\n"));
    assertFalse(SystemTheme.parseLinux("(<<uint32 10>>,)\n"));
  }
}
