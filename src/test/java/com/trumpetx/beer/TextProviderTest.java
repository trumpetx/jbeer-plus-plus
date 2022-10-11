package com.trumpetx.beer;

import static com.trumpetx.beer.TextProvider.getText;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TextProviderTest {
  @Test
  void test_getText_arg() {
    assertEquals(
        "TrumpetX, share :beer: with @Someone please!",
        getText("reply.++share", "TrumpetX", ":beer:"));
    assertEquals("Don't mess with my :beer: buddy!", getText("reply.dontmess", ":beer:"));
  }
}
