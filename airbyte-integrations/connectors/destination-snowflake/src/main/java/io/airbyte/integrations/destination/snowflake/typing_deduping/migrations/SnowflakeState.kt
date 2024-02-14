package io.airbyte.integrations.destination.snowflake.typing_deduping.migrations

// Fields are explicitly nullable.. This reflects the underlying storage medium, which is a JSON blob.
data class SnowflakeState(val extractedAtUpdatedToUtcTimezone: Boolean?)
