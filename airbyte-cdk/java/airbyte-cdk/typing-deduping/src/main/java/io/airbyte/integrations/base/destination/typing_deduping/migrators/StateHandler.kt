package io.airbyte.integrations.base.destination.typing_deduping.migrators

import io.airbyte.integrations.base.destination.typing_deduping.StreamId

interface StateHandler<DestinationState> {

    /**
     * Creates the airbyte_internal.state table if it doesn't exist. Fetches all records from that table.
     */
    fun getStates(): Map<StreamId, DestinationState>

    // not all destinations support `UPDATE state SET data = OBJECT_INSERT(data, 'new_key', true)`. (redshift, specifically, makes this really hard.)
    // so we instead assume that all destinations can do some sort of upsert with the full updated value.
    // this also lets us avoid doing diff-tracking on the state object.
    fun commitStates(states: Map<StreamId, DestinationState>)
}
