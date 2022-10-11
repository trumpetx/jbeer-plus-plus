package com.trumpetx.beer;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultString;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class TextProvider {
  private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("i18n/lang");

  private TextProvider() {}

  public static String getText(String key, Object... args) {
    String textResource = defaultString(resourceBundle.getString(key), key);
    if (isNotEmpty(args)) {
      return MessageFormat.format(textResource, args);
    }
    return textResource;
  }
}
