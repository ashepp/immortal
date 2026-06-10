/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.immortal.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dream-bounce policy (pure decisions). Background: Meta's power manager
 * force-wakes any dream ~2 min after idle, and decides ambient-vs-sleep from its
 * presence service. We relaunch the frame on every system bounce; whether the
 * frame HOLDS the screen decides permanent-frame vs presence-driven sleep.
 */
class DreamPolicyTest {

  @Test
  fun systemBounce_alwaysRelaunchesTheFrame() {
    assertTrue(DreamPolicy.shouldRelaunch(userExitAgoMs = 125_000, interactive = true))
  }

  @Test
  fun userTapExit_neverRelaunches() {
    assertFalse(DreamPolicy.shouldRelaunch(userExitAgoMs = 500, interactive = true))
  }

  @Test
  fun powerButtonSleep_neverRelaunches() {
    // Screen is off (not interactive): the user or system put the device to sleep.
    assertFalse(DreamPolicy.shouldRelaunch(userExitAgoMs = 60_000, interactive = false))
  }

  @Test
  fun bridgeInFlight_suppressesRelaunch() {
    // Tapping "Calls" cold-starts the stock launcher, whose idle dream stops and
    // fires DREAMING_STOPPED. While the bridge is in flight we must NOT relaunch
    // the frame over the stock home (the reported "Calls kicks me back").
    assertTrue(DreamPolicy.bridging(bridgeAgoMs = 0))
    assertTrue(DreamPolicy.bridging(bridgeAgoMs = 3_000))
  }

  @Test
  fun afterBridgeGrace_relaunchResumes() {
    // Long after a bridge, normal screensaver behavior is back.
    assertFalse(DreamPolicy.bridging(bridgeAgoMs = 30_000))
    // A stale/never-set bridge timestamp (huge elapsed) must not suppress.
    assertFalse(DreamPolicy.bridging(bridgeAgoMs = Long.MAX_VALUE))
  }

  @Test
  fun mainsPoweredPortals_holdTheScreen() {
    // No battery (Portal+, Mini, gen-2, TV): permanent frame, saver irrelevant.
    assertTrue(DreamPolicy.holdScreenOn(hasBattery = false, batterySaver = true, powered = false))
  }

  @Test
  fun batterySaver_releasesTheScreenOnlyWhileUnplugged() {
    // Unplugged + saver on: don't hold — presence decides photos vs sleep.
    assertFalse(DreamPolicy.holdScreenOn(hasBattery = true, batterySaver = true, powered = false))
    // Charging: permanent frame.
    assertTrue(DreamPolicy.holdScreenOn(hasBattery = true, batterySaver = true, powered = true))
    // Saver off: permanent frame on battery too.
    assertTrue(DreamPolicy.holdScreenOn(hasBattery = true, batterySaver = false, powered = false))
  }
}
