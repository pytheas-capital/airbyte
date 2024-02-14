package io.airbyte.integrations.base.destination.typing_deduping.migrators;

import io.airbyte.integrations.base.destination.typing_deduping.StreamConfig;

/**
 * Migrations may do two things:
 * <ol>
 *   <li>Modify the raw table</li>
 *   <li>Trigger a soft reset</li>
 * </ol>
 *
 * The raw table modification should happen in {@link #migrateIfNecessary(Object, StreamConfig)}. However,
 * if multiple migrations want to trigger a soft reset, we should only trigger a single soft reset,
 * because soft resets are idempotent. There's no reason to trigger multiple soft resets in sequence,
 * and it would be a waste of warehouse compute to do so. Migrations MUST NOT directly run a soft reset
 * within {@link #migrateIfNecessary(Object, StreamConfig)}.
 * <p>
 * Migrations are encouraged to store something into the destination State blob. This allows us to make
 * fewer queries into customer data. However, migrations MUST NOT rely solely on the state blob to trigger
 * migrations. It's possible for a state to not be committed after a migration runs (e.g. a well-timed
 * OOMKill). Therefore, if the state blob indicates that a migration is necessary, migrations must still
 * confirm against the database that the migration is necessary.
 */
public interface Migration<State> {

  /**
   * Perform the migration if it's necessary. This typically looks like:
   * <pre>
   * // Check the state blob
   * if (requireMigration(state)) {
   *   // Check the database, in case a previous migration ran, but failed to update the state
   *   if (requireMigration(database)) {
   *     migrate();
   *   }
   * }
   * </pre>
   */
  MigrationResult<State> migrateIfNecessary(State state, StreamConfig stream) throws Exception;

  record MigrationResult<State>(State updatedState, boolean softReset) {}
}
