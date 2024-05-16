/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import java.io.ByteArrayOutputStream
import java.util.Locale
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag
import scala.util.Try
import com.google.common.base.CaseFormat
import com.google.protobuf.ByteString
import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import com.google.protobuf.Descriptors
import com.google.protobuf.Parser
import com.google.protobuf.UnsafeByteOperations
import com.google.protobuf.WireFormat
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.AnySupport.Prefer.Java
import kalix.javasdk.impl.AnySupport.Prefer.Scala
import kalix.javasdk.impl.ErrorHandling.BadRequestException
import org.slf4j.LoggerFactory
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion
import scalapb.options.Scalapb

import scala.collection.compat.immutable.ArraySeq

object AnySupport {

  private final val KalixPrimitiveFieldNumber = 1
  final val KalixPrimitive = "type.kalix.io/"
  final val DefaultTypeUrlPrefix = "type.googleapis.com"
  final val ProtobufEmptyTypeUrl = "type.googleapis.com/google.protobuf.Empty"

  private val log = LoggerFactory.getLogger(classOf[AnySupport])

  sealed abstract class Primitive[T: ClassTag] {
    val name: String = fieldType.name().toLowerCase(Locale.ROOT)
    val fullName: String = KalixPrimitive + name
    final val clazz: Class[_] = implicitly[ClassTag[T]].runtimeClass
    def write(stream: CodedOutputStream, t: T): Unit
    def read(stream: CodedInputStream): T
    def fieldType: WireFormat.FieldType
    def defaultValue: T
    val tag: Int = (KalixPrimitiveFieldNumber << 3) | fieldType.getWireType
  }

  private object StringPrimitive extends Primitive[String] {
    override def fieldType = WireFormat.FieldType.STRING
    override def defaultValue = ""
    override def write(stream: CodedOutputStream, t: String): Unit = stream.writeString(KalixPrimitiveFieldNumber, t)
    override def read(stream: CodedInputStream): String = stream.readString()
  }

  object BytesPrimitive extends Primitive[ByteString] {
    override def fieldType = WireFormat.FieldType.BYTES
    override def defaultValue = ByteString.EMPTY
    override def write(stream: CodedOutputStream, t: ByteString): Unit =
      stream.writeBytes(KalixPrimitiveFieldNumber, t)
    override def read(stream: CodedInputStream): ByteString = stream.readBytes()
  }

  private final val Primitives = Seq(
    StringPrimitive,
    BytesPrimitive,
    new Primitive[Integer] {
      override def fieldType = WireFormat.FieldType.INT32
      override def defaultValue = 0
      override def write(stream: CodedOutputStream, t: Integer) =
        stream.writeInt32(KalixPrimitiveFieldNumber, t)
      override def read(stream: CodedInputStream) = stream.readInt32()
    },
    new Primitive[java.lang.Long] {
      override def fieldType = WireFormat.FieldType.INT64
      override def defaultValue = 0L
      override def write(stream: CodedOutputStream, t: java.lang.Long) =
        stream.writeInt64(KalixPrimitiveFieldNumber, t)
      override def read(stream: CodedInputStream) = stream.readInt64()
    },
    new Primitive[java.lang.Float] {
      override def fieldType = WireFormat.FieldType.FLOAT
      override def defaultValue = 0f
      override def write(stream: CodedOutputStream, t: java.lang.Float) =
        stream.writeFloat(KalixPrimitiveFieldNumber, t)
      override def read(stream: CodedInputStream) = stream.readFloat()
    },
    new Primitive[java.lang.Double] {
      override def fieldType = WireFormat.FieldType.DOUBLE
      override def defaultValue = 0d
      override def write(stream: CodedOutputStream, t: java.lang.Double) =
        stream.writeDouble(KalixPrimitiveFieldNumber, t)
      override def read(stream: CodedInputStream) = stream.readDouble()
    },
    new Primitive[java.lang.Boolean] {
      override def fieldType = WireFormat.FieldType.BOOL
      override def defaultValue = false
      override def write(stream: CodedOutputStream, t: java.lang.Boolean) =
        stream.writeBool(KalixPrimitiveFieldNumber, t)
      override def read(stream: CodedInputStream) = stream.readBool()
    })

