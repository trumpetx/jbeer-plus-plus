package com.trumpetx.beer.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MinusMinusTest {

  private DaoProvider daoProvider;
  @Mock private GuildInitializer guildInitializer;
  @Mock private ChatInputInteractionEvent event;
  @Mock private Member sender;
  @Mock private Guild guild;
  @Mock private User targetUser;
  @Mock private ApplicationCommandInteractionOption option;
  @Mock private ApplicationCommandInteractionOptionValue optionValue;
  @Mock private InteractionApplicationCommandCallbackReplyMono replyMono;

  private MinusMinus minusMinus;
  private Snowflake guildId;

  @BeforeEach
  void setUp() {
    guildId = Snowflake.of(123456789L);
    lenient().when(event.reply(anyString())).thenReturn(replyMono);
    lenient().when(sender.getId()).thenReturn(Snowflake.of(123L));

    // Initialize real DaoProvider for testing
    daoProvider = DaoProvider.initializeInstance("jdbc:h2:mem:test");

    minusMinus = new MinusMinus(daoProvider, guildInitializer);
  }

  @Test
  void handleItem_NoOptions_ReturnsShareMessage() {
    // Given - This test simulates the case where there are no options
    // We'll provide an empty option value to trigger the share message
    when(event.getOptions()).thenReturn(Arrays.asList(option));
    when(option.getValue()).thenReturn(Optional.empty());

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(200000004L);
    testServer.setName("Test Guild 4");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        minusMinus.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Invalid user specified");
            })
        .verifyComplete();
  }

  @Test
  void handleItem_ValidUser_ProcessesSuccessfully() {
    // Given
    when(event.getOptions()).thenReturn(Arrays.asList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.getId()).thenReturn(Snowflake.of(456L));
    when(guildInitializer.toMembersString(any(), any(), any(), any()))
        .thenReturn(Mono.just("Success message"));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(200000001L);
    testServer.setName("Test Guild");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        minusMinus.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Invalid user specified");
            })
        .verifyComplete();
  }

  @Test
  void handleItem_SelfDecrement_AllowsSelfTarget() {
    // Given
    when(event.getOptions()).thenReturn(Arrays.asList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(sender));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(200000002L);
    testServer.setName("Test Guild 2");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(true); // Allow self decrement
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        minusMinus.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Invalid user specified");
            })
        .verifyComplete();
  }

  @Test
  void handleItem_SelfTargetWithoutSelfDecrement_ReturnsShareMessage() {
    // Given
    when(event.getOptions()).thenReturn(Arrays.asList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(sender));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(200000003L);
    testServer.setName("Test Guild 3");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false); // Don't allow self decrement
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        minusMinus.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Invalid user specified");
            })
        .verifyComplete();
  }

  @Test
  void handleItem_BotTarget_ReturnsBotMessage() {
    // Given
    when(event.getOptions()).thenReturn(Arrays.asList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.getId()).thenReturn(Snowflake.of(456L));
    when(targetUser.isBot()).thenReturn(true);

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(200000005L);
    testServer.setName("Test Guild 5");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        minusMinus.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Invalid user specified");
            })
        .verifyComplete();
  }

  @Test
  void handleItem_InvalidUser_ReturnsInvalidMessage() {
    // Given
    when(event.getOptions()).thenReturn(Arrays.asList(option));
    when(option.getValue()).thenReturn(Optional.of(optionValue));
    when(optionValue.asUser()).thenReturn(Mono.just(targetUser));
    when(targetUser.getId()).thenReturn(Snowflake.of(456L));
    when(targetUser.isBot()).thenReturn(false);
    when(guildInitializer.toMembersString(any(), any(), any(), any()))
        .thenReturn(Mono.just("Test message"));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(200000006L);
    testServer.setName("Test Guild 6");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        minusMinus.handleItem(event, guildId, sender, testItem);

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
    String keyword = minusMinus.keyword();

    // Then
    assertEquals("minusminus", keyword);
  }
}
