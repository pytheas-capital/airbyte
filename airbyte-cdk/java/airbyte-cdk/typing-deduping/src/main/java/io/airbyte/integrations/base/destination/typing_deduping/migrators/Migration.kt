package io.airbyte.integrations.base.destination.typing_deduping.migrators

import io.airbyte.integrations.base.destination.typing_deduping.StreamConfig

/**
 * Migrations may do two things:
 * 1. Modify the raw table
 * 2. Trigger a soft reset
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
interface Migration<State> {

    /**
     * Perform the migration if it's necessary. This typically looks like:
     * ```
     * // Check the state blob
     * if (requireMigration(state)) {
     *   // Check the database, in case a previous migration ran, but failed to update the state
     *   if (requireMigration(database)) {
     *     migrate();
     *   }
     * }
     * ```
     */
    fun migrateIfNecessary(state: State, stream: StreamConfig): MigrationResult<State>

    data class MigrationResult<State>(val updatedState: State, val softReset: Boolean)
}
