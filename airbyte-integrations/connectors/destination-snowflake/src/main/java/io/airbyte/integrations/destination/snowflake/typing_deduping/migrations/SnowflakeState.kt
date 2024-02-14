package io.airbyte.integrations.destination.snowflake.typing_deduping.migrations

data class SnowflakeState(val extractedAtUpdatedToUtcTimezone: Boolean?)
