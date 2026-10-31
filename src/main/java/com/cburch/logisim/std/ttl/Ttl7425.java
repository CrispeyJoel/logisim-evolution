/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x25: dual 4-input NOR gate with strobe.
 *
 * <p>Model based on the National Semiconductor DM7425 / Texas Instruments SN7425 data sheet. Each
 * gate is a 4-input NOR whose output is gated by a strobe input with positive true logic: with the
 * strobe high the output is the NOR of the four data inputs, and with the strobe low the output is
 * off, which is high. As a formula, {@code Y = NOT(G AND (A OR B OR C OR D))}. A low strobe or a
 * high data input decides the output even if the remaining inputs are unknown or in error; any
 * other unsettled input leaves the output in error. There is no HDL model.
 *
 * <p>No gate symbol is drawn, because a strobed NOR has no single standard symbol; the pin names
 * are shown instead.
 */
public class Ttl7425 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7425";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1G = 2;
  public static final int PORT_INDEX_1C = 3;
  public static final int PORT_INDEX_1D = 4;
  public static final int PORT_INDEX_1Y = 5;
  public static final int PORT_INDEX_2Y = 6;
  public static final int PORT_INDEX_2A = 7;
  public static final int PORT_INDEX_2B = 8;
  public static final int PORT_INDEX_2G = 9;
  public static final int PORT_INDEX_2C = 10;
  public static final int PORT_INDEX_2D = 11;

  private static final int DELAY = 1;
  private static final byte PIN_COUNT = 14;
  private static final byte[] OUTPUT_PINS = {6, 8};
  private static final String[] PORT_NAMES = {
    "1A", "1B", "1G (strobe, active HIGH)", "1C", "1D", "1Y",
    "2Y", "2A", "2B", "2G (strobe, active HIGH)", "2C", "2D"
  };
  /** Physical pin labels (index = pin number - 1); null for the power pins. */
  private static final String[] PIN_LABELS = {
    "1A", "1B", "1G", "1C", "1D", "1Y", null, "2Y", "2A", "2B", "2G", "2C", "2D", null
  };
  /** Port indexes of {A, B, C, D, strobe G, output Y} for each gate. */
  private static final int[][] GATES = {
    {PORT_INDEX_1A, PORT_INDEX_1B, PORT_INDEX_1C, PORT_INDEX_1D, PORT_INDEX_1G, PORT_INDEX_1Y},
    {PORT_INDEX_2A, PORT_INDEX_2B, PORT_INDEX_2C, PORT_INDEX_2D, PORT_INDEX_2G, PORT_INDEX_2Y}
  };

  public Ttl7425() {
    super(_ID, PIN_COUNT, OUTPUT_PINS, null, null, PORT_NAMES, false, DEFAULT_HEIGHT, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(painter, x, y, height, PIN_LABELS);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    for (final var gate : GATES) {
      final var data =
          state
              .getPortValue(gate[0])
              .or(state.getPortValue(gate[1]))
              .or(state.getPortValue(gate[2]))
              .or(state.getPortValue(gate[3]));
      state.setPort(gate[5], state.getPortValue(gate[4]).and(data).not(), DELAY);
    }
  }
}
