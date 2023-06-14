package kalix.javasdk.impl.client

import com.google.protobuf.any.Any
import kalix.javasdk.DeferredCall
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.PathParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.QueryParamParameter
import kalix.javasdk.impl.reflection.SyntheticRequestServiceMethod
import kalix.spring.KalixClient
import kalix.spring.impl.RestKalixClientImpl
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.RequestMethod

import java.lang.reflect.ParameterizedType

class ServiceCall {

}

class ServiceCall2[A1, A2, R](kalixClient: KalixClient, lambda: scala.Any) {

  def invoke(a1: A1, a2: A2): DeferredCall[Any, R] = {

    val method = MethodRefResolver.resolveMethodRef(lambda)

    //TODO add params to uri placeholder
    //TODO get body from params by index

    val returnType =
      method.getGenericReturnType.asInstanceOf[ParameterizedType].getActualTypeArguments.head.asInstanceOf[Class[R]]

    val restService: RestServiceIntrospector.RestService =
      RestServiceIntrospector.inspectService(method.getDeclaringClass)
    val restMethod: SyntheticRequestServiceMethod =
      restService.methods.find(_.javaMethod.getName == method.getName).get

    val requestMethod: RequestMethod = restMethod.requestMethod

    val bodyIndex = restMethod.params.collect { case p: BodyParameter => p }.map(_.param.getParameterIndex).headOption

    val params = Seq(a1, a2);

    val map: LinkedMultiValueMap[String, String] = new LinkedMultiValueMap()
    restMethod.params
      .collect { case p: QueryParamParameter => p }
      .foreach(param => {
        map.add(param.name, params(param.param.getParameterIndex).toString) //TODO null?
      })

    val pathVariables: Map[String, String] = restMethod.params
      .collect { case p: PathParameter => p }
      .map(param => (param.name, params(param.param.getParameterIndex).toString))
      .toMap + ("id" -> "123")

    kalixClient.asInstanceOf[RestKalixClientImpl].post(restMethod.parsedPath.path, map, pathVariables, None, returnType)
  }
}
