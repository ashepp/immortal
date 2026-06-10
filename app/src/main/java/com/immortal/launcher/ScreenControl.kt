/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.immortal.launcher

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

/**
 * Turns the Portal's screen off. A normal app can't call PowerManager.goToSleep
 * (signature permission), so we use device-admin lockNow(), which the Portal has no
 * keyguard for — it simply blanks the screen and a tap wakes it.
 *
 * If the admin isn't active (provisioning didn't run `dpm set-active-admin`), every
 * call here is a logged no-op, so the app still behaves correctly — it just can't
 * force the screen off.
 */
object ScreenControl {
  private const val TAG = "ImmortalSleep"

  fun admin(context: Context) = ComponentName(context, AdminReceiver::class.java)

  fun dpm(context: Context): DevicePolicyManager =
      context.getSystemService(DevicePolicyManager::class.java)

  fun isAdminActive(context: Context): Boolean =
      runCatching { dpm(context).isAdminActive(admin(context)) }.getOrDefault(false)

  /** Blank the screen now, if we hold the device-admin force-lock policy. */
  fun sleep(context: Context) {
    if (!isAdminActive(context)) {
      Log.w(TAG, "sleep requested but device admin not active — no-op")
      return
    }
    runCatching { dpm(context).lockNow() }
        .onSuccess { Log.i(TAG, "lockNow(): screen off") }
        .onFailure { Log.w(TAG, "lockNow() failed", it) }
  }
}
