package io.airbyte.integrations.destination.snowflake.typing_deduping.migrations

import io.airbyte.integrations.base.destination.typing_deduping.StreamConfig
import io.airbyte.integrations.base.destination.typing_deduping.migrators.Migration

class ExtractedAtUtcTimezoneMigration : Migration<SnowflakeState> {
    override fun migrateIfNecessary(state: SnowflakeState, stream: StreamConfig): Migration.MigrationResult<SnowflakeState> {
        return Migration.MigrationResult(state!!.copy(extractedAtUpdatedToUtcTimezone = true), true);
    }
}
