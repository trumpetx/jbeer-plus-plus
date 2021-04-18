package com.trumpetx.beer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class ServerConversion {
  static final Map<String, Map<String, Integer>> OLD_DATA;

  static {
    try {
      OLD_DATA = new ObjectMapper().readValue(ServerConversion.class.getResourceAsStream("/db.json"), new TypeReference<Map<String, Map<String, Integer>>>() {
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
