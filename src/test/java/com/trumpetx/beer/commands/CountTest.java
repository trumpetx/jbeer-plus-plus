package com.trumpetx.beer.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.*;
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
class CountTest {

  private DaoProvider daoProvider;
  @Mock private GuildInitializer guildInitializer;
  @Mock private ChatInputInteractionEvent event;
  @Mock private Member sender;
  @Mock private Guild guild;
  @Mock private InteractionApplicationCommandCallbackReplyMono replyMono;
  @Mock private discord4j.core.object.entity.Member discordMember1;
  @Mock private discord4j.core.object.entity.Member discordMember2;

  private Count count;
  private Snowflake guildId;

  @BeforeEach
  void setUp() {
    guildId = Snowflake.of(123456789L);
    lenient().when(event.reply(anyString())).thenReturn(replyMono);

    // Initialize real DaoProvider for testing
    daoProvider = DaoProvider.initializeInstance("jdbc:h2:mem:test");

    count = new Count(daoProvider);
  }

  @Test
  void handleItem_NoMemberItems_ReturnsNoneMessage() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.just(guild));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456789L);
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
        count.handleItem(event, guildId, sender, testItem);

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
  void handleItem_WithMemberItems_ReturnsLeaderboard() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.just(guild));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456790L);
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
    member1.setId(10L);
    daoProvider.memberDao.create(member1);

    com.trumpetx.beer.domain.Member member2 = new com.trumpetx.beer.domain.Member();
    member2.setId(20L);
    daoProvider.memberDao.create(member2);

    MemberItem memberItem1 = new MemberItem();
    memberItem1.setMember(member1);
    memberItem1.setItem(testItem);
    memberItem1.setCount(10L);
    daoProvider.memberItemDao.create(memberItem1);

    MemberItem memberItem2 = new MemberItem();
    memberItem2.setMember(member2);
    memberItem2.setItem(testItem);
    memberItem2.setCount(5L);
    daoProvider.memberItemDao.create(memberItem2);

    // Setup guild members (Discord4J Member type)
    when(guild.getMemberById(Snowflake.of(10L))).thenReturn(Mono.just(discordMember1));
    when(guild.getMemberById(Snowflake.of(20L))).thenReturn(Mono.just(discordMember2));
    when(discordMember1.getDisplayName()).thenReturn("User1");
    when(discordMember2.getDisplayName()).thenReturn("User2");

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        count.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("User1"));
              verify(event).reply(contains("User2"));
              verify(event).reply(contains("10"));
              verify(event).reply(contains("5"));
            })
        .verifyComplete();
  }

  @Test
  void handleItem_GuildNotFound_ReturnsError() {
    // Given
    when(sender.getGuild()).thenReturn(Mono.empty());

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456791L);
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
        count.handleItem(event, guildId, sender, testItem);

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
    testServer.setId(123456792L);
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
    member1.setId(30L);
    daoProvider.memberDao.create(member1);

    MemberItem memberItem1 = new MemberItem();
    memberItem1.setMember(member1);
    memberItem1.setItem(testItem);
    memberItem1.setCount(5L);
    daoProvider.memberItemDao.create(memberItem1);

    // Guild member lookup fails
    when(guild.getMemberById(Snowflake.of(30L)))
        .thenReturn(Mono.error(new RuntimeException("Member not found")));

    // When
    Mono<InteractionApplicationCommandCallbackReplyMono> result =
        count.handleItem(event, guildId, sender, testItem);

    // Then
    StepVerifier.create(result)
        .assertNext(
            reply -> {
              verify(event).reply(contains("<@!30>"));
            })
        .verifyComplete();
  }

  @Test
  void keyword_ReturnsCorrectKeyword() {
    // When
    String keyword = count.keyword();

    // Then
    assertEquals("count", keyword);
  }
}
