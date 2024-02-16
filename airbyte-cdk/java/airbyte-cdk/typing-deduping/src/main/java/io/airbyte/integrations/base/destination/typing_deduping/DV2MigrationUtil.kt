package io.airbyte.integrations.base.destination.typing_deduping

import com.google.common.collect.Streams
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CompletableFuture

class DV2MigrationUtil {
  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(DV2MigrationUtil::class.java)

    @JvmStatic
    fun <DialectTableDefinition> executeRawTableMigrations(
        sqlGenerator: SqlGenerator,
        destinationHandler: DestinationHandler<DialectTableDefinition>,
        parsedCatalog: ParsedCatalog,
        v1V2Migrator: DestinationV1V2Migrator<DialectTableDefinition>,
        v2TableMigrator: V2TableMigrator
    ) {
      prepareSchemas(sqlGenerator, destinationHandler, parsedCatalog)
      val prepareTablesTasks: MutableSet<CompletableFuture<Optional<Exception>>> = HashSet()
      for (stream in parsedCatalog.streams) {
        prepareTablesTasks.add(CompletableFuture.supplyAsync {
          try {
            // Migrate the Raw Tables if this is the first v2 sync after a v1 sync
            v1V2Migrator.migrateIfNecessary(sqlGenerator, destinationHandler, stream)
            v2TableMigrator.migrateIfNecessary(stream)
            return@supplyAsync Optional.empty<Exception>()
          } catch (e: Exception) {
            // Catch exception + extract as Optional because we're invoking this class from java
            // and it's just easier this way.
            // TODO just... do kotlin things instead
            LOGGER.error("Exception occurred while preparing tables for stream " + stream.id.originalName, e)
            return@supplyAsync Optional.of<Exception>(e)
          }
        })
      }
      CompletableFuture.allOf(*prepareTablesTasks.toTypedArray()).join()
      FutureUtils.reduceExceptions(prepareTablesTasks, "The following exceptions were thrown attempting to prepare tables:\n")
    }

    private fun <DialectTableDefinition> prepareSchemas(
        sqlGenerator: SqlGenerator,
        destinationHandler: DestinationHandler<DialectTableDefinition>,
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
