// Copyright 2026 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.androidbrowserhelper.trusted;

/**
 * An opaque launcher activity used exclusively for cold shortcut launches on desktop
 * environments (e.g. ChromeOS / Android PC).
 * <p>
 * On desktop form factors, starting an opaque activity in a new task avoids
 * {@code DesktopModeCompatPolicy} translucent activity exemptions (which freeze caption
 * controls and desktop windows), while establishing a task root in the TWA package so that the
 * system taskbar running-app indicator is correctly attributed to the TWA icon.
 * <p>
 * On mobile devices, {@link ShortcutTrampolineActivity} routes cold shortcut launches directly
 * via {@link TwaLauncher} to preserve the invisible trampoline experience without rendering
 * an empty starting window.
 */
public class ColdShortcutActivity extends LauncherActivity {}
