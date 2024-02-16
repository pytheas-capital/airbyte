/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.base.destination.typing_deduping;

import static io.airbyte.cdk.integrations.base.IntegrationRunner.TYPE_AND_DEDUPE_THREAD_NAME;
import static io.airbyte.integrations.base.destination.typing_deduping.FutureUtils.countOfTypingDedupingThreads;

import io.airbyte.cdk.integrations.destination.StreamSyncSummary;
import io.airbyte.protocol.models.v0.StreamDescriptor;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

/**
 * This is a NoOp implementation which skips and Typing and Deduping operations and does not emit
 * the final tables. However, this implementation still performs V1->V2 migrations and V2
 * json->string migrations in the raw tables.
 */
@Slf4j
public class NoOpTyperDeduperWithV1V2Migrations implements TyperDeduper {

  private final DestinationV1V2Migrator v1V2Migrator;
  private final V2TableMigrator v2TableMigrator;
  private final ExecutorService executorService;
  private final ParsedCatalog parsedCatalog;
  private final SqlGenerator sqlGenerator;
  private final DestinationHandler<?> destinationHandler;

  public NoOpTyperDeduperWithV1V2Migrations(final SqlGenerator sqlGenerator,
                                            final DestinationHandler<?> destinationHandler,
                                            final ParsedCatalog parsedCatalog,
                                            final DestinationV1V2Migrator v1V2Migrator,
                                            final V2TableMigrator v2TableMigrator,
                                            final int defaultThreadCount) {
    this.sqlGenerator = sqlGenerator;
    this.destinationHandler = destinationHandler;
    this.parsedCatalog = parsedCatalog;
    this.v1V2Migrator = v1V2Migrator;
    this.v2TableMigrator = v2TableMigrator;
    this.executorService = Executors.newFixedThreadPool(countOfTypingDedupingThreads(defaultThreadCount),
        new BasicThreadFactory.Builder().namingPattern(TYPE_AND_DEDUPE_THREAD_NAME).build());
  }

  @Override
  public void executeRawTableMigrations() {
    // TODO
    DV2MigrationUtil.executeRawTableMigrations(null, sqlGenerator, destinationHandler, parsedCatalog, v1V2Migrator, v2TableMigrator, Collections.emptyList());
  }

  @Override
  public void prepareFinalTables() {
    log.info("Skipping prepareFinalTables");
  }

  @Override
  public void typeAndDedupe(final String originalNamespace, final String originalName, final boolean mustRun) {
    log.info("Skipping TypeAndDedupe");
  }

  @Override
  public Lock getRawTableInsertLock(final String originalNamespace, final String originalName) {
    return new Lock() {

      @Override
      public void lock() {

      }

      @Override
      public void lockInterruptibly() {

      }

      @Override
      public boolean tryLock() {
        // To mimic NoOp behavior always return true that lock is acquired
        return true;
      }

      @Override
      public boolean tryLock(final long time, final TimeUnit unit) {
        // To mimic NoOp behavior always return true that lock is acquired
        return true;
      }

      @Override
      public void unlock() {

      }

      @Override
      public Condition newCondition() {
        // Always throw exception to avoid callers from using this path
        throw new UnsupportedOperationException("This lock implementation does not support retrieving a Condition");
      }

    };
  }

  @Override
  public void typeAndDedupe(final Map<StreamDescriptor, StreamSyncSummary> streamSyncSummaries) {
    log.info("Skipping TypeAndDedupe final");
  }

  @Override
  public void commitFinalTables() {
    log.info("Skipping commitFinalTables final");
  }

  @Override
  public void cleanup() {
    log.info("Cleaning Up type-and-dedupe thread pool");
    this.executorService.shutdown();
  }

}
