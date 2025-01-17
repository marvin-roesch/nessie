/*
 * Copyright (C) 2022 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.versioned.storage.common.config;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Function;
import org.immutables.value.Value;
import org.projectnessie.versioned.storage.common.objtypes.CommitObj;
import org.projectnessie.versioned.storage.common.objtypes.IndexObj;
import org.projectnessie.versioned.storage.common.objtypes.IndexSegmentsObj;
import org.projectnessie.versioned.storage.common.persist.Persist;

public interface StoreConfig {

  String CONFIG_REPOSITORY_ID = "repository-id";
  String DEFAULT_REPOSITORY_ID = "";

  String CONFIG_PARENTS_PER_COMMIT = "parents-per-commit";
  int DEFAULT_PARENTS_PER_COMMIT = 20;

  String CONFIG_COMMIT_TIMEOUT_MILLIS = "commit-timeout-millis";
  int DEFAULT_COMMIT_TIMEOUT_MILLIS = 5_000;

  String CONFIG_COMMIT_RETRIES = "commit-retries";
  int DEFAULT_COMMIT_RETRIES = Integer.MAX_VALUE;

  String CONFIG_RETRY_INITIAL_SLEEP_MILLIS_LOWER = "retry-initial-sleep-millis-lower";
  int DEFAULT_RETRY_INITIAL_SLEEP_MILLIS_LOWER = 5;

  String CONFIG_RETRY_INITIAL_SLEEP_MILLIS_UPPER = "retry-initial-sleep-millis-upper";
  int DEFAULT_RETRY_INITIAL_SLEEP_MILLIS_UPPER = 25;

  String CONFIG_RETRY_MAX_SLEEP_MILLIS = "retry-max-sleep-millis";
  int DEFAULT_RETRY_MAX_SLEEP_MILLIS = 250;

  String CONFIG_MAX_INCREMENTAL_INDEX_SIZE = "max-incremental-index-size";
  int DEFAULT_MAX_INCREMENTAL_INDEX_SIZE = 50 * 1024;

  String CONFIG_MAX_SERIALIZED_INDEX_SIZE = "max-serialized-index-size";
  int DEFAULT_MAX_SERIALIZED_INDEX_SIZE = 200 * 1024;

  String CONFIG_MAX_REFERENCE_STRIPES_PER_COMMIT = "max-reference-stripes-per-commit";
  int DEFAULT_MAX_REFERENCE_STRIPES_PER_COMMIT = 50;

  String CONFIG_ASSUMED_WALL_CLOCK_DRIFT_MICROS = "assumed-wall-clock-drift-micros";
  long DEFAULT_ASSUMED_WALL_CLOCK_DRIFT_MICROS = 5_000_000L;

  String CONFIG_NAMESPACE_VALIDATION = "namespace-validation";
  boolean DEFAULT_NAMESPACE_VALIDATION = true;

  /**
   * Committing operations by default enforce that all (parent) namespaces exist.
   *
   * <p>This configuration setting is only present for a few Nessie releases to work around
   * potential migration issues and is subject to removal.
   *
   * @since 0.52.0
   */
  @Value.Default
  default boolean validateNamespaces() {
    return DEFAULT_NAMESPACE_VALIDATION;
  }

  /**
   * A free-form string that identifies a particular Nessie storage repository.
   *
   * <p>When remote (shared) storage is used, multiple Nessie repositories may co-exist in the same
   * database (and in the same schema). In that case this configuration parameter can be used to
   * distinguish those repositories.
   *
   * <p>All {@link org.projectnessie.versioned.storage.common.persist.Persist} implementations must
   * respect this parameter.
   */
  @Value.Default
  default String repositoryId() {
    return DEFAULT_REPOSITORY_ID;
  }

  /**
   * Used when committing to Nessie, when the HEAD (or tip) of a branch changed during the commit,
   * this value defines the maximum number of retries. Default is {@value #DEFAULT_COMMIT_RETRIES},
   * which means unlimited.
   *
   * @see #commitTimeoutMillis()
   * @see #retryInitialSleepMillisLower()
   * @see #retryInitialSleepMillisUpper()
   * @see #retryMaxSleepMillis()
   */
  @Value.Default
  default int commitRetries() {
    return DEFAULT_COMMIT_RETRIES;
  }

  /**
   * Timeout for CAS-like operations in milliseconds. Default is {@value
   * #DEFAULT_COMMIT_TIMEOUT_MILLIS} milliseconds.
   *
   * @see #commitRetries()
   * @see #retryInitialSleepMillisLower()
   * @see #retryInitialSleepMillisUpper()
   * @see #retryMaxSleepMillis()
   */
  @Value.Default
  default long commitTimeoutMillis() {
    return DEFAULT_COMMIT_TIMEOUT_MILLIS;
  }

  /**
   * When the commit logic has to retry an operation due to a concurrent, conflicting update to the
   * database state, usually a concurrent change to a branch HEAD, this parameter defines the
   * <em>initial</em> lower bound of the sleep time. Default is {@value
   * #DEFAULT_RETRY_INITIAL_SLEEP_MILLIS_LOWER} ms.
   *
   * @see #commitRetries()
   * @see #commitTimeoutMillis()
   * @see #retryInitialSleepMillisUpper()
   * @see #retryMaxSleepMillis()
   */
  @Value.Default
  default long retryInitialSleepMillisLower() {
    return DEFAULT_RETRY_INITIAL_SLEEP_MILLIS_LOWER;
  }

  /**
   * When the commit logic has to retry an operation due to a concurrent, conflicting update to the
   * database state, usually a concurrent change to a branch HEAD, this parameter defines the
   * <em>initial</em> upper bound of the sleep time. Default is {@value
   * #DEFAULT_RETRY_INITIAL_SLEEP_MILLIS_UPPER} ms.
   *
   * @see #commitRetries()
   * @see #commitTimeoutMillis()
   * @see #retryInitialSleepMillisLower()
   * @see #retryMaxSleepMillis()
   */
  @Value.Default
  default long retryInitialSleepMillisUpper() {
    return DEFAULT_RETRY_INITIAL_SLEEP_MILLIS_UPPER;
  }

  /**
   * When the commit logic has to retry an operation due to a concurrent, conflicting update to the
   * database state, usually a concurrent change to a branch HEAD, this parameter defines the
   * <em>maximum</em> sleep time. Each retry doubles the {@link #retryInitialSleepMillisLower()
   * lower} and {@link #retryInitialSleepMillisUpper() upper} bounds of the random sleep time,
   * unless the doubled upper bound would exceed the value of this configuration property. Default
   * is {@value #DEFAULT_RETRY_MAX_SLEEP_MILLIS} ms.
   *
   * @see #commitRetries()
   * @see #commitTimeoutMillis()
   * @see #retryInitialSleepMillisLower()
   * @see #retryInitialSleepMillisUpper()
   */
  @Value.Default
  default long retryMaxSleepMillis() {
    return DEFAULT_RETRY_MAX_SLEEP_MILLIS;
  }

  /**
   * The number of parent-commit-hashes stored in {@link CommitObj#tail()}. Defaults to {@value
   * #DEFAULT_PARENTS_PER_COMMIT}.
   */
  @Value.Default
  default int parentsPerCommit() {
    return DEFAULT_PARENTS_PER_COMMIT;
  }

  /**
   * The maximum allowed serialized size of a {@link
   * org.projectnessie.versioned.storage.common.indexes.StoreIndex store index}. This value is used
   * to determine, when elements in a {@link IndexObj#index() reference index segment} need to be
   * split, defaults to {@value #DEFAULT_MAX_SERIALIZED_INDEX_SIZE}.
   *
   * <p>Note: this value <em>must</em> be smaller than a database's {@link
   * Persist#hardObjectSizeLimit() hard item/row size limit}.
   */
  @Value.Default
  default int maxSerializedIndexSize() {
    return DEFAULT_MAX_SERIALIZED_INDEX_SIZE;
  }

  /**
   * The maximum allowed serialized size of a {@link
   * org.projectnessie.versioned.storage.common.indexes.StoreIndex store index}. This value is used
   * to determine, when elements in a {@link CommitObj#incrementalIndex() commit's incremental
   * index}, which were kept from previous commits, need to be pushed to a new or updated {@link
   * CommitObj#referenceIndex() reference index}, defaults to {@value
   * #DEFAULT_MAX_SERIALIZED_INDEX_SIZE}.
   *
   * <p>Note: this value <em>must</em> be smaller than a database's {@link
   * Persist#hardObjectSizeLimit() hard item/row size limit}.
   */
  @Value.Default
  default int maxIncrementalIndexSize() {
    return DEFAULT_MAX_INCREMENTAL_INDEX_SIZE;
  }

  /**
   * If the external reference index for this commit consists of up to this amount of stripes, the
   * references to the stripes will be stored {@link CommitObj#referenceIndexStripes() inside} the
   * commit object, if there are more than this amount of stripes, an external {@link
   * IndexSegmentsObj} will be created instead, referenced via {@link CommitObj#referenceIndex()}.
   */
  @Value.Default
  default int maxReferenceStripesPerCommit() {
    return DEFAULT_MAX_REFERENCE_STRIPES_PER_COMMIT;
  }

  /**
   * The assumed wall-clock drift between multiple Nessie instances in microseconds, defaults to
   * {@value #DEFAULT_ASSUMED_WALL_CLOCK_DRIFT_MICROS}.
   */
  @Value.Default
  default long assumedWallClockDriftMicros() {
    return DEFAULT_ASSUMED_WALL_CLOCK_DRIFT_MICROS;
  }

  /** The {@link Clock} to use, do not change for production. */
  @Value.Default
  default Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Retrieves the current timestamp in microseconds since epoch, using the configured {@link
   * #clock()}.
   */
  @Value.Redacted
  @Value.Auxiliary
  @Value.NonAttribute
  default long currentTimeMicros() {
    Instant instant = clock().instant();
    long time = instant.getEpochSecond();
    long nano = instant.getNano();
    return SECONDS.toMicros(time) + NANOSECONDS.toMicros(nano);
  }

  @Value.Immutable
  interface Adjustable extends StoreConfig {
    static Adjustable empty() {
      return ImmutableAdjustable.builder().build();
    }

    default Adjustable from(StoreConfig config) {
      return ImmutableAdjustable.builder().from(this).from(config).build();
    }

    default Adjustable fromFunction(Function<String, String> configFunction) {
      Adjustable a = this;
      String v;

      v = configFunction.apply(CONFIG_REPOSITORY_ID);
      if (v != null) {
        a = a.withRepositoryId(v);
      }
      v = configFunction.apply(CONFIG_COMMIT_RETRIES);
      if (v != null) {
        a = a.withCommitRetries(Integer.parseInt(v.trim()));
      }
      v = configFunction.apply(CONFIG_COMMIT_TIMEOUT_MILLIS);
      if (v != null) {
        a = a.withCommitTimeoutMillis(Long.parseLong(v.trim()));
      }
      v = configFunction.apply(CONFIG_RETRY_INITIAL_SLEEP_MILLIS_LOWER);
      if (v != null) {
        a = a.withRetryInitialSleepMillisLower(Long.parseLong(v.trim()));
      }
      v = configFunction.apply(CONFIG_RETRY_INITIAL_SLEEP_MILLIS_UPPER);
      if (v != null) {
        a = a.withRetryInitialSleepMillisUpper(Long.parseLong(v.trim()));
      }
      v = configFunction.apply(CONFIG_RETRY_MAX_SLEEP_MILLIS);
      if (v != null) {
        a = a.withRetryMaxSleepMillis(Long.parseLong(v.trim()));
      }
      v = configFunction.apply(CONFIG_PARENTS_PER_COMMIT);
      if (v != null) {
        a = a.withParentsPerCommit(Integer.parseInt(v.trim()));
      }
      v = configFunction.apply(CONFIG_MAX_INCREMENTAL_INDEX_SIZE);
      if (v != null) {
        a = a.withMaxIncrementalIndexSize(Integer.parseInt(v.trim()));
      }
      v = configFunction.apply(CONFIG_MAX_SERIALIZED_INDEX_SIZE);
      if (v != null) {
        a = a.withMaxSerializedIndexSize(Integer.parseInt(v.trim()));
      }
      v = configFunction.apply(CONFIG_MAX_REFERENCE_STRIPES_PER_COMMIT);
      if (v != null) {
        a = a.withMaxReferenceStripesPerCommit(Integer.parseInt(v.trim()));
      }
      v = configFunction.apply(CONFIG_ASSUMED_WALL_CLOCK_DRIFT_MICROS);
      if (v != null) {
        a = a.withAssumedWallClockDriftMicros(Integer.parseInt(v.trim()));
      }
      v = configFunction.apply(CONFIG_NAMESPACE_VALIDATION);
      if (v != null) {
        a = a.withValidateNamespaces(Boolean.parseBoolean(v.trim()));
      }
      return a;
    }

    /** See {@link StoreConfig#repositoryId()}. */
    Adjustable withRepositoryId(String repositoryId);

    /** See {@link StoreConfig#commitRetries()}. */
    Adjustable withCommitRetries(int commitRetries);

    /** See {@link StoreConfig#commitTimeoutMillis()}. */
    Adjustable withCommitTimeoutMillis(long commitTimeoutMillis);

    /** See {@link StoreConfig#retryInitialSleepMillisLower()}. */
    Adjustable withRetryInitialSleepMillisLower(long retryInitialSleepMillisLower);

    /** See {@link StoreConfig#retryInitialSleepMillisUpper()}. */
    Adjustable withRetryInitialSleepMillisUpper(long retryInitialSleepMillisUpper);

    /** See {@link StoreConfig#retryMaxSleepMillis()}. */
    Adjustable withRetryMaxSleepMillis(long retryMaxSleepMillis);

    /** See {@link StoreConfig#parentsPerCommit()}. */
    Adjustable withParentsPerCommit(int parentsPerCommit);

    /** See {@link StoreConfig#maxIncrementalIndexSize()}. */
    Adjustable withMaxIncrementalIndexSize(int maxIncrementalIndexSize);

    /** See {@link StoreConfig#maxSerializedIndexSize()}. */
    Adjustable withMaxSerializedIndexSize(int maxSerializedIndexSize);

    /** See {@link StoreConfig#maxReferenceStripesPerCommit()}. */
    Adjustable withMaxReferenceStripesPerCommit(int maxReferenceStripesPerCommit);

    /** See {@link StoreConfig#assumedWallClockDriftMicros()}. */
    Adjustable withAssumedWallClockDriftMicros(long assumedWallClockDriftMicros);

    /** See {@link StoreConfig#validateNamespaces ()} ()}. */
    Adjustable withValidateNamespaces(boolean validateNamespaces);

    /** See {@link StoreConfig#clock()}. */
    Adjustable withClock(Clock clock);
  }
}
