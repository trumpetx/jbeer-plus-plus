package com.trumpetx.beer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

class LogConfigurer {
  private static final String PROGRAM_LOGGER_NAME = "com.trumpetx.beer";

  private LogConfigurer() {
  }

  static void setLoggerLevel(String loggerName, String level) {
    if (isNotBlank(level)) {
      Logger root = (Logger) LoggerFactory.getLogger(loggerName);
      root.setLevel(Level.toLevel(level));
    }
  }

  static void setProgramLogging(String rootLevel, String level) {
    setLoggerLevel(Logger.ROOT_LOGGER_NAME, rootLevel);
    setLoggerLevel(PROGRAM_LOGGER_NAME, level);
  }
}
