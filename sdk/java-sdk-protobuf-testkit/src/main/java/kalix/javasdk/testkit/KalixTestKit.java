/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit;

import akka.actor.ActorSystem;
import akka.annotation.InternalApi;
import akka.grpc.GrpcClientSettings;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.pattern.Patterns;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import kalix.javasdk.Kalix;
import kalix.javasdk.KalixRunner;
import kalix.javasdk.Principal;
import kalix.javasdk.impl.GrpcClients;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.ProxyInfoHolder;
import kalix.javasdk.testkit.EventingTestKit.IncomingMessages;
import kalix.javasdk.testkit.impl.KalixRuntimeContainer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static kalix.javasdk.testkit.impl.KalixRuntimeContainer.DEFAULT_GOOGLE_PUBSUB_PORT;
import static kalix.javasdk.testkit.impl.KalixRuntimeContainer.DEFAULT_KAFKA_PORT;
import static kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport.GOOGLE_PUBSUB;
import static kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport.KAFKA;
import static kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport.TEST_BROKER;

/**
 * Testkit for running Kalix services locally.
 *
 * <p>Requires Docker for starting a local instance of the Kalix Runtime.
 *
 * <p>Create a KalixTestkit with an {@link Kalix} service descriptor, and then {@link #start} the
 * testkit before testing the service with gRPC or HTTP clients. Call {@link #stop} after tests are
 * complete.
 */
public class KalixTestKit {

  public static class MockedEventing {
    public static final String VALUE_ENTITY = "value-entity";
    public static final String EVENT_SOURCED_ENTITY = "event-sourced-entity";
    public static final String STREAM = "stream";
    public static final String TOPIC = "topic";
    private final Map<String, Set<String>> mockedIncomingEvents; //Subscriptions
    private final Map<String, Set<String>> mockedOutgoingEvents; //Destination

    private MockedEventing() {
      this(new HashMap<>(), new HashMap<>());
    }

    private MockedEventing(Map<String, Set<String>> mockedIncomingEvents, Map<String, Set<String>> mockedOutgoingEvents) {
      this.mockedIncomingEvents = mockedIncomingEvents;
      this.mockedOutgoingEvents = mockedOutgoingEvents;
    }

    public static MockedEventing EMPTY = new MockedEventing();

    public MockedEventing withValueEntityIncomingMessages(String typeId) {
      Map<String, Set<String>> copy = new HashMap<>(mockedIncomingEvents);
      copy.compute(VALUE_ENTITY, updateValues(typeId));
      return new MockedEventing(copy, new HashMap<>(mockedOutgoingEvents));
    }

    public MockedEventing withEventSourcedIncomingMessages(String typeId) {
      Map<String, Set<String>> copy = new HashMap<>(mockedIncomingEvents);
      copy.compute(EVENT_SOURCED_ENTITY, updateValues(typeId));
      return new MockedEventing(copy, new HashMap<>(mockedOutgoingEvents));
    }

    public MockedEventing withStreamIncomingMessages(String service, String streamId) {
      Map<String, Set<String>> copy = new HashMap<>(mockedIncomingEvents);
      copy.compute(STREAM, updateValues(service + "/" + streamId));
      return new MockedEventing(copy, new HashMap<>(mockedOutgoingEvents));
    }

    public MockedEventing withTopicIncomingMessages(String topic) {
      Map<String, Set<String>> copy = new HashMap<>(mockedIncomingEvents);
      copy.compute(TOPIC, updateValues(topic));
      return new MockedEventing(copy, new HashMap<>(mockedOutgoingEvents));
    }

    public MockedEventing withTopicOutgoingMessages(String topic) {
      Map<String, Set<String>> copy = new HashMap<>(mockedOutgoingEvents);
      copy.compute(TOPIC, updateValues(topic));
      return new MockedEventing(new HashMap<>(mockedIncomingEvents), copy);
    }

    @NotNull
    private BiFunction<String, Set<String>, Set<String>> updateValues(String typeId) {
      return (key, currentValues) -> {
        if (currentValues == null) {
          LinkedHashSet<String> values = new LinkedHashSet<>(); //order is relevant only for tests
          values.add(typeId);
          return values;
        } else {
          currentValues.add(typeId);
          return currentValues;
        }
      };
    }

