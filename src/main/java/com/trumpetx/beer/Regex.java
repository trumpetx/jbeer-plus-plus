package com.trumpetx.beer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class Regex {
  private static final Map<String, Pattern> PATTERN_CACHE = new HashMap<>();

  private Regex() {}

  private static Pattern generateCommandPattern(String command) {
    return Pattern.compile("(\\w+)" + Pattern.quote(command) + ".*");
  }

  static Pattern command(String command) {
    return PATTERN_CACHE.computeIfAbsent(command, Regex::generateCommandPattern);
  }
}
