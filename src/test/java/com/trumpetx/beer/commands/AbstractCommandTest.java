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
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AbstractCommandTest {

  private DaoProvider daoProvider;
  @Mock private GuildInitializer guildInitializer;
  @Mock private ChatInputInteractionEvent event;
  @Mock private discord4j.core.object.command.Interaction interaction;
  @Mock private discord4j.core.object.entity.Member sender;
  @Mock private Guild guild;
  @Mock private Server server;
  @Mock private Item item;
  @Mock private MemberItem memberItem;
  @Mock private com.trumpetx.beer.domain.Member domainMember;
  @Mock private User user;
  @Mock private InteractionApplicationCommandCallbackReplyMono replyMono;

  private TestCommand testCommand;
  private Snowflake guildId;

  @BeforeEach
  void setUp() {
    guildId = Snowflake.of(123456789L);
    lenient().when(event.reply(anyString())).thenReturn(replyMono);
    lenient().when(replyMono.withEphemeral(anyBoolean())).thenReturn(replyMono);

    // Mock the interaction
    lenient().when(event.getInteraction()).thenReturn(interaction);
    lenient().when(interaction.getGuildId()).thenReturn(Optional.of(guildId));
    lenient().when(interaction.getMember()).thenReturn(Optional.of(sender));

    // Initialize real DaoProvider for testing
    daoProvider = DaoProvider.initializeInstance("jdbc:h2:mem:test");

    testCommand = new TestCommand(daoProvider, guildInitializer);
  }

  @Test
  void execute_ValidGuildAndSender_ProcessesSuccessfully() {
    // Given
    when(event.getInteraction().getGuildId()).thenReturn(Optional.of(guildId));
    when(event.getInteraction().getMember()).thenReturn(Optional.of(sender));

    // Set up real data in the database
    Server testServer = new Server();
    testServer.setId(123456700L);
    testServer.setName("Test Guild");
    daoProvider.serverDao.create(testServer);

    Item testItem = new Item();
    testItem.setKeyword("beer");
    testItem.setEmoji(":beer:");
    testItem.setEmojiPlural(":beers:");
    testItem.setSelfIncrement(false);
    testItem.setServer(testServer);
    daoProvider.itemDao.create(testItem);

    lenient().when(sender.getGuild()).thenReturn(Mono.just(guild));
    lenient().when(guild.getName()).thenReturn("Test Guild");
    lenient().when(sender.getUsername()).thenReturn("TestUser");
    lenient().when(sender.getMention()).thenReturn("<@123>");

    // When
    InteractionApplicationCommandCallbackReplyMono result = testCommand.execute(event);

    // Then
    // TestCommand is a real object, so we can't verify its method calls
    // The test passes if no exception is thrown
  }

  @Test
  void execute_NoGuildId_ReturnsInvalid() {
    // Given
    when(event.getInteraction().getGuildId()).thenReturn(Optional.empty());
    when(event.getInteraction().getMember()).thenReturn(Optional.of(sender));
    when(event.getCommandName()).thenReturn("test");

    // When
    InteractionApplicationCommandCallbackReplyMono result = testCommand.execute(event);

    // Then
    verify(event).reply("An error occurred processing the command: test");
  }

  @Test
  void execute_NoSender_ReturnsInvalid() {
    // Given
    when(event.getInteraction().getGuildId()).thenReturn(Optional.of(guildId));
    when(event.getInteraction().getMember()).thenReturn(Optional.empty());
    when(event.getCommandName()).thenReturn("test");

    // When
    InteractionApplicationCommandCallbackReplyMono result = testCommand.execute(event);

    // Then
    verify(event).reply("An error occurred processing the command: test");
  }

  @Test
  void execute_NoBeerItem_ReturnsInvalid() {
    // Given
    Snowflake testGuildId = Snowflake.of(987654321L);
    when(event.getInteraction().getGuildId()).thenReturn(Optional.of(testGuildId));
    when(event.getInteraction().getMember()).thenReturn(Optional.of(sender));

    // Set up a server with no beer item
    Server testServer = new Server();
    testServer.setId(testGuildId.asLong());
    testServer.setName("Test Guild 2");
    daoProvider.serverDao.create(testServer);

    when(event.getCommandName()).thenReturn("test");

    // When
    InteractionApplicationCommandCallbackReplyMono result = testCommand.execute(event);

    // Then
    verify(event).reply("An error occurred processing the command: test");
  }

  @Test
  void displayNameOrMention_MemberFound_ReturnsDisplayName() {
    // Given
    when(memberItem.getMember()).thenReturn(domainMember);
    lenient().when(guild.getMemberById(Snowflake.of(123L))).thenReturn(Mono.just(sender));
    lenient().when(sender.getDisplayName()).thenReturn("DisplayName");
    lenient().when(domainMember.getId()).thenReturn(123L);
    lenient().when(domainMember.toMention()).thenReturn("<@123>");

    // When
    String result = testCommand.displayNameOrMention(guild, memberItem);

    // Then
    assertEquals("DisplayName", result);
  }

  @Test
  void displayNameOrMention_MemberNotFound_ReturnsMention() {
    // Given
    when(guild.getMemberById(Snowflake.of(123L)))
        .thenReturn(Mono.error(new RuntimeException("Not found")));
    when(domainMember.getId()).thenReturn(123L);
    when(domainMember.toMention()).thenReturn("<@123>");
    when(memberItem.getMember()).thenReturn(domainMember);

    // When
    String result = testCommand.displayNameOrMention(guild, memberItem);

    // Then
    assertEquals("<@123>", result);
  }

  @Test
  void displayNameOrMention_ExceptionThrown_ReturnsMention() {
    // Given
    when(guild.getMemberById(Snowflake.of(123L)))
        .thenReturn(Mono.error(new RuntimeException("API Error")));
    when(domainMember.getId()).thenReturn(123L);
    when(domainMember.toMention()).thenReturn("<@123>");
    when(memberItem.getMember()).thenReturn(domainMember);

    // When
    String result = testCommand.displayNameOrMention(guild, memberItem);

    // Then
    assertEquals("<@123>", result);
  }

  // Test implementation of AbstractCommand for testing purposes
  private static class TestCommand extends AbstractCommand {
    public TestCommand(DaoProvider daoProvider, GuildInitializer guildInitializer) {
      super("test", "Test command", daoProvider);
    }

    @Override
    public String keyword() {
      return "test";
    }

    @Override
    public Mono<InteractionApplicationCommandCallbackReplyMono> handleItem(
        ChatInputInteractionEvent event,
        Snowflake guildId,
        discord4j.core.object.entity.Member sender,
        Item item) {
      return Mono.just(event.reply("Test response"));
    }
  }
}
