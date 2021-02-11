/**
 * Event Sourcing support.
 *
 * <p>Event sourced entities can be annotated with the {@link
 * com.akkaserverless.javasdk.eventsourced.EventSourcedEntity @EventSourcedEntity} annotation, and
 * supply command handlers using the {@link
 * com.akkaserverless.javasdk.eventsourced.CommandHandler @CommandHandler} annotation.
 *
 * <p>In addition, {@link com.akkaserverless.javasdk.eventsourced.EventHandler @EventHandler}
 * annotated methods should be defined to handle events, and {@link
 * com.akkaserverless.javasdk.eventsourced.Snapshot @Snapshot} and {@link
 * com.akkaserverless.javasdk.eventsourced.SnapshotHandler @SnapshotHandler} annotated methods
 * should be defined to produce and handle snapshots respectively.
 */
package com.akkaserverless.javasdk.eventsourced;
