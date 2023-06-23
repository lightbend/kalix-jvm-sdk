/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.javasdk.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.any.Any;
import kalix.javasdk.JsonSupport;
import kalix.javasdk.impl.AnySupport;
import kalix.javasdk.impl.ComponentDescriptor;
import kalix.javasdk.impl.JsonMessageCodec;
import kalix.javasdk.impl.RestDeferredCall;
import kalix.javasdk.impl.Validations;
import kalix.spring.impl.RestKalixClientImpl;
import kalix.spring.testmodels.Message;
import kalix.spring.testmodels.Number;
import kalix.spring.testmodels.action.ActionsTestModels.GetClassLevel;
import kalix.spring.testmodels.action.ActionsTestModels.GetWithOneParam;
import kalix.spring.testmodels.action.ActionsTestModels.GetWithoutParam;
import kalix.spring.testmodels.action.ActionsTestModels.PostWithOneQueryParam;
import kalix.spring.testmodels.action.ActionsTestModels.PostWithTwoParam;
import kalix.spring.testmodels.action.ActionsTestModels.PostWithoutParam;
import kalix.spring.testmodels.valueentity.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class ComponentClientTest {

  private final JsonMessageCodec messageCodec = new JsonMessageCodec();
  private RestKalixClientImpl restKalixClient;
  private ComponentClient componentClient;

  @BeforeEach
  public void initEach() {
    restKalixClient = new RestKalixClientImpl(messageCodec);
    componentClient = new ComponentClient(restKalixClient);
  }

  @Test
  public void shouldReturnDeferredCallForSimpleGETRequest() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(GetWithoutParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction().call(GetWithoutParam::message);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message());
  }

  @Test
  public void shouldReturnDeferredCallForGETRequestWithParam() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(GetWithOneParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");
    String param = "a b&c@d";

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction()
        .call(GetWithOneParam::message)
        .params(param);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param);
  }

  @Test
  public void shouldReturnDeferredCallForGETRequestWithTwoParams() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(GetClassLevel.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");
    String param = "a b&c@d";
    Long param2 = 2L;

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction()
        .call(GetClassLevel::message)
        .params(param, param2);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param, param2);
  }

  @Test
  public void shouldReturnDeferredCallForGETRequestWithTwoPathParamsAnd2ReqParams() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(GetClassLevel.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message2");
    String param = "a b&c@d";
    Long param2 = 2L;
    String param3 = "!@!#$%^%++___";
    int param4 = 4;

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction()
        .call(GetClassLevel::message2)
        .params(param, param2, param3, param4);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param, param2, param3, param4);
  }

  @Test
  public void shouldReturnDeferredCallForGETRequestWithListAsReqParam() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(GetClassLevel.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message3");
    String param = "a b&c@d";
    Long param2 = 2L;
    String param3 = "!@!#$%^%++___";
    List<String> param4 = List.of("1", "2");

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction()
        .call(GetClassLevel::message3)
        .params(param, param2, param3, param4);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param, param2, param3, param4);
  }

  @Test
  public void shouldReturnDeferredCallForSimplePOSTRequest() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(PostWithoutParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");
    Message body = new Message("hello world");

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction().call(PostWithoutParam::message).params(body);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertThat(getBody(targetMethod, call.message(), Message.class)).isEqualTo(body);
  }

  @Test
  public void shouldReturnDeferredCallForPOSTRequestWithTwoParamsAndBody() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(PostWithTwoParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");
    String param = "a b&c@d";
    Long param2 = 2L;
    Message body = new Message("hello world");

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction()
        .call(PostWithTwoParam::message)
        .params(param, param2, body);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param, param2);
    assertThat(getBody(targetMethod, call.message(), Message.class)).isEqualTo(body);
  }

  @Test
  public void shouldReturnDeferredCallForPOSTRequestWhenMultipleMethodsAreAvailable() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(PostWithoutParam.class, messageCodec);
    var action2 = descriptorFor(PostWithTwoParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    restKalixClient.registerComponent(action2.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");
    Message body = new Message("hello world");

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction().call(PostWithoutParam::message).params(body);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertThat(getBody(targetMethod, call.message(), Message.class)).isEqualTo(body);
  }

  @Test
  public void shouldReturnDeferredCallForPOSTWithRequestParams() throws InvalidProtocolBufferException {
    //given
    var action = descriptorFor(PostWithOneQueryParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    var targetMethod = action.serviceDescriptor().findMethodByName("Message");
    String param = "a b&c@d";
    Message body = new Message("hello world");

    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction()
        .call(PostWithOneQueryParam::message)
        .params(param, body);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param);
    assertThat(getBody(targetMethod, call.message(), Message.class)).isEqualTo(body);
  }

  @Test
  public void shouldReturnDeferredCallForVEWithRandomId() throws InvalidProtocolBufferException {
    //given
    var counterVE = descriptorFor(Counter.class, messageCodec);
    restKalixClient.registerComponent(counterVE.serviceDescriptor());
    var targetMethod = counterVE.serviceDescriptor().findMethodByName("RandomIncrease");
    Integer param = 10;

    //when
    RestDeferredCall<Any, Number> call = (RestDeferredCall<Any, Number>) componentClient.forValueEntity()
        .call(Counter::randomIncrease)
        .params(param);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), param);
  }

  private ComponentDescriptor descriptorFor(Class<?> clazz, JsonMessageCodec messageCodec) {
    Validations.validate(clazz).failIfInvalid();
    return ComponentDescriptor.descriptorFor(clazz, messageCodec);
  }

  private <T> T getBody(Descriptors.MethodDescriptor targetMethod, Any message, Class<T> clazz) throws InvalidProtocolBufferException {
    var dynamicMessage = DynamicMessage.parseFrom(targetMethod.getInputType(), message.value());
    var body = (DynamicMessage) targetMethod.getInputType()
        .getFields().stream()
        .filter(f -> f.getName().equals("json_body"))
        .map(dynamicMessage::getField)
        .findFirst().orElseThrow();

    return decodeJson(body, clazz);
  }

  private <T> T decodeJson(DynamicMessage dm, Class<T> clazz) {
    String typeUrl = (String) dm.getField(Any.javaDescriptor().findFieldByName("type_url"));
    ByteString bytes = (ByteString) dm.getField(Any.javaDescriptor().findFieldByName("value"));

    var any = com.google.protobuf.Any.newBuilder().setTypeUrl(typeUrl).setValue(bytes).build();

    return JsonSupport.decodeJson(clazz, any);
  }

  private void assertMethodParamsMatch(Descriptors.MethodDescriptor targetMethod, Any message, Object... methodArgs) throws InvalidProtocolBufferException {
    assertThat(message.typeUrl()).isEqualTo(AnySupport.DefaultTypeUrlPrefix() + "/" + targetMethod.getInputType().getFullName());
    var dynamicMessage = DynamicMessage.parseFrom(targetMethod.getInputType(), message.value());

    List<Object> args = targetMethod.getInputType().getFields().stream().filter(f -> !f.getName().equals("json_body")).map(dynamicMessage::getField).toList();

    assertThat(args).containsOnly(methodArgs);
  }
}