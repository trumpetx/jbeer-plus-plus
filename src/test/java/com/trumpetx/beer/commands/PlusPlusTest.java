package com.trumpetx.beer.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PlusPlusTest {

  @Mock private DaoProvider daoProvider;
  @Mock private GuildInitializer guildInitializer;
  @Mock private ChatInputInteractionEvent event;
  @Mock private Member sender;
  @Mock private User targetUser;
  @Mock private Item item;
  @Mock private Server server;
  @Mock private ApplicationCommandInteractionOption option;
  @Mock private ApplicationCommandInteractionOptionValue optionValue;
  @Mock private InteractionApplicationCommandCallbackReplyMono replyMono;

  private PlusPlus plusPlus;
  private Snowflake guildId;

  @BeforeEach
  void setUp() {
    plusPlus = new PlusPlus(daoProvider, guildInitializer);
    guildId = Snowflake.of(123456789L);
    lenient().when(event.reply(anyString())).thenReturn(replyMono);
  }

  @Test
  void handleItem_NoOptions_ReturnsShareMessage() {
    // Given
    when(event.getOptions()).thenReturn(Collections.emptyList());
    when(sender.getMention()).thenReturn("<@123>");
    when(item.getEmoji()).thenReturn(":beer:");

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        plusPlus.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("share"));
              verify(event).reply(contains("<@123>"));
              verify(event).reply(contains(":beer:"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_ValidUser_ProcessesSuccessfully() {
    // Given
    when(event.getOptions()).thenReturn(Collections.singletonList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.getId()).thenReturn(Snowflake.of(987654321L));
    when(sender.getId()).thenReturn(Snowflake.of(123456789L));
    when(targetUser.isBot()).thenReturn(false);
    when(item.isSelfIncrement()).thenReturn(false);
    when(item.getEmoji()).thenReturn(":beer:");
    when(sender.getMention()).thenReturn("<@123>");

    when(guildInitializer.toMembersString(
            eq(Collections.singletonList(targetUser)), eq(guildId), eq(item), any()))
        .thenReturn(Mono.just("Success message"));

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        plusPlus.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(guildInitializer)
                  .toMembersString(
                      eq(Collections.singletonList(targetUser)), eq(guildId), eq(item), any());
            })
        .verifyComplete();
  }

  @Test
  void handleItem_SelfIncrement_AllowsSelfTarget() {
    // Given
    when(event.getOptions()).thenReturn(Collections.singletonList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.isBot()).thenReturn(false);
    when(item.isSelfIncrement()).thenReturn(true);
    when(item.getEmoji()).thenReturn(":beer:");
    when(sender.getMention()).thenReturn("<@123>");

    when(guildInitializer.toMembersString(
            eq(Collections.singletonList(targetUser)), eq(guildId), eq(item), any()))
        .thenReturn(Mono.just("Success message"));

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        plusPlus.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(guildInitializer)
                  .toMembersString(
                      eq(Collections.singletonList(targetUser)), eq(guildId), eq(item), any());
            })
        .verifyComplete();
  }

  @Test
  void handleItem_SelfTargetWithoutSelfIncrement_ReturnsGreedyMessage() {
    // Given
    when(event.getOptions()).thenReturn(Collections.singletonList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.getId()).thenReturn(Snowflake.of(123456789L));
    when(sender.getId()).thenReturn(Snowflake.of(123456789L));
    when(item.isSelfIncrement()).thenReturn(false);
    when(item.getEmoji()).thenReturn(":beer:");
    when(sender.getMention()).thenReturn("<@123>");

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        plusPlus.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("greedy"));
              verify(event).reply(contains("<@123>"));
              verify(event).reply(contains(":beer:"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_BotTarget_ReturnsBotMessage() {
    // Given
    when(event.getOptions()).thenReturn(Collections.singletonList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.getId()).thenReturn(Snowflake.of(987654321L));
    when(sender.getId()).thenReturn(Snowflake.of(123456789L));
    when(targetUser.isBot()).thenReturn(true);
    when(item.getEmoji()).thenReturn(":beer:");

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        plusPlus.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("Bots don't like"));
              verify(event).reply(contains(":beer:"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_NoUserOption_ReturnsInvalidUserMessage() {
    // Given
    when(event.getOptions()).thenReturn(Collections.singletonList(option));
    when(option.getValue()).thenReturn(Optional.empty());

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        plusPlus.handleItem(event, guildId, sender, item);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Invalid user specified");
            })
        .verifyComplete();
  }

  @Test
  void keyword_ReturnsCorrectKeyword() {
    // When
    String keyword = plusPlus.keyword();

    // Then
    assertEquals("plusplus", keyword);
  }
}
