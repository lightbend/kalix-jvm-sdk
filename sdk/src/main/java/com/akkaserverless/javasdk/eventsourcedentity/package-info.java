/**
 * Event Sourcing support.
 *
 * <p>Event sourced entities can be annotated with the {@link
 * com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity @EventSourcedEntity} annotation,
 * and supply command handlers using the {@link
 * com.akkaserverless.javasdk.eventsourcedentity.CommandHandler @CommandHandler} annotation.
 *
 * <p>In addition, {@link com.akkaserverless.javasdk.eventsourcedentity.EventHandler @EventHandler}
 * annotated methods should be defined to handle events, and {@link
 * com.akkaserverless.javasdk.eventsourcedentity.Snapshot @Snapshot} and {@link
 * com.akkaserverless.javasdk.eventsourcedentity.SnapshotHandler @SnapshotHandler} annotated methods
 * should be defined to produce and handle snapshots respectively.
 */
package com.akkaserverless.javasdk.eventsourcedentity;
