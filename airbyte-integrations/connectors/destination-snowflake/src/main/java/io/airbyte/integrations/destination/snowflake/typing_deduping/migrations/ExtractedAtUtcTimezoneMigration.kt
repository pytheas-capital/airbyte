package io.airbyte.integrations.destination.snowflake.typing_deduping.migrations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.airbyte.cdk.db.jdbc.JdbcDatabase
import io.airbyte.commons.json.Jsons
import io.airbyte.integrations.base.destination.typing_deduping.StreamConfig
import io.airbyte.integrations.base.destination.typing_deduping.migrators.Migration

class ExtractedAtUtcTimezoneMigration(
    private val database: JdbcDatabase
) : Migration<SnowflakeState> {

    override fun migrateIfNecessary(state: SnowflakeState, stream: StreamConfig): Migration.MigrationResult<SnowflakeState> {
        if (state.extractedAtUpdatedToUtcTimezone == true) {
            return Migration.MigrationResult.noop(state)
        }
        
        val rawRecordTimezone: JsonNode? = database.queryJsons(
                { connection ->
                    // TODO write an actual sql query
                    connection.prepareStatement("""
                    select
                      extract(timezone_hour from "_airbyte_extracted_at") as tzh,
                      extract(timezone_minute from "_airbyte_extracted_at") as tzm
                    FROM "airbyte_internal"."whatever"
                    LIMIT 1
                """.trimIndent())
                },
                { rs -> (Jsons.emptyObject() as ObjectNode)
                        .put("tzh", rs.getInt("tzh"))
                        .put("tzm", rs.getInt("tzm"))
                }
        ).first()
        if (rawRecordTimezone == null) {
            // There are no raw records. No migration necessary.
            return Migration.MigrationResult.noop(state)
        }
        if (rawRecordTimezone.get("tzh").intValue() == 0 && rawRecordTimezone.get("tzm").intValue() == 0) {
            // The raw records are already in UTC. No migration necessary.
            return Migration.MigrationResult.noop(state)
        }

        // TODO execute sql query to update the timezone of the extracted_at column

        return Migration.MigrationResult(state.copy(extractedAtUpdatedToUtcTimezone = true), true)
    }
}
