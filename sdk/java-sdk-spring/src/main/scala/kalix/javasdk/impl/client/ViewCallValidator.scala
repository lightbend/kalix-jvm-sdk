package kalix.javasdk.impl.client

import java.lang.reflect.Method

import kalix.javasdk.action.Action
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.valueentity.ValueEntity
import kalix.javasdk.workflow.Workflow
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

object ViewCallValidator {

  def validate(method: Method): Unit = {
    val declaringClass = method.getDeclaringClass
    if (classOf[Action].isAssignableFrom(declaringClass)
      || classOf[ValueEntity[_]].isAssignableFrom(declaringClass)
      || classOf[EventSourcedEntity[_, _]].isAssignableFrom(declaringClass)
      || classOf[Workflow[_]].isAssignableFrom(declaringClass)) {
      throw new IllegalStateException(
        "Use dedicated builder for calling " + declaringClass.getSuperclass.getSimpleName
        + " component method " + declaringClass.getSimpleName + "::" + method.getName + ". This builder is meant for View component calls.")
    }

    val paramsWithMissingAnnotations = method.getParameterAnnotations.toSeq
      .zip(method.getParameters)
      .filter { case (annotations, _) =>
        annotations.isEmpty || !annotations.toSeq.exists(annotation => {
          val annType = annotation.annotationType()
          classOf[PathVariable].isAssignableFrom(annType) || classOf[RequestParam].isAssignableFrom(annType)
        })
      }
      .map(_._2)

    if (paramsWithMissingAnnotations.nonEmpty) {
      throw new IllegalStateException(
        s"When using ComponentClient each [${method.getName}] View query method parameter should be annotated with @PathVariable or @RequestParam annotations. "
        + s"Missing annotations for params with types: [${paramsWithMissingAnnotations.map(_.getType.getSimpleName).mkString(", ")}]")
         // it would be nicer to have param names, but when using `resolveMethodRef` all param names are gone, we have only "arg0", "arg1", etc.
    }

  }
}
