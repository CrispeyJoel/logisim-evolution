/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 7425 dual 4-input NOR gate with strobe. */
class Ttl7425Test {
  /** {A, B, C, D, strobe G, output Y} port indexes for each gate. */
  private static final int[][] GATES = {
    {
      Ttl7425.PORT_INDEX_1A, Ttl7425.PORT_INDEX_1B, Ttl7425.PORT_INDEX_1C,
      Ttl7425.PORT_INDEX_1D, Ttl7425.PORT_INDEX_1G, Ttl7425.PORT_INDEX_1Y
    },
    {
      Ttl7425.PORT_INDEX_2A, Ttl7425.PORT_INDEX_2B, Ttl7425.PORT_INDEX_2C,
      Ttl7425.PORT_INDEX_2D, Ttl7425.PORT_INDEX_2G, Ttl7425.PORT_INDEX_2Y
    }
  };
  private static final int STROBE = 4;
  private static final int OUT = 5;

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl7425();
    final var hiddenPower = TtlTestInstanceState.createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    // Bottom row, pins 1-6.
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_1A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_1B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_1G, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_1C, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_1D, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_1Y, 110, 30, EndData.OUTPUT_ONLY);
    // Top row, pins 8-13.
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_2Y, 130, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_2A, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_2B, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_2G, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_2C, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl7425.PORT_INDEX_2D, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = TtlTestInstanceState.createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, 12, 130, 30, EndData.INPUT_ONLY); // Physical pin 7: GND
    assertPort(shownPower, 13, 10, -30, EndData.INPUT_ONLY); // Physical pin 14: VCC
  }

  @Test
  void outputIsNorOfDataInputsWhileStrobeIsHighAndHighWhileStrobeIsLow() {
    final var gate = new Ttl7425();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var pins : GATES) {
      for (var bits = 0; bits < 32; bits++) {
        driveAllLow(state);
        var anyData = false;
        for (var i = 0; i < 5; i++) {
          final var high = (bits & (1 << i)) != 0;
          state.setPortValue(pins[i], high ? Value.TRUE : Value.FALSE);
          if (high && i != STROBE) anyData = true;
        }
        final var strobe = (bits & (1 << STROBE)) != 0;
        gate.propagate(state);
        final var expected = (strobe && anyData) ? Value.FALSE : Value.TRUE;
        assertEquals(expected, state.getPortValue(pins[OUT]), "bits=" + bits);
        assertOtherOutputs(state, pins[OUT], Value.TRUE);
      }
    }
  }

  @Test
  void lowStrobeForcesOutputHighEvenIfDataInputsAreUnsettled() {
    final var gate = new Ttl7425();
    final var state = new TtlTestInstanceState(gate, false);

    driveAllLow(state);
    state.setPortValue(Ttl7425.PORT_INDEX_1A, Value.ERROR);
    state.setPortValue(Ttl7425.PORT_INDEX_1B, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl7425.PORT_INDEX_1Y));
  }

  @Test
  void aHighDataInputForcesOutputLowEvenIfOthersAreUnsettled() {
    final var gate = new Ttl7425();
    final var state = new TtlTestInstanceState(gate, false);

    driveAllLow(state);
    state.setPortValue(Ttl7425.PORT_INDEX_1G, Value.TRUE);
    state.setPortValue(Ttl7425.PORT_INDEX_1A, Value.TRUE);
    state.setPortValue(Ttl7425.PORT_INDEX_1B, Value.ERROR);
    state.setPortValue(Ttl7425.PORT_INDEX_1C, Value.UNKNOWN);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7425.PORT_INDEX_1Y));
  }

  @Test
  void unsettledDataInputThatDecidesTheOutputMakesItError() {
    final var gate = new Ttl7425();
    final var state = new TtlTestInstanceState(gate, false);
    final var pins = GATES[0];

    for (var data = 0; data < 4; data++) {
      for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
        driveAllLow(state);
        state.setPortValue(pins[STROBE], Value.TRUE);
        state.setPortValue(pins[data], unsettled);
        gate.propagate(state);
        assertEquals(Value.ERROR, state.getPortValue(pins[OUT]));
        assertOtherOutputs(state, pins[OUT], Value.TRUE);
      }
    }
  }

  @Test
  void unsettledStrobeMakesOutputErrorOnlyWhenADataInputIsHigh() {
    final var gate = new Ttl7425();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
      driveAllLow(state);
      state.setPortValue(Ttl7425.PORT_INDEX_1G, unsettled);
      state.setPortValue(Ttl7425.PORT_INDEX_1A, Value.TRUE);
      gate.propagate(state);
      assertEquals(Value.ERROR, state.getPortValue(Ttl7425.PORT_INDEX_1Y));

      // With every data input low the output is high whatever the strobe does.
      state.setPortValue(Ttl7425.PORT_INDEX_1A, Value.FALSE);
      gate.propagate(state);
      assertEquals(Value.TRUE, state.getPortValue(Ttl7425.PORT_INDEX_1Y));
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl7425();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(12, Value.FALSE);
    state.setPortValue(13, Value.TRUE);
    driveFirstGateLow(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7425.PORT_INDEX_1Y));

    state.setPortValue(13, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(13, Value.TRUE);
    driveFirstGateLow(state);
    gate.propagate(state);
    assertEquals(Value.FALSE, state.getPortValue(Ttl7425.PORT_INDEX_1Y));

    state.setPortValue(12, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static void driveAllLow(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      for (var i = 0; i < 5; i++) {
        state.setPortValue(pins[i], Value.FALSE);
      }
    }
  }

  /** Strobe high and one data input high, so that the first output is driven low. */
  private static void driveFirstGateLow(TtlTestInstanceState state) {
    driveAllLow(state);
    state.setPortValue(Ttl7425.PORT_INDEX_1G, Value.TRUE);
    state.setPortValue(Ttl7425.PORT_INDEX_1A, Value.TRUE);
  }

  private static void assertOtherOutputs(TtlTestInstanceState state, int driven, Value expected) {
    for (final var pins : GATES) {
      if (pins[OUT] != driven) {
        assertEquals(expected, state.getPortValue(pins[OUT]));
      }
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      assertEquals(Value.UNKNOWN, state.getPortValue(pins[OUT]));
    }
  }
}
