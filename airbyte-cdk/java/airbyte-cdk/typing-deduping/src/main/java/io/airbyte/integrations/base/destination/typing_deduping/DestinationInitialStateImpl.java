/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.base.destination.typing_deduping;

public record DestinationInitialStateImpl<DestinationState>(StreamConfig streamConfig,
                                          boolean isFinalTablePresent,
                                          InitialRawTableState initialRawTableState,
                                          boolean isSchemaMismatch,
                                          boolean isFinalTableEmpty,
                                          DestinationState destinationState)
    implements DestinationInitialState<DestinationState> {

}
