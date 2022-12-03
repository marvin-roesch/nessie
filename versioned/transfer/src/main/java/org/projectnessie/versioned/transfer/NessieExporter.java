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
package org.projectnessie.versioned.transfer;

import static org.projectnessie.versioned.transfer.ExportImportConstants.DEFAULT_BUFFER_SIZE;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.immutables.value.Value;
import org.projectnessie.versioned.StoreWorker;
import org.projectnessie.versioned.persist.adapter.DatabaseAdapter;
import org.projectnessie.versioned.store.DefaultStoreWorker;
import org.projectnessie.versioned.transfer.files.ExportFileSupplier;
import org.projectnessie.versioned.transfer.serialize.TransferTypes.ExportMeta;

@Value.Immutable
public abstract class NessieExporter {

  public static final String NAMED_REFS_PREFIX = "named-refs";
  public static final String COMMITS_PREFIX = "commits";

  public static Builder builder() {
    return ImmutableNessieExporter.builder();
  }

  @SuppressWarnings("UnusedReturnValue")
  public interface Builder {
    /** Mandatory, specify the {@code DatabaseAdapter} to use. */
    Builder databaseAdapter(DatabaseAdapter databaseAdapter);

    /** Optional, specify a custom {@link ObjectMapper}. */
    Builder objectMapper(ObjectMapper objectMapper);

    /** Optional, specify a custom {@link StoreWorker}. */
    Builder storeWorker(StoreWorker storeWorker);

    /**
     * Optional, specify a different buffer size than the default value of {@value
     * ExportImportConstants#DEFAULT_BUFFER_SIZE}.
     */
    Builder outputBufferSize(int outputBufferSize);

    /**
     * Maximum size of a file containing commits or named references. Default is to write everything
     * into a single file.
     */
    Builder maxFileSize(long maxFileSize);

    /**
     * The expected number of commits in the Nessie repository, default is {@value
     * ExportImportConstants#DEFAULT_EXPECTED_COMMIT_COUNT}.
     */
    Builder expectedCommitCount(int expectedCommitCount);

    Builder progressListener(ProgressListener progressListener);

    Builder exportFileSupplier(ExportFileSupplier exportFileSupplier);

    NessieExporter build();
  }

  abstract DatabaseAdapter databaseAdapter();

  @Value.Default
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Value.Default
  StoreWorker storeWorker() {
    return DefaultStoreWorker.instance();
  }

  @Value.Default
  int outputBufferSize() {
    return DEFAULT_BUFFER_SIZE;
  }

  @Value.Default
  long maxFileSize() {
    return Long.MAX_VALUE;
  }

  @Value.Default
  int expectedCommitCount() {
    return ExportImportConstants.DEFAULT_EXPECTED_COMMIT_COUNT;
  }

  abstract ExportFileSupplier exportFileSupplier();

  @Value.Default
  ProgressListener progressListener() {
    return (x, y) -> {};
  }

  public ExportMeta exportNessieRepository() throws IOException {
    ExportFileSupplier exportFiles = exportFileSupplier();

    exportFiles.preValidate();

    return new ExportDatabaseAdapter(exportFiles, this).exportRepo();
  }
}