/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.workflow

import kalix.scalasdk.Context

trait WorkflowContext extends Context {

  /**
   * The id of the workflow that this context is for.
   *
   * @return
   *   The workflow id.
   */
  def workflowId: String
}
