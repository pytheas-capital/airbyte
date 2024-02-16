/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.base.destination.typing_deduping;

import java.util.List;
import java.util.Map;

public interface DestinationHandler<DestinationState> {

  void execute(final Sql sql) throws Exception;

  List<DestinationInitialState<DestinationState>> gatherInitialState(List<StreamConfig> streamConfigs) throws Exception;

  void commitDestinationStates(final Map<StreamId, DestinationState> destinationStates) throws Exception;


}
