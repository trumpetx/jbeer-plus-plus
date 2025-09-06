package com.trumpetx.beer.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class HelpTest {

  @Mock private DaoProvider daoProvider;
  @Mock private GuildInitializer guildInitializer;
  @Mock private ChatInputInteractionEvent event;
  @Mock private Member sender;
  @Mock private Item item;
  @Mock private InteractionApplicationCommandCallbackReplyMono replyMono;

  private Help help;
  private Snowflake guildId;

  @BeforeEach
  void setUp() {
    help = new Help(daoProvider);
    guildId = Snowflake.of(123456789L);
    lenient().when(event.reply(anyString())).thenReturn(replyMono);
  }

  @Test
  void handleItem_ReturnsHelpMessage() {
    // Given
    when(event.reply(anyString())).thenReturn(replyMono);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        help.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("How to Beer-Plus-Plus"));
              verify(event).reply(contains("/beerplusplus @Someone"));
              verify(event).reply(contains("/beerminusminus @Someone"));
              verify(event).reply(contains("/beercount"));
              verify(event).reply(contains("/beerpercent"));
              verify(event).reply(contains("/beerabout"));
              verify(event).reply(contains("Beer++ has been rewritten with slash commands"));
              verify(event)
                  .reply(contains("Feedback: <https://discordbots.org/bot/405811389748346881>"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_HelpMessageContainsAllCommands() {
    // Given
    when(event.reply(anyString())).thenReturn(replyMono);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        help.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              String expectedCommands =
                  "/beerplusplus @Someone\\n"
                      + "/beerminusminus @Someone\\n"
                      + "/beercount\\n"
                      + "/beerpercent\\n"
                      + "/beerabout";

              verify(event).reply(contains("/beerplusplus @Someone"));
              verify(event).reply(contains("/beerminusminus @Someone"));
              verify(event).reply(contains("/beercount"));
              verify(event).reply(contains("/beerpercent"));
              verify(event).reply(contains("/beerabout"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_HelpMessageContainsCodeBlock() {
    // Given
    when(event.reply(anyString())).thenReturn(replyMono);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        help.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("```"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_HelpMessageContainsEmojis() {
    // Given
    when(event.reply(anyString())).thenReturn(replyMono);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        help.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains(":beers:"));
            })
        .verifyComplete();
  }

  @Test
  void keyword_ReturnsCorrectKeyword() {
    // When
    String keyword = help.keyword();

    // Then
    assertEquals("about", keyword);
  }
}
