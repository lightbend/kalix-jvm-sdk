/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.any.Any;
import kalix.javasdk.JsonSupport;
import kalix.javasdk.Metadata;
import kalix.javasdk.impl.*;
import kalix.javasdk.impl.client.ComponentClientImpl;
import kalix.javasdk.impl.telemetry.Telemetry;
import kalix.spring.impl.RestKalixClientImpl;
import kalix.spring.testmodels.Message;
import kalix.spring.testmodels.Number;
import kalix.spring.testmodels.action.ActionsTestModels.*;
import kalix.spring.testmodels.valueentity.Counter;
import kalix.spring.testmodels.valueentity.User;
import kalix.spring.testmodels.view.ViewTestModels.UserByEmailWithGet;
import kalix.spring.testmodels.view.ViewTestModels.UserByEmailWithGetWithoutAnnotation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class ComponentClientTest {

  private final JsonMessageCodec messageCodec = new JsonMessageCodec();
  private RestKalixClientImpl restKalixClient;
  private ComponentClientImpl componentClient;

  @BeforeEach
  public void initEach() {
    restKalixClient = new RestKalixClientImpl(messageCodec);
    componentClient = new ComponentClientImpl(restKalixClient);
  }

  @Test
  public void shouldNotReturnDeferredCallMethodNotAnnotatedAsRESTEndpoint() {
    assertThatThrownBy(() -> componentClient.forAction().call(GetWithoutParam::missingRestAnnotation))
      .hasMessage("Method [missingRestAnnotation] is not annotated as a REST endpoint.");
  }

  @Test
  public void shouldFailWhenCallingOtherComponentFromViewCallBuilder() {
    assertThatThrownBy(() -> componentClient.forView().call(GetWithoutParam::missingRestAnnotation))
      .hasMessage("Use dedicated builder for calling Action component method GetWithoutParam::missingRestAnnotation. This builder is meant for View component calls.");
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
  public void shouldReturnDefferedCallWithTraceparent() {
    //given
    var action = descriptorFor(PostWithOneQueryParam.class, messageCodec);
    restKalixClient.registerComponent(action.serviceDescriptor());
    String param = "a b&c@d";
    Message body = new Message("hello world");
    String traceparent = "074c4c8d-d87c-4573-847f-77951ce4e0a4";
    Metadata metadata = MetadataImpl.Empty().set(Telemetry.TRACE_PARENT_KEY(), traceparent);
    componentClient.setCallMetadata(metadata);
    //when
    RestDeferredCall<Any, Message> call = (RestDeferredCall<Any, Message>) componentClient.forAction().call(PostWithOneQueryParam::message).params(param, body);

    //then
    assertThat(call.metadata().get(Telemetry.TRACE_PARENT_KEY()).get()).isEqualTo(traceparent);
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

  @Test
  public void shouldFailWhenCallingViewWithNotAnnotatedParams() {
    assertThatThrownBy(() -> componentClient.forView().call(UserByEmailWithGetWithoutAnnotation::getUser))
      .hasMessage("When using ComponentClient each [getUser] View query method parameter should be annotated with @PathVariable, @RequestParam or @RequestBody annotations. Missing annotations for params with types: [String]");
  }

  @Test
  public void shouldFailWhenCallingViewMethodWithoutQueryAnnotation() {
    assertThatThrownBy(() -> componentClient.forView().call(UserByEmailWithGetWithoutAnnotation::getUserWithoutQuery))
      .hasMessage("A View query method [getUserWithoutQuery] should be annotated with @Query annotation.");
  }

  @Test
  public void shouldReturnDeferredCallForViewRequest() throws InvalidProtocolBufferException {
    //given
    var view = descriptorFor(UserByEmailWithGet.class, messageCodec);
    restKalixClient.registerComponent(view.serviceDescriptor());
    var targetMethod = view.serviceDescriptor().findMethodByName("GetUser");
    String email = "email@example.com";

    //when
    RestDeferredCall<Any, User> call = (RestDeferredCall<Any, User>) componentClient.forView()
      .call(UserByEmailWithGet::getUser)
      .params(email);

    //then
    assertThat(call.fullServiceName()).isEqualTo(targetMethod.getService().getFullName());
    assertThat(call.methodName()).isEqualTo(targetMethod.getName());
    assertMethodParamsMatch(targetMethod, call.message(), email);
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