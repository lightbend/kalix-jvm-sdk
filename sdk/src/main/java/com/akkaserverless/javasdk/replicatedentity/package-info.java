/**
 * Replicated Entity (Conflict-free Replicated Data Type) support.
 *
 * <p>Replicated entities can be annotated with the {@link
 * com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity @CrdtEntity} annotation, and supply
 * command handlers using the {@link
 * com.akkaserverless.javasdk.replicatedentity.CommandHandler @CommandHandler} annotation.
 *
 * <p>The data stored by a replicated entity can be stored in a subtype of {@link
 * com.akkaserverless.javasdk.replicatedentity.ReplicatedData}. These can be created using a {@link
 * com.akkaserverless.javasdk.replicatedentity.ReplicatedDataFactory}, which is a super-interface of
 * both the {@link com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityCreationContext},
 * available for injection constructors, and of the {@link
 * com.akkaserverless.javasdk.replicatedentity.CommandContext}, available for injection in
 * {@code @CommandHandler} annotated methods.
 */
package com.akkaserverless.javasdk.replicatedentity;
