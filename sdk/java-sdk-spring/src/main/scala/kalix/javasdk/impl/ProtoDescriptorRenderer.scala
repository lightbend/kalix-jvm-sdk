/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import scala.collection.mutable

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.Descriptors.FileDescriptor

object ProtoDescriptorRenderer {

  def toString(fileDescriptor: FileDescriptor): String = {
    // not water tight but better than the default non-protobuf Proto-toString format
    val proto = fileDescriptor.toProto
    val builder = new mutable.StringBuilder()
    builder ++= s"""syntax = "${fileDescriptor.toProto.getSyntax.toLowerCase}";\n\n"""

    builder ++= "package "
    builder ++= fileDescriptor.getPackage + ";\n\n"

    fileDescriptor.getDependencies.forEach { deps =>
      builder ++= s"""import "${deps.getFullName}";\n"""
    }
    builder ++= "\n"

    if (proto.hasOptions) {
      proto.getOptions.getAllFields.forEach { (option, value) =>
        builder ++= "  option ("
        builder ++= option.getFullName
        builder ++= ") = {\n"
        value.toString.split("\n").foreach { optionLine =>
          val indent = optionLine.count(_ == ' ') / 4
          builder ++= "    "
          builder ++= (" " * indent)
          builder ++= optionLine.replace("^[ ]*", "")
          builder ++= "\n"
        }
        builder ++= "    };\n"
      }
    }

    proto.getMessageTypeList.forEach { messageType =>
      builder ++= "message "
      builder ++= messageType.getName
      builder ++= " {\n"
      messageType.getFieldList.forEach { field =>
        builder ++= "  "
        if (field.getLabel == FieldDescriptorProto.Label.LABEL_REPEATED)
          builder ++= "repeated "
        else if (field.getLabel == FieldDescriptorProto.Label.LABEL_REQUIRED)
          builder ++= "required "
        else if (field.getProto3Optional)
          builder ++= "optional "
        if (field.hasTypeName)
          builder ++= field.getTypeName
        else
          builder ++= field.getType.name().toLowerCase.drop(5)
        builder ++= " "
        builder ++= field.getName
        builder ++= " = ";
        builder ++= field.getNumber.toString
        if (field.hasOptions) {
          field.getOptions.getAllFields.forEach { (option, value) =>
            builder ++= " [("
            builder ++= option.getFullName
            builder ++= ")."
            builder ++= value.toString.replace("\n", "").replace(':', '=')
            builder ++= "]"
          }
        }
        builder ++= ";\n"
      }
      builder ++= "}\n\n"
    }
    proto.getServiceList.forEach { service =>
      builder ++= "service "
      builder ++= service.getName
      builder ++= " {\n"
      if (service.hasOptions) {
        service.getOptions.getAllFields.forEach { (option, value) =>
          builder ++= "  option ("
          builder ++= option.getFullName
          builder ++= ") = {\n"
          value.toString.split("\n").foreach { optionLine =>
            val indent = optionLine.count(_ == ' ') / 4
            builder ++= "    "
            builder ++= (" " * indent)
            builder ++= optionLine.replace("^[ ]*", "")
            builder ++= "\n"
          }
          builder ++= "    };\n"
        }
      }

      service.getMethodList.forEach { method =>
        builder ++= "  rpc "
        builder ++= method.getName
        builder ++= "("
        if (method.getClientStreaming) builder ++= "stream "
        builder ++= method.getInputType
        builder ++= ") returns ("
        if (method.getServerStreaming) builder ++= "stream "
        builder ++= method.getOutputType
        builder ++= ") "
        if (method.hasOptions) {
          builder ++= "{\n"
          method.getOptions.getAllFields.forEach { (option, value) =>
            builder ++= "    option ("
            builder ++= option.getFullName
            builder ++= ") = {\n"
            value.toString.split("\n").foreach { optionLine =>
              val indent = optionLine.count(_ == ' ') / 4
              builder ++= "      "
              builder ++= (" " * indent)
              builder ++= optionLine.replace("^[ ]*", "")
              builder ++= "\n"
            }
            builder ++= "    };\n"
          }
          builder ++= "  }\n"
        } else builder ++= "{}\n\n"
      }
      builder ++= "}\n\n"
    }
    builder.toString()
  }

}
