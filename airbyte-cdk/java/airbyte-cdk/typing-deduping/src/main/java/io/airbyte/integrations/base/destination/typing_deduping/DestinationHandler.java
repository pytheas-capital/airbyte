/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.base.destination.typing_deduping;

import java.util.List;

public interface DestinationHandler<DestinationState> {

  void execute(final Sql sql) throws Exception;

  List<DestinationInitialState<DestinationState>> gatherInitialState(List<StreamConfig> streamConfigs) throws Exception;

}
