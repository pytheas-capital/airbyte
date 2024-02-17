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
interface Migration<DestinationState> {

    /**
     * Check if a migration is necessary according to the state blob. Implementations SHOULD NOT
     * perform IO in this method.
     */
    fun requireMigration(state: DestinationState): Boolean

    /**
     * Perform the migration if it's necessary. This method will only be called if a previous call to
     * [requireMigration] returned true. Implementations of this method MUST check against the database
     * to confirm the the migration is still necessary, in case a previous migration ran, but failed
     * to update the state.
     */
    fun migrateIfNecessary(stream: StreamConfig): MigrationResult<DestinationState>

    data class MigrationResult<DestinationState>(val updatedDestinationState: DestinationState, val softReset: Boolean) {
        companion object {
            /**
             * If a migration detects no need to migrate, it should return this.
             */
            fun <State> noop(state: State): MigrationResult<State> {
                return MigrationResult(state, false)
            }
        }
    }
}
