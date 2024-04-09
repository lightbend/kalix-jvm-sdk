/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

/**
 * Interface used to represent HTTP status code. **NOT** for user extension.
 */
public interface StatusCode {

  // return the value of the status code
  int value();

  enum Success implements StatusCode {
    OK(200),
    CREATED(201),
    ACCEPTED(202),
    NON_AUTHORITATIVE_INFORMATION(203),
    NO_CONTENT(204),
    RESET_CONTENT(205),
    PARTIAL_CONTENT(206),
    MULTI_STATUS(207),
    ALREADY_REPORTED(208),
    IM_USED(226);

    private final int value;

    Success(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    public static Success from(int statusCode) {
      for (Success s : Success.values()) {
        if (s.value() == statusCode) {
          return s;
        }
      }
      throw new IllegalArgumentException("No matching status for [" + statusCode + "]");
    }
  }

  enum Redirect implements StatusCode {
    MULTIPLE_CHOICES(300),
    MOVED_PERMANENTLY(301),
    FOUND(302),
    SEE_OTHER(303),
    NOT_MODIFIED(304),
    USE_PROXY(305),
    TEMPORARY_REDIRECT(307),
    PERMANENT_REDIRECT(308);

    private final int value;

    Redirect(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }
  }


  /** The supported HTTP error codes that can be used when replying from the Kalix user function. */
  enum ErrorCode implements StatusCode {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    CONFLICT(409),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);

    private final int value;

    ErrorCode(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }
  }
}
