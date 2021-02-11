/**
 * Conflict-free Replicated Data Type support.
 *
 * <p>CRDT entities can be annotated with the {@link
 * com.akkaserverless.javasdk.crdt.CrdtEntity @CrdtEntity} annotation, and supply command handlers
 * using the {@link com.akkaserverless.javasdk.crdt.CommandHandler @CommandHandler} annotation.
 *
 * <p>The data stored by a CRDT entity can be stored in a subtype of {@link
 * com.akkaserverless.javasdk.crdt.Crdt}. These can be created using a {@link
 * com.akkaserverless.javasdk.crdt.CrdtFactory}, which is a super-interface of both the {@link
 * com.akkaserverless.javasdk.crdt.CrdtCreationContext}, available for injection constructors, and
 * of the {@link com.akkaserverless.javasdk.crdt.CommandContext}, available for injection in
 * {@code @CommandHandler} annotated methods.
 */
package com.akkaserverless.javasdk.crdt;