  private final val ClassToPrimitives = Primitives
    .map(p => p.clazz -> p)
    .asInstanceOf[Seq[(Any, Primitive[Any])]]
    .toMap
  private final val NameToPrimitives = Primitives
    .map(p => p.fullName -> p)
    .asInstanceOf[Seq[(String, Primitive[Any])]]
    .toMap

  /**
   * INTERNAL API
   */
  private[kalix] def encodePrimitiveBytes(bytes: ByteString): ByteString =
    primitiveToBytes(BytesPrimitive, bytes)

  /**
   * INTERNAL API
   */
  private[kalix] def decodePrimitiveBytes(bytes: ByteString): ByteString =
    bytesToPrimitive(BytesPrimitive, bytes)

  private def primitiveToBytes[T](primitive: Primitive[T], value: T): ByteString =
    if (value != primitive.defaultValue) {
      val baos = new ByteArrayOutputStream()
      val stream = CodedOutputStream.newInstance(baos)
      primitive.write(stream, value)
      stream.flush()
      UnsafeByteOperations.unsafeWrap(baos.toByteArray)
    } else ByteString.EMPTY

  private def bytesToPrimitive[T](primitive: Primitive[T], bytes: ByteString) = {
    val stream = bytes.newCodedInput()
    if (LazyList
        .continually(stream.readTag())
        .takeWhile(_ != 0)
        .exists { tag =>
          if (primitive.tag != tag) {
            stream.skipField(tag)
            false
          } else true
        }) {
      primitive.read(stream)
    } else primitive.defaultValue
  }

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this says which
   * should be preferred.
   */
  sealed trait Prefer
  object Prefer {
    case object Java extends Prefer
    case object Scala extends Prefer
  }

  final val PREFER_JAVA = Java
  final val PREFER_SCALA = Scala

  def flattenDescriptors(descriptors: Seq[Descriptors.FileDescriptor]): Map[String, Descriptors.FileDescriptor] =
    flattenDescriptors(Map.empty, descriptors)

  private def flattenDescriptors(
      seenSoFar: Map[String, Descriptors.FileDescriptor],
      descriptors: Seq[Descriptors.FileDescriptor]): Map[String, Descriptors.FileDescriptor] =
    descriptors.foldLeft(seenSoFar) { case (results, descriptor) =>
      val descriptorName = descriptor.getName
      if (results.contains(descriptorName)) results
      else {
        val withDesc = results.updated(descriptorName, descriptor)
        flattenDescriptors(
          withDesc,
          descriptor.getDependencies.asScala.toSeq ++ descriptor.getPublicDependencies.asScala)
      }
    }

  def extractBytes(bytes: ByteString): ByteString = bytesToPrimitive(BytesPrimitive, bytes)
}

