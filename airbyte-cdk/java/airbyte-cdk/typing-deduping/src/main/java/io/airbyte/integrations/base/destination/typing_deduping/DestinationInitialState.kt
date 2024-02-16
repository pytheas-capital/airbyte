/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */
package io.airbyte.integrations.base.destination.typing_deduping

import java.util.*

@JvmRecord
data class DestinationInitialState<DestinationState>(val streamConfig: StreamConfig,
                                                                      val isFinalTablePresent: Boolean,
                                                                      val initialRawTableState: InitialRawTableState,
                                                                      val isSchemaMismatch: Boolean,
                                                                      val isFinalTableEmpty: Boolean,
                                                                      val destinationState: DestinationState) {
  /**
   * Utility method for java interop. Kotlin files should just use the [copy] method directly.
   */
  fun withDestinationState(updatedState: DestinationState) = copy(destinationState = updatedState)
}
