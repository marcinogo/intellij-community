// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem.impl;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public interface FusAwareAction {
  void recordFeatureUsageStatistics(@NotNull AnActionEvent event, @NotNull FeatureUsageData data);
}
