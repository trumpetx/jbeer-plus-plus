package com.trumpetx.beer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RegexTest {
  @ParameterizedTest
  @CsvSource({
    "++,Beer++,Beer",
    "++,beer++,beer",
    "++,beer++ @SandyBeach,beer",
    "--,beer--,beer",
    "--,beer-- @SandyBeach,beer",
    "\\#\\#,wine\\#\\#,wine",
    "%%,wine%%,wine",
  })
  void test_group(String keyword, String input, String command) {
    Matcher m = Regex.command(keyword).matcher(input);
    assertTrue(m.matches());
    assertEquals(input, m.group(0));
    assertEquals(command, m.group(1));
  }
}