    @Override
    public String toString() {
      return "MockedEventing{" +
          "mockedIncomingEvents=" + mockedIncomingEvents +
          ", mockedOutgoingEvents=" + mockedOutgoingEvents +
          '}';
    }

    public boolean hasIncomingConfig() {
      return !mockedIncomingEvents.isEmpty();
    }

    public boolean hasConfig() {
      return hasIncomingConfig() || hasOutgoingConfig();
    }

    public boolean hasOutgoingConfig() {
      return !mockedOutgoingEvents.isEmpty();
    }

    public String toIncomingFlowConfig() {
      return toConfig(mockedIncomingEvents);
    }

    public String toOutgoingFlowConfig() {
      return toConfig(mockedOutgoingEvents);
    }

    private String toConfig(Map<String, Set<String>> configs) {
      return configs.entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .flatMap(entry -> {
            String subscriptionType = entry.getKey();
            return entry.getValue().stream().map(name -> subscriptionType + "," + name);
          }).collect(Collectors.joining(";"));
    }

    boolean hasValueEntitySubscription(String typeId) {
      return checkExistence(VALUE_ENTITY, typeId);
    }

    boolean hasEventSourcedEntitySubscription(String typeId) {
      return checkExistence(EVENT_SOURCED_ENTITY, typeId);
    }

    boolean hasStreamSubscription(String service, String streamId) {
      return checkExistence(STREAM, service + "/" + streamId);
    }

    boolean hasTopicSubscription(String topic) {
      return checkExistence(TOPIC, topic);
    }

    boolean hasTopicDestination(String topic) {
      Set<String> values = mockedOutgoingEvents.get(TOPIC);
      return values != null && values.contains(topic);
    }

    private boolean checkExistence(String type, String name) {
      Set<String> values = mockedIncomingEvents.get(type);
      return values != null && values.contains(name);
    }
  }

  /**
   * Settings for KalixTestkit.
   */
  public static class Settings {
    /**
     * Default stop timeout (10 seconds).
     */
    public static Duration DEFAULT_STOP_TIMEOUT = Duration.ofSeconds(10);
    /**
     * Default settings for KalixTestkit.
     */
    public static Settings DEFAULT = new Settings(DEFAULT_STOP_TIMEOUT);

    /**
     * Timeout setting for stopping the local Kalix test instance.
     */
    public final Duration stopTimeout;

    /**
     * The name of this service when deployed.
     */
    public final String serviceName;

    /**
     * Whether ACL checking is enabled.
     */
    public final boolean aclEnabled;

    /**
     * Whether advanced View features are enabled.
     */
    public final boolean advancedViews;

    /**
     * To override workflow tick interval for integration tests
     */
    public final Optional<Duration> workflowTickInterval;

    /**
     * Service port mappings from serviceName to host:port
     */
    public final Map<String, String> servicePortMappings;

    public final EventingSupport eventingSupport;

    public final MockedEventing mockedEventing;

    /**
     * Create new settings for KalixTestkit.
     *
     * @param stopTimeout timeout to use when waiting for Kalix to stop
     * @deprecated Use Settings.DEFAULT.withStopTimeout() instead.
     */
    @Deprecated
    public Settings(final Duration stopTimeout) {
      this(stopTimeout, "self", false, false, Optional.empty(), Collections.emptyMap(), TEST_BROKER, MockedEventing.EMPTY);
    }

    public enum EventingSupport {
      /**
       * This is the default type used and allows the testing eventing integrations without an external broker dependency
       * running.
       */
      TEST_BROKER,

      /**
       * Used if you want to use an external Google PubSub (or its Emulator) on your tests.
       * <p>
       * Note: the Google PubSub broker instance needs to be started independently.
       */
      GOOGLE_PUBSUB,

      /**
       * Used if you want to use an external Kafka broker on your tests.
       * <p>
       * Note: the Kafka broker instance needs to be started independently.
       */
      KAFKA
    }

