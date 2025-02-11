// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.test;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.concurrent.ThreadSafety.ThreadSafe;
import com.google.devtools.build.lib.rules.test.TestRunnerAction.ResolvedPaths;
import com.google.devtools.build.lib.util.Pair;
import com.google.devtools.build.lib.util.Preconditions;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.view.test.TestStatus.BlazeTestStatus;
import com.google.devtools.build.lib.view.test.TestStatus.TestResultData;
import java.util.Collection;
import javax.annotation.Nullable;

/**
 * This is the event passed from the various test strategies to the {@code RecordingTestListener}
 * upon test completion.
 */
@ThreadSafe
@Immutable
public class TestResult {

  private final TestRunnerAction testAction;
  private final TestResultData data;
  private final boolean cached;
  @Nullable protected final Path execRoot;

  /**
   * Construct the TestResult for the given test / status.
   *
   * @param testAction The test that was run.
   * @param data test result protobuffer.
   * @param cached true if this is a locally cached test result.
   * @param execRoot The execution root in which the action was carried out; can be null, in which
   *     case everything depending on the execution root is ignored.
   */
  public TestResult(
      TestRunnerAction testAction, TestResultData data, boolean cached, @Nullable Path execRoot) {
    this.testAction = Preconditions.checkNotNull(testAction);
    this.data = data;
    this.cached = cached;
    this.execRoot = execRoot;
  }

  public TestResult(TestRunnerAction testAction, TestResultData data, boolean cached) {
    this(testAction, data, cached, null);
  }

  public static boolean isBlazeTestStatusPassed(BlazeTestStatus status) {
    return status == BlazeTestStatus.PASSED || status == BlazeTestStatus.FLAKY;
  }

  /**
   * @return The test action.
   */
  public TestRunnerAction getTestAction() {
    return testAction;
  }

  /**
   * @return The test log path. Note, that actual log file may no longer
   *         correspond to this artifact - use getActualLogPath() method if
   *         you need log location.
   */
  public Path getTestLogPath() {
    return testAction.getTestLog().getPath();
  }

  /**
   * Return if result was loaded from local action cache.
   */
  public final boolean isCached() {
    return cached;
  }

  /**
   * @return Coverage data artifact, if available and null otherwise.
   */
  public Path getCoverageData() {
    if (data.getHasCoverage()) {
      return testAction.getCoverageData().getPath();
    }
    return null;
  }

  /**
   * @return The test status artifact.
   */
  public Artifact getTestStatusArtifact() {
    // these artifacts are used to keep track of the number of pending and completed tests.
    return testAction.getCacheStatusArtifact();
  }


  /**
   * Gets the test name in a user-friendly format.
   * Will generally include the target name and shard number, if applicable.
   *
   * @return The test name.
   */
  public String getTestName() {
    return testAction.getTestName();
  }

  /**
   * @return The test label.
   */
  public String getLabel() {
    return Label.print(testAction.getOwner().getLabel());
  }

  /**
   * @return The test shard number.
   */
  public int getShardNum() {
    return testAction.getShardNum();
  }

  /**
   * @return Total number of test shards. 0 means
   *     no sharding, whereas 1 means degenerate sharding.
   */
  public int getTotalShards() {
    return testAction.getExecutionSettings().getTotalShards();
  }

  public TestResultData getData() {
    return data;
  }

  /**
   * @return Collection of files created by the test, tagged by their name indicating usage (e.g.,
   *     "test.log").
   */
  public Collection<Pair<String, Path>> getFiles() {
    ImmutableList.Builder<Pair<String, Path>> builder = new ImmutableList.Builder<>();
    if (testAction.getTestLog().getPath().exists()) {
      builder.add(Pair.of("test.log", testAction.getTestLog().getPath()));
    }
    if (execRoot != null) {
      ResolvedPaths resolvedPaths = testAction.resolve(execRoot);
      if (resolvedPaths.getXmlOutputPath().exists()) {
        builder.add(Pair.of("test.xml", resolvedPaths.getXmlOutputPath()));
      }
    }
    return builder.build();
  }
}
