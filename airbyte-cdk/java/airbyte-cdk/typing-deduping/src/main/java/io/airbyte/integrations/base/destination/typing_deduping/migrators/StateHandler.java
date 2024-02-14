package io.airbyte.integrations.base.destination.typing_deduping.migrators;

import io.airbyte.integrations.base.destination.typing_deduping.StreamId;
import java.util.Map;

public interface StateHandler<State> {

  /**
   * Creates the airbyte_internal.state table if it doesn't exist. Fetches all records from that table.
   */
  Map<StreamId, State> getStates() throws Exception;

  // not all destinations support `UPDATE state SET data = OBJECT_INSERT(data, 'new_key', true)`. (redshift, specifically, makes this really hard.)
  // so we instead assume that all destinations can do some sort of upsert with the full updated value.
  // this also lets us avoid doing diff-tracking on the state object.
  void commitStates(Map<StreamId, State> states) throws Exception;

}