    private Settings(
        final Duration stopTimeout,
        final String serviceName,
        final boolean aclEnabled,
        final boolean advancedViews,
        final Optional<Duration> workflowTickInterval,
        final Map<String, String> servicePortMappings,
        final EventingSupport eventingSupport,
        final MockedEventing mockedEventing) {
      this.stopTimeout = stopTimeout;
      this.serviceName = serviceName;
      this.aclEnabled = aclEnabled;
      this.advancedViews = advancedViews;
      this.workflowTickInterval = workflowTickInterval;
      this.servicePortMappings = servicePortMappings;
      this.eventingSupport = eventingSupport;
      this.mockedEventing = mockedEventing;
    }

    /**
     * Set a custom stop timeout, for stopping the local Kalix test instance.
     *
     * @param stopTimeout timeout to use when waiting for Kalix to stop
     * @return updated Settings
     */
    public Settings withStopTimeout(final Duration stopTimeout) {
      return new Settings(stopTimeout, serviceName, aclEnabled, advancedViews, workflowTickInterval, servicePortMappings, eventingSupport, mockedEventing);
    }

    /**
     * Set the name of this service. This will be used by the service when making calls on other
     * services run by the testkit to authenticate itself, allowing those services to apply ACLs
     * based on that name.
     *
     * @param serviceName The name of this service.
     * @return The updated settings.
     */
    public Settings withServiceName(final String serviceName) {
      return new Settings(stopTimeout, serviceName, aclEnabled, advancedViews, workflowTickInterval, servicePortMappings, eventingSupport, mockedEventing);
    }

    /**
     * Disable ACL checking in this service.
     *
     * @return The updated settings.
     */
    public Settings withAclDisabled() {
      return new Settings(stopTimeout, serviceName, false, advancedViews, workflowTickInterval, servicePortMappings, eventingSupport, mockedEventing);
    }

    /**
     * Enable ACL checking in this service.
     *
     * @return The updated settings.
     */
    public Settings withAclEnabled() {
      return new Settings(stopTimeout, serviceName, true, advancedViews, workflowTickInterval, servicePortMappings, eventingSupport, mockedEventing);
    }

    /**
     * Enable advanced View features for this service.
     *
     * @return The updated settings.
     */
    public Settings withAdvancedViews() {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, workflowTickInterval, servicePortMappings, eventingSupport, mockedEventing);
    }

