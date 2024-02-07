package com.example.tracing.api;

import com.example.tracing.domain.UserEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.client.RestTemplate;


@Subscribe.EventSourcedEntity(value = UserEntity.class, ignoreUnknown = true)
public class GetRandomNameActionSync extends Action {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GetRandomNameActionSync.class);

  private final RestTemplate restTemplate;
  private final Tracer tracer;
  private final ComponentClient componentClient;

  public GetRandomNameActionSync(ActionCreationContext context, ComponentClient componentClient) {
    this.restTemplate = new RestTemplate();
    this.tracer = context.getTracer().orElse(null);
    this.componentClient = componentClient;
  }

  public Effect<String> handleAdd(UserEvent.UserAdded userAdded) {
    if (actionContext().eventSubject().isPresent()) {
      var randomName = getRandomName();

      var updateNameCall = componentClient
          .forEventSourcedEntity(actionContext().eventSubject().get())
          .call(UserEntity::updateName)
          .params(new UserEntity.UserCmd.UpdateNameCmd(randomName));

      return effects().forward(updateNameCall);
    } else {
      return effects().ignore();
    }
  }

  // gets random name from external API using a synchronous call
  private String getRandomName() {
    Span span = startSpan("random-name-sync");
    try (Scope ignored = span.makeCurrent()) {
      RandomNameResult result = this.restTemplate.getForObject("https://randomuser.me/api/?inc=name&noinfo", RandomNameResult.class);
      span.setAttribute("user.id", actionContext().eventSubject().orElse("unknown"));
      span.setAttribute("random.name", result.name());
      return result.name();
    } finally {
      span.end();
    }
  }

  private Span startSpan(String spanName) {
    var extractedContext = actionContext().metadata().traceContext().get();

    return tracer
        .spanBuilder(spanName)
        .setParent(extractedContext)
        .setSpanKind(SpanKind.CLIENT)
        .startSpan();
  }
}