class AnySupport(
    descriptors: Array[Descriptors.FileDescriptor],
    classLoader: ClassLoader,
    typeUrlPrefix: String = AnySupport.DefaultTypeUrlPrefix,
    prefer: AnySupport.Prefer = AnySupport.Prefer.Java)
    extends MessageCodec {

  import AnySupport._
  private val allDescriptors = flattenDescriptors(ArraySeq.unsafeWrapArray(descriptors))

  private val allTypes: Map[String, Descriptors.Descriptor] = (for {
    descriptor <- allDescriptors.values
    messageType <- descriptor.getMessageTypes.asScala
  } yield messageType.getFullName -> messageType).toMap

  private val reflectionCache = TrieMap.empty[String, Try[ResolvedType[Any]]]

  private def strippedFileName(fileName: String) =
    fileName.split(Array('/', '\\')).last.stripSuffix(".proto")

  private def tryResolveJavaPbType(typeDescriptor: Descriptors.Descriptor) = {
    val fileDescriptor = typeDescriptor.getFile
    val options = fileDescriptor.getOptions
    // Firstly, determine the java package
    val packageName =
      if (options.hasJavaPackage) options.getJavaPackage + "."
      else if (fileDescriptor.getPackage.nonEmpty) fileDescriptor.getPackage + "."
      else ""

    val outerClassName =
      if (options.hasJavaMultipleFiles && options.getJavaMultipleFiles) ""
      else if (options.hasJavaOuterClassname) options.getJavaOuterClassname + "$"
      else if (fileDescriptor.getName.nonEmpty) {
        val name = strippedFileName(fileDescriptor.getName)
        if (name.contains('_') || name.contains('-') || !name(0).isUpper) {
          // transform snake and kebab case into camel case
          CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name.replace('-', '_')) + "$"
        } else {
          // keep name as is to keep already camel cased file name
          strippedFileName(fileDescriptor.getName) + "$"
        }
      } else ""

    val className = packageName + outerClassName + typeDescriptor.getName
    try {
      log.debug("Attempting to load class {}", className)

      val clazz = classLoader.loadClass(className)
      val parser = clazz.getMethod("parser").invoke(null).asInstanceOf[Parser[com.google.protobuf.Message]]
      Some(new JavaPbResolvedType(parser))

    } catch {
      case cnfe: ClassNotFoundException =>
        log.debug("Failed to load class [{}] because: {}", className, cnfe.getMessage)
        None
      case nsme: NoSuchElementException =>
        throw SerializationException(
          s"Found com.google.protobuf.Message class $className to deserialize protobuf ${typeDescriptor.getFullName} but it didn't have a static parser() method on it.",
          nsme)
      case iae @ (_: IllegalAccessException | _: IllegalArgumentException) =>
        throw SerializationException(s"Could not invoke $className.parser()", iae)
      case cce: ClassCastException =>
        throw SerializationException(s"$className.parser() did not return a ${classOf[Parser[_]]}", cce)
    }
  }

  private def tryResolveScalaPbType(typeDescriptor: Descriptors.Descriptor) = {
    // todo - attempt to load the package.proto file for this package to get default options from there
    val fileDescriptor = typeDescriptor.getFile
    val options = fileDescriptor.getOptions
    val scalaOptions: Scalapb.ScalaPbOptions = if (options.hasExtension(Scalapb.options)) {
      options.getExtension(Scalapb.options)
    } else Scalapb.ScalaPbOptions.getDefaultInstance

    // Firstly, determine the java package
    val packageName =
      if (scalaOptions.hasPackageName) scalaOptions.getPackageName + "."
      else if (options.hasJavaPackage) options.getJavaPackage + "."
      else if (fileDescriptor.getPackage.nonEmpty) fileDescriptor.getPackage + "."
      else ""

    // flat package could be overridden on the command line, so attempt to load both possibilities if it's not
    // explicitly setclassLoader.loadClass(className)
    val possibleBaseNames =
      if (scalaOptions.hasFlatPackage) {
        if (scalaOptions.getFlatPackage) Seq("")
        else Seq(fileDescriptor.getName.stripSuffix(".proto") + ".")
      } else if (fileDescriptor.getName.nonEmpty) Seq("", strippedFileName(fileDescriptor.getName) + ".")
      else Seq("")

    possibleBaseNames.collectFirst(Function.unlift { baseName =>
      val className = packageName + baseName + typeDescriptor.getName
      val companionName = className + "$"
      try {
        log.debug("Attempting to load scalapb.GeneratedMessageCompanion object {}", className)
        val clazz = classLoader.loadClass(className)
        val companion = classLoader.loadClass(companionName)
        if (classOf[GeneratedMessageCompanion[_]].isAssignableFrom(companion) &&
          classOf[scalapb.GeneratedMessage].isAssignableFrom(clazz)) {
          val companionObject = companion.getField("MODULE$").get(null).asInstanceOf[GeneratedMessageCompanion[_]]
          Some(new ScalaPbResolvedType(companionObject))
        } else {
          None
        }
      } catch {
        case cnfe: ClassNotFoundException =>
          log.debug("Failed to load class [{}] because: {}", className, cnfe.getMessage)
          None
      }
    })
  }

  def resolveTypeDescriptor(typeDescriptor: Descriptors.Descriptor): ResolvedType[Any] =
    reflectionCache
      .getOrElseUpdate(
        typeDescriptor.getFullName,
        Try {
          val maybeResolvedType =
            if (prefer == Prefer.Java) {
              tryResolveJavaPbType(typeDescriptor).orElse(tryResolveScalaPbType(typeDescriptor))
            } else {
              tryResolveScalaPbType(typeDescriptor).orElse(tryResolveJavaPbType(typeDescriptor))
            }

          maybeResolvedType match {
            case Some(resolvedType) => resolvedType.asInstanceOf[ResolvedType[Any]]
            case None =>
              throw SerializationException("Could not determine serializer for type " + typeDescriptor.getFullName)
          }
        })
      .get

  def resolveServiceDescriptor(
      serviceDescriptor: Descriptors.ServiceDescriptor): Map[String, ResolvedServiceMethod[_, _]] =
    serviceDescriptor.getMethods.asScala.map { method =>
      method.getName -> ResolvedServiceMethod(
        method,
        resolveTypeDescriptor(method.getInputType),
        resolveTypeDescriptor(method.getOutputType))
    }.toMap

  private def resolveTypeUrl(typeName: String): Option[ResolvedType[_]] =
    allTypes.get(typeName).map(resolveTypeDescriptor)

  def encodeJava(value: Any): JavaPbAny =
    value match {
      case javaPbAny: JavaPbAny   => javaPbAny
      case scalaPbAny: ScalaPbAny => ScalaPbAny.toJavaProto(scalaPbAny)
      case _                      => ScalaPbAny.toJavaProto(encodeScala(value))
    }

  def encodeScala(value: Any): ScalaPbAny =
    value match {
      case javaPbAny: JavaPbAny   => ScalaPbAny.fromJavaProto(javaPbAny)
      case scalaPbAny: ScalaPbAny => scalaPbAny

      // these are all generated message so needs to go before GeneratedMessage,
      // but we encode them inside Any just like regular message, we just need to get the type_url right
      case javaBytes: com.google.protobuf.BytesValue =>
        ScalaPbAny.fromJavaProto(JavaPbAny.pack(javaBytes))

      case scalaBytes: com.google.protobuf.wrappers.BytesValue =>
        ScalaPbAny.pack(scalaBytes)

      case javaText: com.google.protobuf.StringValue =>
        ScalaPbAny.fromJavaProto(JavaPbAny.pack(javaText))

      case scalaText: com.google.protobuf.wrappers.StringValue =>
        ScalaPbAny.pack(scalaText)

      case javaProtoMessage: com.google.protobuf.Message =>
        ScalaPbAny(
          typeUrlPrefix + "/" + javaProtoMessage.getDescriptorForType.getFullName,
          javaProtoMessage.toByteString)

      case scalaPbMessage: GeneratedMessage =>
        ScalaPbAny(typeUrlPrefix + "/" + scalaPbMessage.companion.scalaDescriptor.fullName, scalaPbMessage.toByteString)

      case null =>
        throw NullSerializationException

      case _ if ClassToPrimitives.contains(value.getClass) =>
        val primitive = ClassToPrimitives(value.getClass)
        ScalaPbAny(primitive.fullName, primitiveToBytes(primitive, value))

      case byteString: ByteString =>
        ScalaPbAny(BytesPrimitive.fullName, primitiveToBytes(BytesPrimitive, byteString))

      case other =>
        throw SerializationException(
          s"Don't know how to serialize object of type ${other.getClass}. Try passing a protobuf or use a primitive type.")
    }

  /**
   * Decodes a JavaPbAny wrapped proto message into the concrete user message type or a ScalaPbAny wrapped Kalix
   * primitive into the Java primitive type value. Must only be used where primitive values are expected.
   */
  def decodePossiblyPrimitive(any: ScalaPbAny): Any = {
    val typeUrl = any.typeUrl
    if (typeUrl.startsWith(KalixPrimitive)) {
      // Note that this decodes primitive bytestring and string but not json which falls over to message decode below
      NameToPrimitives.get(typeUrl) match {
        case Some(primitive) =>
          bytesToPrimitive(primitive, any.value)
        case None =>
          throw SerializationException("Unknown primitive type url: " + typeUrl)
      }
    } else {
      decodeMessage(any)
    }
  }

  /**
   * Decodes a JavaPbAny wrapped proto message into the concrete user message type or a Kalix specific wrapping of
   * bytes, string or strings containing JSON into com.google.protobuf.{BytesValue, StringValue} which the user method
   * is expected to accept for such messages (for example coming from a topic).
   *
   * Other JavaPbAny wrapped primitives are not expected, but the wrapped value is passed through as it is.
   */
  def decodeMessage(any: ScalaPbAny): Any = {
    val typeUrl = any.typeUrl
    if (typeUrl.equals(BytesPrimitive.fullName)) {
      // raw byte strings we turn into BytesValue and expect service method to accept
      val bytes = bytesToPrimitive(BytesPrimitive, any.value)
      if (prefer == PREFER_JAVA)
        com.google.protobuf.BytesValue.of(bytes)
      else
        com.google.protobuf.wrappers.BytesValue.of(bytes)

    } else if (typeUrl.equals(StringPrimitive.fullName)) {
      // strings as StringValue
      val string = bytesToPrimitive(StringPrimitive, any.value)
      if (prefer == PREFER_JAVA)
        com.google.protobuf.StringValue.of(string)
      else
        com.google.protobuf.wrappers.StringValue.of(string)

    } else if (typeUrl.startsWith(JsonSupport.KALIX_JSON)) {
      // we do not actually parse JSON here but returns it as is and let the user
      // decide which json type to try decode it into etc. based on the type_url which
      // may have additional detail about what it can be JSON-deserialized into
      if (prefer == PREFER_JAVA)
        ScalaPbAny.toJavaProto(any)
      else
        any

    } else if (typeUrl.startsWith(KalixPrimitive)) {
      // pass on as is, the generated types will not match the primitive type if we unwrap/decode
      any
    } else {
      // wrapped concrete protobuf message, parse into the right type
      val typeName = typeUrl.split("/", 2) match {
        case Array(host, typeName) =>
          if (host != typeUrlPrefix) {
            log.warn(
              "Message type [{}] does not match configured type url prefix [{}]",
              typeUrl: Any,
              typeUrlPrefix: Any)
          }
          typeName
        case _ =>
          log.warn(
            "Message type [{}] does not have a url prefix, it should have one that matches the configured type url prefix [{}]",
            typeUrl: Any,
            typeUrlPrefix: Any)
          typeUrl
      }

      resolveTypeUrl(typeName) match {
        case Some(parser) =>
          try {
            parser.parseFrom(any.value)
          } catch {
            case ex: scalapb.validate.FieldValidationException =>
              throw BadRequestException(ex.getMessage)
          }
        case None =>
          throw SerializationException("Unable to find descriptor for type: " + typeUrl)
      }
    }
  }

  override def typeUrlFor(clz: Class[_]): String = clz.getName
}

final case class SerializationException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause)
object NullSerializationException extends RuntimeException("Don't know how to serialize object of type null.")

/**
 * INTERNAL API
 */
// only here to avoid MODULE$ forwarder mess from Java
private[kalix] object ByteStringEncoding {

  def encodePrimitiveBytes(bytes: ByteString): ByteString =
    AnySupport.encodePrimitiveBytes(bytes)

  def decodePrimitiveBytes(bytes: ByteString): ByteString =
    AnySupport.decodePrimitiveBytes(bytes)

}

trait MessageCodec {
  def decodeMessage(any: ScalaPbAny): Any
  def encodeScala(value: Any): ScalaPbAny
  def encodeJava(value: Any): JavaPbAny
  def typeUrlFor(clz: Class[_]): String
}
