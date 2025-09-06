package com.trumpetx.beer.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
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
class PercentTest {

  private DaoProvider daoProvider;
  @Mock private ChatInputInteractionEvent event;
  @Mock private Member sender;
  @Mock private Guild guild;
  @Mock private InteractionApplicationCommandCallbackReplyMono replyMono;
  @Mock private discord4j.core.object.entity.Member discordMember1;
  @Mock private discord4j.core.object.entity.Member discordMember2;

  private Percent percent;
  private Snowflake guildId;

  @BeforeEach
  void setUp() {
    guildId = Snowflake.of(123456789L);
    lenient().when(event.reply(anyString())).thenReturn(replyMono);

    // Initialize real DaoProvider for testing
    daoProvider = DaoProvider.initializeInstance("jdbc:h2:mem:test");

    percent = new Percent(daoProvider);
  }

  @Test
  void handleItem_NoMemberItems_ReturnsNoneMessage() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.just(guild));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456800L);
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
        percent.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("No one has any"));
              verify(event).reply(contains(":beers:"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_WithMemberItems_ReturnsPercentages() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.just(guild));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456801L);
    testServer.setName("Test Guild 2");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // Create real members and member items
    com.trumpetx.beer.domain.Member member1 = new com.trumpetx.beer.domain.Member();
    member1.setId(40L);
    daoProvider.memberDao.create(member1);

    com.trumpetx.beer.domain.Member member2 = new com.trumpetx.beer.domain.Member();
    member2.setId(50L);
    daoProvider.memberDao.create(member2);

    MemberItem memberItem1 = new MemberItem();
    memberItem1.setMember(member1);
    memberItem1.setItem(testItem);
    memberItem1.setCount(60L);
    daoProvider.memberItemDao.create(memberItem1);

    MemberItem memberItem2 = new MemberItem();
    memberItem2.setMember(member2);
    memberItem2.setItem(testItem);
    memberItem2.setCount(40L);
    daoProvider.memberItemDao.create(memberItem2);

    // No need to mock guild member lookups since we're using mentions directly

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        percent.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("<@!40>"));
              verify(event).reply(contains("<@!50>"));
              verify(event).reply(contains("60%"));
              verify(event).reply(contains("40%"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_GuildNotFound_ReturnsError() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.empty());

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456802L);
    testServer.setName("Test Guild 3");
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
        percent.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply("Error: Guild not found");
            })
        .verifyComplete();
  }

  @Test
  void handleItem_MemberLookupFails_UsesMention() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.just(guild));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456803L);
    testServer.setName("Test Guild 4");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // Create real member and member item
    com.trumpetx.beer.domain.Member member1 = new com.trumpetx.beer.domain.Member();
    member1.setId(60L);
    daoProvider.memberDao.create(member1);

    MemberItem memberItem1 = new MemberItem();
    memberItem1.setMember(member1);
    memberItem1.setItem(testItem);
    memberItem1.setCount(100L);
    daoProvider.memberItemDao.create(memberItem1);

    // No need to mock guild member lookup since we're using mentions directly

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        percent.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("<@!60>"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_EqualPercentages_HandlesCorrectly() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.just(guild));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456804L);
    testServer.setName("Test Guild 5");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    // Create real members and member items
    com.trumpetx.beer.domain.Member member1 = new com.trumpetx.beer.domain.Member();
    member1.setId(70L);
    daoProvider.memberDao.create(member1);

    com.trumpetx.beer.domain.Member member2 = new com.trumpetx.beer.domain.Member();
    member2.setId(80L);
    daoProvider.memberDao.create(member2);

    MemberItem memberItem1 = new MemberItem();
    memberItem1.setMember(member1);
    memberItem1.setItem(testItem);
    memberItem1.setCount(50L);
    daoProvider.memberItemDao.create(memberItem1);

    MemberItem memberItem2 = new MemberItem();
    memberItem2.setMember(member2);
    memberItem2.setItem(testItem);
    memberItem2.setCount(50L);
    daoProvider.memberItemDao.create(memberItem2);

    // No need to mock guild member lookups since we're using mentions directly

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        percent.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("<@!70>"));
              verify(event).reply(contains("<@!80>"));
              verify(event).reply(contains("50%"));
            })
        .verifyComplete();
  }

  @Test
  void keyword_ReturnsCorrectKeyword() {
    // When
    String keyword = percent.keyword();

    // Then
    assertEquals("percent", keyword);
  }
}
