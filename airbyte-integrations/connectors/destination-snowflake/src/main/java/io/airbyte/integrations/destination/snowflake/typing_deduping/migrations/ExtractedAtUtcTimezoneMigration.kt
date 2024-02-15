package io.airbyte.integrations.destination.snowflake.typing_deduping.migrations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.airbyte.cdk.db.jdbc.JdbcDatabase
import io.airbyte.cdk.integrations.base.JavaBaseConstants
import io.airbyte.commons.json.Jsons
import io.airbyte.integrations.base.destination.typing_deduping.StreamConfig
import io.airbyte.integrations.base.destination.typing_deduping.migrators.Migration
import org.jooq.conf.ParamType
import org.jooq.impl.DSL.*

class ExtractedAtUtcTimezoneMigration(private val database: JdbcDatabase) : Migration<SnowflakeState> {

  override fun migrateIfNecessary(state: SnowflakeState, stream: StreamConfig): Migration.MigrationResult<SnowflakeState> {
    if (state.extractedAtUpdatedToUtcTimezone == true) {
      return Migration.MigrationResult.noop(state)
    }

    val rawRecordTimezone: JsonNode? = database.queryJsons(
        { connection ->
          connection.prepareStatement(
              select(
                  field(sql("extract(timezone_hour from \"_airbyte_extracted_at\")")).`as`("tzh"),
                  field(sql("extract(timezone_minute from \"_airbyte_extracted_at\")")).`as`("tzm")
              ).from(table(quotedName(stream.id().rawNamespace, stream.id().rawName)))
                  .limit(1)
                  .getSQL(ParamType.INLINED))
        },
        { rs ->
          (Jsons.emptyObject() as ObjectNode)
              .put("tzh", rs.getInt("tzh"))
              .put("tzm", rs.getInt("tzm"))
        }
    ).first()
    if (rawRecordTimezone == null
        || (rawRecordTimezone.get("tzh").intValue() == 0 && rawRecordTimezone.get("tzm").intValue() == 0)) {
      // There are no raw records, or the raw records are already in UTC. No migration necessary. Update the state.
      return Migration.MigrationResult(state.copy(extractedAtUpdatedToUtcTimezone = true), false)
    }

    database.execute(
        update(table(quotedName(stream.id().rawNamespace, stream.id().rawName)))
            .set(
                field(quotedName(JavaBaseConstants.COLUMN_NAME_AB_EXTRACTED_AT)),
                // this is the easiest way to forcibly set the offset on a timestamptz.
                // We convert to timestamp_ntz to remove the offset,
                // then convert to string and append a 'Z' offset,
                // then convert back to timestamp_tz.
                // We _could_ go through convert_timezone and manually add a negative offset number of hours
                // but that's a lot more work for no real benefit.
                sql("""
                  cast(cast(cast("_airbyte_extracted_at" as timestampntz) as string) || 'Z' as timestamptz)
                  """.trimIndent())
            ).getSQL(ParamType.INLINED)
    )

    // We've executed the migration. Update the state and trigger a soft reset.
    return Migration.MigrationResult(state.copy(extractedAtUpdatedToUtcTimezone = true), true)
  }
}
