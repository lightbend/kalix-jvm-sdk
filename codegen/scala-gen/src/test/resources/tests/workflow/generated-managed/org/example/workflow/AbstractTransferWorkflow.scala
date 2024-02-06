package org.example.workflow

import com.google.protobuf.empty.Empty
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.ProtoWorkflow
import org.example.Components
import org.example.ComponentsImpl
import org.example.workflow
import org.example.workflow.domain.TransferState

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractTransferWorkflow extends ProtoWorkflow[TransferState] {

  def components: Components =
    new ComponentsImpl(commandContext())

  def start(currentState: TransferState, transfer: Transfer): AbstractWorkflow.Effect[Empty]

  def getState(currentState: TransferState, empty: Empty): AbstractWorkflow.Effect[Transfer]

}

