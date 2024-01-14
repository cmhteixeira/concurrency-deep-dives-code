package com.cmhteixeira.java.http;

public record RequestLine(HttpVerb verb, String path, HttpVersion version) {}
