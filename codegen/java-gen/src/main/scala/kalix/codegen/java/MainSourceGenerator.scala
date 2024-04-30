/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.codegen.java

import kalix.codegen.ModelBuilder
import kalix.codegen.ModelBuilder.StatefulComponent
import kalix.codegen.ModelBuilder.Service
import kalix.codegen._

/**
 * Responsible for generating Main and KalixFactory Java source from an entity model
 */
object MainSourceGenerator {
  import kalix.codegen.SourceGeneratorUtils._
  import JavaGeneratorUtils._

  def generate(model: ModelBuilder.Model, mainClassPackage: PackageNaming, mainClassName: String): GeneratedFiles =
    GeneratedFiles.Empty
      .addManaged(File.java(mainClassPackage, "KalixFactory", kalixFactorySource(mainClassPackage.javaPackage, model)))
      .addUnmanaged(
        File.java(
          mainClassPackage,
          mainClassName,
          mainSource(mainClassPackage.javaPackage, mainClassName, model.statefulComponents, model.services)))

  private[codegen] def mainSource(
      mainClassPackageName: String,
      mainClassName: String,
      entities: Map[String, StatefulComponent],
      services: Map[String, Service]): String = {

    val entityImports = entities.values.collect {
      case entity: ModelBuilder.EventSourcedEntity   => entity.impl
      case entity: ModelBuilder.ValueEntity          => entity.impl
      case entity: ModelBuilder.ReplicatedEntity     => entity.impl
      case component: ModelBuilder.WorkflowComponent => component.impl
    }.toSeq

    val serviceImports = services.values.collect {
      case service: ModelBuilder.ActionService => service.impl
      case view: ModelBuilder.ViewService      => view.impl
    }.toSeq

    implicit val imports: Imports =
      generateImports(
        entityImports ++ serviceImports,
        mainClassPackageName,
        Seq("kalix.javasdk.Kalix", "org.slf4j.Logger", "org.slf4j.LoggerFactory"))

    val entityRegistrationParameters = entities.values.toList
      .sortBy(_.messageType.name)
      .collect {
        case entity: ModelBuilder.EventSourcedEntity   => s"${typeName(entity.impl)}::new"
        case entity: ModelBuilder.ValueEntity          => s"${typeName(entity.impl)}::new"
        case entity: ModelBuilder.ReplicatedEntity     => s"${typeName(entity.impl)}::new"
        case component: ModelBuilder.WorkflowComponent => s"${typeName(component.impl)}::new"
      }

    val serviceRegistrationParameters = services.values.toList
      .sortBy(_.messageType.name)
      .collect {
        case service: ModelBuilder.ActionService => s"${typeName(service.impl)}::new"
        case view: ModelBuilder.ViewService      => s"${typeName(view.impl)}::new"
      }

    val registrationParameters = entityRegistrationParameters ::: serviceRegistrationParameters
    s"""package $mainClassPackageName;
        |
        |${writeImports(imports)}
        |
        |$unmanagedComment
        |
        |public final class ${mainClassName} {
        |
        |  private static final Logger LOG = LoggerFactory.getLogger(${mainClassName}.class);
        |
        |  public static Kalix createKalix() {
        |    // The KalixFactory automatically registers any generated Actions, Views or Entities,
        |    // and is kept up-to-date with any changes in your protobuf definitions.
        |    // If you prefer, you may remove this and manually register these components in a
        |    // `new Kalix()` instance.
        |    return KalixFactory.withComponents(
        |      ${registrationParameters.mkString(",\n      ")});
        |  }
        |
        |  public static void main(String[] args) throws Exception {
        |    LOG.info("starting the Kalix service");
        |    createKalix().start();
        |  }
        |}
        |""".stripMargin

  }

