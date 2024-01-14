package com.cmhteixeira.java.http;

import java.util.Objects;

public sealed interface HttpVerb
    permits HttpVerb.GET,
        HttpVerb.POST,
        HttpVerb.PUT,
        HttpVerb.DELETE,
        HttpVerb.HEAD,
        HttpVerb.OPTIONS,
        HttpVerb.TRACE,
        HttpVerb.PATCH,
        HttpVerb.CONNECT {
  static HttpVerb fromString(String input) {
    if (Objects.equals(input, "GET")) return new GET();
    if (Objects.equals(input, "POST")) return new POST();
    if (Objects.equals(input, "PUT")) return new PUT();
    if (Objects.equals(input, "DELETE")) return new DELETE();
    if (Objects.equals(input, "HEAD")) return new HEAD();
    if (Objects.equals(input, "OPTIONS")) return new OPTIONS();
    if (Objects.equals(input, "TRACE")) return new TRACE();
    if (Objects.equals(input, "PATCH")) return new PATCH();
    if (Objects.equals(input, "CONNECT")) return new CONNECT();
    else
      throw new IllegalArgumentException(
          String.format("The input '%s' is not a valid HTTP verb", input));
  }

  record GET() implements HttpVerb {
    @Override
    public String toString() {
      return "GET";
    }
  }

  record POST() implements HttpVerb {
    @Override
    public String toString() {
      return "POST";
    }
  }

  record PUT() implements HttpVerb {
    @Override
    public String toString() {
      return "PUT";
    }
  }

  record DELETE() implements HttpVerb {
    @Override
    public String toString() {
      return "DELETE";
    }
  }

  record HEAD() implements HttpVerb {
    @Override
    public String toString() {
      return "HEAD";
    }
  }

  record OPTIONS() implements HttpVerb {
    @Override
    public String toString() {
      return "OPTIONS";
    }
  }

  record TRACE() implements HttpVerb {
    @Override
    public String toString() {
      return "TRACE";
    }
  }

  record PATCH() implements HttpVerb {
    @Override
    public String toString() {
      return "PATCH";
    }
  }

  record CONNECT() implements HttpVerb {
    @Override
    public String toString() {
      return "CONNECT";
    }
  }
}