    /**
     * Overrides workflow tick interval
     *
     * @return The updated settings.
     */
    public Settings withWorkflowTickInterval(Duration tickInterval) {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, Optional.of(tickInterval), servicePortMappings, eventingSupport, mockedEventing);
    }

    /**
     * Mock the incoming messages flow from a ValueEntity.
     */
    public Settings withValueEntityIncomingMessages(String typeId) {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, workflowTickInterval, servicePortMappings, eventingSupport,
          mockedEventing.withValueEntityIncomingMessages(typeId));
    }

    /**
     * Mock the incoming events flow from an EventSourcedEntity.
     */
    public Settings withEventSourcedEntityIncomingMessages(String typeId) {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, workflowTickInterval, servicePortMappings, eventingSupport,
          mockedEventing.withEventSourcedIncomingMessages(typeId));
    }

    /**
     * Mock the incoming messages flow from a Stream (eventing.in.direct in case of protobuf SDKs).
     */
    public Settings withStreamIncomingMessages(String service, String streamId) {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, workflowTickInterval, servicePortMappings, eventingSupport,
          mockedEventing.withStreamIncomingMessages(service, streamId));
    }

    /**
     * Mock the incoming events flow from a Topic.
     */
    public Settings withTopicIncomingMessages(String topic) {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, workflowTickInterval, servicePortMappings, eventingSupport,
          mockedEventing.withTopicIncomingMessages(topic));
    }

    /**
     * Mock the outgoing events flow for a Topic.
     */
    public Settings withTopicOutgoingMessages(String topic) {
      return new Settings(stopTimeout, serviceName, aclEnabled, true, workflowTickInterval, servicePortMappings, eventingSupport,
          mockedEventing.withTopicOutgoingMessages(topic));
    }

    /**
     * Add a service port mapping from serviceName to host:port.
     *
     * @return The updated settings.
     */
    public Settings withServicePortMapping(String serviceName, String host, int port) {
      var updatedMappings = new HashMap<>(servicePortMappings);
      updatedMappings.put(serviceName, host + ":" + port);
      return new Settings(stopTimeout, serviceName, aclEnabled, advancedViews, workflowTickInterval, Map.copyOf(updatedMappings), eventingSupport, mockedEventing);
    }

    public Settings withEventingSupport(EventingSupport eventingSupport) {
      return new Settings(stopTimeout, serviceName, aclEnabled, advancedViews, workflowTickInterval, servicePortMappings, eventingSupport, mockedEventing);
    }

    @Override
    public String toString() {
      var portMappingsRendered =
          servicePortMappings.entrySet().stream()
              .map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList());

      return "Settings(" +
          "stopTimeout=" + stopTimeout +
          ", serviceName='" + serviceName + '\'' +
          ", aclEnabled=" + aclEnabled +
          ", advancedViews=" + advancedViews +
          ", workflowTickInterval=" + workflowTickInterval +
          ", servicePortMappings=[" + String.join(", ", portMappingsRendered) + "]" +
          ", eventingSupport=" + eventingSupport +
          ", mockedEventing=" + mockedEventing +
          ')';
    }
  }

  private static final Logger log = LoggerFactory.getLogger(KalixTestKit.class);

  private final Kalix kalix;
  private final MessageCodec messageCodec;
  private final EventingTestKit.MessageBuilder messageBuilder;
  private final Settings settings;

  private boolean started = false;
  private String proxyHost;
  private int proxyPort;
  private Optional<KalixRuntimeContainer> runtimeContainer = Optional.empty();
  private KalixRunner runner;
  private ActorSystem testSystem;
  private EventingTestKit eventingTestKit;

  /**
   * Create a new testkit for a Kalix service descriptor.
   *
   * @param kalix Kalix service descriptor
   */
  public KalixTestKit(final Kalix kalix) {
    this(kalix, kalix.getMessageCodec(), Settings.DEFAULT);
  }

  /**
   * Create a new testkit for a Kalix service descriptor with custom settings.
   *
   * @param kalix    Kalix service descriptor
   * @param settings custom testkit settings
   */
  public KalixTestKit(final Kalix kalix, final Settings settings) {
    this(kalix, kalix.getMessageCodec(), settings);
  }

  /**
   * Create a new testkit for a Kalix service descriptor with custom settings.
   *
   * @param kalix        Kalix service descriptor
   * @param messageCodec message codec
   * @param settings     custom testkit settings
   */
  public KalixTestKit(final Kalix kalix, final MessageCodec messageCodec, final Settings settings) {
    this.kalix = kalix;
    this.messageCodec = messageCodec;
    this.messageBuilder = new EventingTestKit.MessageBuilder(messageCodec);
    this.settings = settings;
  }

  /**
   * Start this testkit with default configuration (loaded from {@code application.conf}).
   *
   * @return this KalixTestkit
   */
  public KalixTestKit start() {
    return start(ConfigFactory.load());
  }

  /**
   * Start this testkit with custom configuration (overrides {@code application.conf}).
   *
   * @param config custom test configuration for the KalixRunner
   * @return this KalixTestkit
   */
  public KalixTestKit start(final Config config) {
    if (started)
      throw new IllegalStateException("KalixTestkit already started");

    Boolean useTestContainers = Optional.ofNullable(System.getenv("KALIX_TESTKIT_USE_TEST_CONTAINERS")).map(Boolean::valueOf).orElse(true);
    int port = userServicePort(useTestContainers);
    Map<String, Object> conf = new HashMap<>();
    conf.put("kalix.user-function-port", port);
    // don't kill the test JVM when terminating the KalixRunner
    conf.put("kalix.system.akka.coordinated-shutdown.exit-jvm", "off");
    // integration tests runs the proxy with Testcontainers and therefore
    // we shouldn't load DockerComposeUtils
    conf.put("kalix.dev-mode.docker-compose-file", "none");
    Config testConfig = ConfigFactory.parseMap(conf);

    runner = kalix.createRunner(testConfig.withFallback(config));
    runner.run();

    testSystem = ActorSystem.create("KalixTestkit", ConfigFactory.parseString("akka.http.server.preview.enable-http2 = true"));

    int eventingBackendPort = startEventingTestkit(useTestContainers);
    runProxy(useTestContainers, port, eventingBackendPort);

    started = true;

    if (log.isDebugEnabled())
      log.debug("TestKit using [{}:{}] for calls to proxy from service", proxyHost, proxyPort);

    return this;
  }

  private int startEventingTestkit(Boolean useTestContainers) {
    var port = eventingTestKitPort(useTestContainers);
    if (settings.eventingSupport == TEST_BROKER || settings.mockedEventing.hasConfig()) {
      log.info("Eventing TestKit booting up on port: " + port);
      eventingTestKit = EventingTestKit.start(testSystem, "0.0.0.0", port, messageCodec);
    }
    return port;
  }

  private int eventingTestKitPort(Boolean useTestContainers) {
    if (useTestContainers) {
      return availableLocalPort();
    } else {
      return 8999;
    }
  }

  private void runProxy(Boolean useTestContainers, int port, int grpcEventingBackendPort) {

    if (useTestContainers) {
      var runtimeContainer = new KalixRuntimeContainer(settings.eventingSupport, port, grpcEventingBackendPort);
      this.runtimeContainer = Optional.of(runtimeContainer);
      runtimeContainer.addEnv("SERVICE_NAME", settings.serviceName);
      runtimeContainer.addEnv("ACL_ENABLED", Boolean.toString(settings.aclEnabled));
      runtimeContainer.addEnv("VIEW_FEATURES_ALL", Boolean.toString(settings.advancedViews));

      List<String> javaOptions = new ArrayList<>();
      javaOptions.add("-Dlogback.configurationFile=logback-dev-mode.xml");

      //always passing grpc params, in the case of e.g. testing pubsub with mocked incoming messages
      if (settings.mockedEventing.hasConfig()) {
        javaOptions.add("-Dkalix.proxy.eventing.grpc-backend.host=host.testcontainers.internal");
        javaOptions.add("-Dkalix.proxy.eventing.grpc-backend.port=" + grpcEventingBackendPort);
      }

      if (settings.eventingSupport == TEST_BROKER) {
        javaOptions.add("-Dkalix.proxy.eventing.support=grpc-backend");
      } else if (settings.eventingSupport == KAFKA) {
        javaOptions.add("-Dkalix.proxy.eventing.support=kafka");
        javaOptions.add("-Dkalix.proxy.eventing.kafka.bootstrap-servers=host.testcontainers.internal:" + DEFAULT_KAFKA_PORT);
      } else if (settings.eventingSupport == GOOGLE_PUBSUB) {
        javaOptions.add("-Dkalix.proxy.eventing.support=google-pubsub-emulator");
        javaOptions.add("-Dkalix.proxy.eventing.google-pubsub-emulator-defaults.host=host.testcontainers.internal");
        javaOptions.add("-Dkalix.proxy.eventing.google-pubsub-emulator-defaults.port=" + DEFAULT_GOOGLE_PUBSUB_PORT);
      }
      if (settings.mockedEventing.hasIncomingConfig()) {
        javaOptions.add("-Dkalix.proxy.eventing.override.sources=" + settings.mockedEventing.toIncomingFlowConfig());
      }
      if (settings.mockedEventing.hasOutgoingConfig()) {
        javaOptions.add("-Dkalix.proxy.eventing.override.destinations=" + settings.mockedEventing.toOutgoingFlowConfig());
      }
      settings.servicePortMappings.forEach((serviceName, hostPort) -> {
        javaOptions.add("-Dkalix.dev-mode.service-port-mappings." + serviceName + "=" + hostPort);
      });

      log.debug("Running container with javaOptions=" + javaOptions);
      runtimeContainer.addEnv("JAVA_TOOL_OPTIONS", String.join(" ", javaOptions));
      settings.workflowTickInterval.ifPresent(tickInterval -> runtimeContainer.addEnv("WORKFLOW_TICK_INTERVAL", tickInterval.toMillis() + ".millis"));

      runtimeContainer.start();

      proxyPort = runtimeContainer.getProxyPort();
      proxyHost = runtimeContainer.getHost();

    } else {
      proxyPort = 9000;
      proxyHost = "localhost";

      Http http = Http.get(testSystem);
      log.info("Checking kalix-runtime status");
      CompletionStage<String> checkingProxyStatus = Patterns.retry(() -> http.singleRequest(HttpRequest.GET("http://localhost:8558/ready")).thenCompose(response -> {
        int responseCode = response.status().intValue();
        if (responseCode == 200) {
          log.info("Kalix-runtime started");
          return CompletableFuture.completedStage("Ok");
        } else {
          log.info("Waiting for kalix-runtime, current response code is {}", responseCode);
          return CompletableFuture.failedFuture(new IllegalStateException("Kalix Runtime not started."));
        }
      }), 10, Duration.ofSeconds(1), testSystem);

      try {
        checkingProxyStatus.toCompletableFuture().get();
      } catch (InterruptedException | ExecutionException e) {
        log.error("Failed to connect to Kalix Runtime with:", e);
        throw new RuntimeException(e);
      }
    }
    // the proxy will announce its host and default port, but to communicate with it,
    // we need to use the port and host that testcontainers will expose
    // therefore, we set a port override in ProxyInfoHolder to allow for inter-component communication
    ProxyInfoHolder holder = ProxyInfoHolder.get(runner.system());
    holder.overridePort(proxyPort);
    holder.overrideProxyHost(proxyHost);
    holder.overrideTracingCollectorEndpoint(""); //emulating ProxyInfo with disabled tracing.
  }

  private int userServicePort(Boolean useTestContainers) {
    if (useTestContainers) {
      return availableLocalPort();
    } else {
      return KalixRuntimeContainer.DEFAULT_USER_SERVICE_PORT;
    }
  }

  /**
   * Get the host name/IP address where the Kalix service is available. This is relevant in certain
   * Continuous Integration environments.
   *
   * @return Kalix host
   */
  public String getHost() {
    if (!started)
      throw new IllegalStateException("Need to start KalixTestkit before accessing the host name");

    return proxyHost;
  }

  /**
   * Get the local port where the Kalix service is available.
   *
   * @return local Kalix port
   */
  public int getPort() {
    if (!started)
      throw new IllegalStateException("Need to start KalixTestkit before accessing the port");

    return proxyPort;
  }

  /**
   * Get an Akka gRPC client for the given service name. The same client instance is shared for the
   * test. The lifecycle of the client is managed by the SDK and it should not be stopped by user
   * code.
   *
   * @param <T>         The "service" interface generated for the service by Akka gRPC
   * @param clientClass The class of a gRPC service generated by Akka gRPC
   */
  public <T> T getGrpcClient(Class<T> clientClass) {
    return GrpcClients.get(getActorSystem()).getGrpcClient(clientClass, getHost(), getPort());
  }

  /**
   * Get an Akka gRPC client for the given service name, authenticating using the given principal.
   * The same client instance is shared for the test. The lifecycle of the client is managed by the
   * SDK and it should not be stopped by user code.
   *
   * @param <T>         The "service" interface generated for the service by Akka gRPC
   * @param clientClass The class of a gRPC service generated by Akka gRPC
   * @param principal   The principal to authenticate calls to the service as.
   */
  public <T> T getGrpcClientForPrincipal(Class<T> clientClass, Principal principal) {
    String serviceName = null;
    if (principal == Principal.SELF) {
      serviceName = settings.serviceName;
    } else if (principal instanceof Principal.LocalService) {
      serviceName = ((Principal.LocalService) principal).getName();
    }
    if (serviceName != null) {
      return GrpcClients.get(getActorSystem())
          .getGrpcClient(clientClass, getHost(), getPort(), serviceName);
    } else {
      return GrpcClients.get(getActorSystem()).getGrpcClient(clientClass, getHost(), getPort());
    }
  }

  /**
   * An Akka Stream materializer to use for running streams. Needed for example in a command handler
   * which accepts streaming elements but returns a single async reply once all streamed elements
   * has been consumed.
   */
  public Materializer getMaterializer() {
    return SystemMaterializer.get(getActorSystem()).materializer();
  }

  /**
   * Get an {@link ActorSystem} for creating Akka HTTP clients.
   *
   * @return test actor system
   */
  public ActorSystem getActorSystem() {
    if (!started)
      throw new IllegalStateException("Need to start KalixTestkit before accessing actor system");
    return testSystem;
  }



  /**
   * Get incoming messages for ValueEntity.
   *
   * @param typeId @TypeId or entity_type of the ValueEntity (depending on the used SDK)
   */
  public IncomingMessages getValueEntityIncomingMessages(String typeId) {
    if (!settings.mockedEventing.hasValueEntitySubscription(typeId)) {
      throwMissingConfigurationException("ValueEntity " + typeId);
    }
    return eventingTestKit.getValueEntityIncomingMessages(typeId);
  }

  /**
   * Get incoming messages for EventSourcedEntity.
   *
   * @param typeId @TypeId or entity_type of the EventSourcedEntity (depending on the used SDK)
   */
  public IncomingMessages getEventSourcedEntityIncomingMessages(String typeId) {
    if (!settings.mockedEventing.hasEventSourcedEntitySubscription(typeId)) {
      throwMissingConfigurationException("EventSourcedEntity " + typeId);
    }
    return eventingTestKit.getEventSourcedEntityIncomingMessages(typeId);
  }

  /**
   * Get incoming messages for Stream (eventing.in.direct in case of protobuf SDKs).
   *
   * @param service  service name
   * @param streamId service stream id
   */
  public IncomingMessages getStreamIncomingMessages(String service, String streamId) {
    if (!settings.mockedEventing.hasStreamSubscription(service, streamId)) {
      throwMissingConfigurationException("Stream " + service + "/" + streamId);
    }
    return eventingTestKit.getStreamIncomingMessages(service, streamId);
  }

  /**
   * Get incoming messages for Topic.
   *
   * @param topic topic name
   */
  public IncomingMessages getTopicIncomingMessages(String topic) {
    if (!settings.mockedEventing.hasTopicSubscription(topic)) {
      throwMissingConfigurationException("Topic " + topic);
    }
    return eventingTestKit.getTopicIncomingMessages(topic);
  }

  /**
   * Get mocked topic destination.
   *
   * @param topic topic name
   */
  public EventingTestKit.OutgoingMessages getTopicOutgoingMessages(String topic) {
    if (!settings.mockedEventing.hasTopicDestination(topic)) {
      throwMissingConfigurationException("Topic " + topic);
    }
    return eventingTestKit.getTopicOutgoingMessages(topic);
  }

  private void throwMissingConfigurationException(String hint) {
    throw new IllegalStateException("Currently configured mocked eventing is [" + settings.mockedEventing +
        "]. To use the MockedEventing API, to configure mocking of " + hint);
  }

  /**
   * Stop the testkit and local Kalix.
   */
  public void stop() {
    try {
      runtimeContainer.ifPresent(container -> container.stop());
    } catch (Exception e) {
      log.error("KalixTestkit proxy container failed to stop", e);
    }
    try {
      testSystem.terminate();
      testSystem
          .getWhenTerminated()
          .toCompletableFuture()
          .get(settings.stopTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("KalixTestkit ActorSystem failed to terminate", e);
    }
    try {
      runner
          .terminate()
          .toCompletableFuture()
          .get(settings.stopTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("KalixTestkit KalixRunner failed to terminate", e);
    }
    started = false;
  }

  /**
   * Get an available local port for testing.
   *
   * @return available local port
   */
  public static int availableLocalPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException("Couldn't get available local port", e);
    }
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  public MessageCodec getMessageCodec() {
    return messageCodec;
  }

  /**
   * INTERNAL API
   */
  @InternalApi
  public KalixRunner getRunner() {
    return runner;
  }

  /**
   * Returns {@link kalix.javasdk.testkit.EventingTestKit.MessageBuilder} utility
   * to create {@link kalix.javasdk.testkit.EventingTestKit.Message}s for the eventing testkit.
   */
  public EventingTestKit.MessageBuilder getMessageBuilder() {
    return messageBuilder;
  }
}