  private[codegen] def kalixFactorySource(mainClassPackageName: String, model: ModelBuilder.Model): String = {
    val entityImports = model.statefulComponents.values.flatMap { ety =>
      Seq(ety.impl, ety.provider)
    }

    val serviceImports = model.services.values.flatMap { serv =>
      serv.messageType.descriptorObject ++
      (serv match {
        case actionServ: ModelBuilder.ActionService =>
          List(actionServ.impl, actionServ.provider)
        case view: ModelBuilder.ViewService =>
          List(view.impl, view.provider)
        case _ => Nil
      })
    }

    val entityContextImports = model.statefulComponents.values.collect {
      case _: ModelBuilder.EventSourcedEntity =>
        List("kalix.javasdk.eventsourcedentity.EventSourcedEntityContext", "java.util.function.Function")
      case _: ModelBuilder.ValueEntity =>
        List("kalix.javasdk.valueentity.ValueEntityContext", "java.util.function.Function")
      case _: ModelBuilder.ReplicatedEntity =>
        List("kalix.javasdk.replicatedentity.ReplicatedEntityContext", "java.util.function.Function")
      case _: ModelBuilder.WorkflowComponent =>
        List("kalix.javasdk.workflow.WorkflowContext", "java.util.function.Function")
    }.flatten

    val serviceContextImports = model.services.values.collect {
      case _: ModelBuilder.ActionService =>
        List("kalix.javasdk.action.ActionCreationContext", "java.util.function.Function")
      case _: ModelBuilder.ViewService =>
        List("kalix.javasdk.view.ViewCreationContext", "java.util.function.Function")
    }.flatten
    val contextImports = (entityContextImports ++ serviceContextImports).toSeq

    implicit val imports =
      generateImports(entityImports ++ serviceImports, mainClassPackageName, "kalix.javasdk.Kalix" +: contextImports)

    def creator(messageType: ProtoMessageType): String = {
      if (imports.clashingNames.contains(messageType.name)) s"create${dotsToCamelCase(typeName(messageType))}"
      else s"create${messageType.name}"
    }

    val registrations = model.services.values
      .flatMap {
        case service: ModelBuilder.EntityService =>
          model.statefulComponents.get(service.componentFullName).toSeq.map {
            case entity: ModelBuilder.EventSourcedEntity =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
            case entity: ModelBuilder.ValueEntity =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
            case entity: ModelBuilder.ReplicatedEntity =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
            case entity: ModelBuilder.WorkflowComponent =>
              s".register(${typeName(entity.provider)}.of(${creator(entity.impl)}))"
          }

        case service: ModelBuilder.ViewService =>
          List(s".register(${typeName(service.provider)}.of(${creator(service.impl)}))")

        case service: ModelBuilder.ActionService =>
          List(s".register(${typeName(service.provider)}.of(${creator(service.impl)}))")

      }
      .toList
      .sorted

    val entityCreators =
      model.statefulComponents.values.toList
        .sortBy(_.messageType.name)
        .collect {
          case entity: ModelBuilder.EventSourcedEntity =>
            s"Function<EventSourcedEntityContext, ${typeName(entity.impl)}> ${creator(entity.impl)}"
          case entity: ModelBuilder.ValueEntity =>
            s"Function<ValueEntityContext, ${typeName(entity.impl)}> ${creator(entity.impl)}"
          case entity: ModelBuilder.ReplicatedEntity =>
            s"Function<ReplicatedEntityContext, ${typeName(entity.impl)}> ${creator(entity.impl)}"
          case component: ModelBuilder.WorkflowComponent =>
            s"Function<WorkflowContext, ${typeName(component.impl)}> ${creator(component.impl)}"
        }

    val serviceCreators = model.services.values.toList
      .sortBy(_.messageType.name)
      .collect {
        case service: ModelBuilder.ActionService =>
          s"Function<ActionCreationContext, ${typeName(service.impl)}> ${creator(service.impl)}"
        case view: ModelBuilder.ViewService =>
          s"Function<ViewCreationContext, ${typeName(view.impl)}> ${creator(view.impl)}"
      }

    val creatorParameters = entityCreators ::: serviceCreators

    s"""package $mainClassPackageName;
        |
        |${writeImports(imports)}
        |
        |$managedComment
        |
        |public final class KalixFactory {
        |
        |  public static Kalix withComponents(
        |      ${creatorParameters.mkString(",\n      ")}) {
        |    Kalix kalix = new Kalix();
        |    return kalix
        |      ${Format.indent(registrations, 6)};
        |  }
        |}
        |""".stripMargin
  }
}
