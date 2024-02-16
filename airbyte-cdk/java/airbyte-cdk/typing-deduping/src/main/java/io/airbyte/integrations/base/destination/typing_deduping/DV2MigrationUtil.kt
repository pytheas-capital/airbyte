package io.airbyte.integrations.base.destination.typing_deduping

import com.google.common.collect.Streams
import io.airbyte.integrations.base.destination.typing_deduping.migrators.Migration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors.toList

class DV2MigrationUtil {
  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(DV2MigrationUtil::class.java)

    @JvmStatic
    fun <DestinationState> executeRawTableMigrations(
        initialStateFutures: List<DestinationInitialState<DestinationState>>,
        sqlGenerator: SqlGenerator,
        destinationHandler: DestinationHandler<DestinationState>,
        parsedCatalog: ParsedCatalog,
        v1V2Migrator: DestinationV1V2Migrator,
        v2TableMigrator: V2TableMigrator,
        migrations: List<Migration<DestinationState>>
    ) {
      prepareSchemas(sqlGenerator, destinationHandler, parsedCatalog)
      val migrationFutures: List<Optional<Exception>> = initialStateFutures.stream()
          // TODO do this in parallel
          .map {initialState ->
              try {
                var currentState = initialState
                // Migrate the Raw Tables if this is the first v2 sync after a v1 sync
                var softReset = false

                // Migrate the Raw Tables if this is the first v2 sync after a v1 sync
                v1V2Migrator.migrateIfNecessary(sqlGenerator, destinationHandler, currentState.streamConfig)
                v2TableMigrator.migrateIfNecessary(currentState.streamConfig)

                for (migration in migrations) {
                  if (migration.requireMigration(currentState.destinationState)) {
                    val migrationResult: Migration.MigrationResult<DestinationState> = migration.migrateIfNecessary(initialState.streamConfig)
                    currentState = currentState.withDestinationState(migrationResult.updatedDestinationState)
                    softReset = softReset or migrationResult.softReset
                  }
                }
                return@map Optional.empty<Exception>()
              } catch (e: Exception) {
                // Catch exception + extract as Optional because we're invoking this class from java
                // and it's just easier this way.
                // TODO just... do kotlin things instead
                LOGGER.error("Exception occurred while preparing tables for stream " + initialState.streamConfig.id.originalName, e)
                return@map Optional.of<Exception>(e)
            }
          }.collect(toList())

//      CompletableFuture.allOf(*initialStateFutures.toTypedArray()).join()
//      FutureUtils.reduceExceptions(migrationFutures, "The following exceptions were thrown attempting to handle raw table migrations:\n")
      // TODO return the migration results
    }

    private fun <DestinationState> prepareSchemas(
        sqlGenerator: SqlGenerator,
        destinationHandler: DestinationHandler<DestinationState>,
        parsedCatalog: ParsedCatalog) {
      val rawSchema = parsedCatalog.streams.stream().map { stream: StreamConfig -> stream.id.rawNamespace }
      val finalSchema = parsedCatalog.streams.stream().map { stream: StreamConfig -> stream.id.finalNamespace }
      val createAllSchemasSql = Streams.concat<String>(rawSchema, finalSchema)
          .filter { obj: String? -> Objects.nonNull(obj) }
          .distinct()
          .map { schema: String? -> sqlGenerator.createSchema(schema) }
          .toList()
      destinationHandler.execute(Sql.concat(createAllSchemasSql))
    }
  }
}
