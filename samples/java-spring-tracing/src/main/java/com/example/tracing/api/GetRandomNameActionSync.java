package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.client.RestTemplate;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomNameActionSync extends Action {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetRandomNameActionSync.class);

  private final Tracer tracer;
  private final ComponentClient componentClient;

  public GetRandomNameActionSync(Tracer tracer, ComponentClient componentClient) {
    this.tracer = tracer;
    this.componentClient = componentClient;
  }

  public Effect<String> handleAdd(UserEvent.UserAdded userAdded) {
    if (actionContext().eventSubject().isPresent()) {
      var randomName = getRandomNameSync();

      var updateNameCall = componentClient
          .forEventSourcedEntity(actionContext().eventSubject().get())
          .call(UserEntity::updateName)
          .params(new UserEntity.UserCmd.UpdateNameCmd(randomName));

      return effects().forward(updateNameCall);
    } else {
      return effects().ignore();
    }
  }

  public Effect<String> handleAdd(UserEvent.UserPhotoUpdated photoUpdated) {
    return effects().ignore();
  }

  // gets random name from external API using a synchronous call and traces that call
  private String getRandomNameSync() {
    var otelCurrentContext = actionContext().metadata().traceContext().asOpenTelemetryContext();
    Span span = tracer
        .spanBuilder("random-name-sync")
        .setParent(otelCurrentContext)
        .setSpanKind(SpanKind.CLIENT)
        .startSpan();

    try (Scope ignored = span.makeCurrent()) {
      RandomUserApi.Name result = new RestTemplate().getForObject("https://randomuser.me/api/?inc=name&noinfo", RandomUserApi.Name.class);
      span.setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));
      span.setAttribute("random.name", result.name());
      return result.name();
    } catch (Exception e) {
      span.setStatus(StatusCode.ERROR, "Failed to fetch name: " + e.getMessage());
    } finally {
      span.end();
    }
    return "unknown";
  }
}
