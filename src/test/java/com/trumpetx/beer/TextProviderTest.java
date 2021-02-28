package com.trumpetx.beer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextProviderTest {
  @Test
  void test_getText_arg(){
    assertEquals("Share :beer: with @Someone please!", TextProvider.getText("reply.share", ":beer:"));
  }
}
