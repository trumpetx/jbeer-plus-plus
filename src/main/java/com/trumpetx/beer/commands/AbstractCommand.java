package com.trumpetx.beer.commands;

import com.j256.ormlite.dao.ForeignCollection;
import com.trumpetx.beer.domain.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

abstract class AbstractCommand implements Command {
  final String keyword;
  final DaoProvider daoProvider;
  final Logger log = LoggerFactory.getLogger(getClass());

  AbstractCommand(String keyword, DaoProvider daoProvider) {
    this.keyword = keyword;
    this.daoProvider = daoProvider;
  }

  @Override
  public String keyword() {
    return keyword;
  }

  abstract Mono<?> handleItem(String command, MessageCreateEvent event, Snowflake guildId, User sender, Item item);

  @Override
  public Mono<?> execute(String command, MessageCreateEvent event) {
    return event.getMember().map(sender -> event
      .getGuildId()
      .map(Snowflake::asLong)
      .map(daoProvider.serverDao::queryForId)
      .map(Server::getItems)
      .map(ForeignCollection::stream)
      .flatMap(itemStream -> itemStream.filter(i -> i.getKeyword().equals(command)).findFirst())
      .map(item -> {
        log.debug("{} command received, sender={}, item={}", command, sender.getUsername(), item);
        return handleItem(command, event, event.getGuildId().get(), sender, item);
      })
      .orElseGet(Mono::empty))
      .orElseGet(Mono::empty);
  }

  List<User> userMentions(MessageCreateEvent event) {
    return ofNullable(event.getMessage().getUserMentions().collectList().block()).orElseGet(Collections::emptyList);
  }

  Member toMember(User users) {
    return daoProvider.memberDao.createIfNotExists(new Member(users.getId().asLong()));
  }

  String displayNameOrMention(Guild guild, MemberItem memberItem) {
    return ofNullable(guild.getMemberById(Snowflake.of(memberItem.getMember().getId())).block())
      .map(discord4j.core.object.entity.Member::getDisplayName)
      .orElseGet(memberItem.getMember()::toMention);
  }

  Mono<?> deleteMessageOfChannel(MessageCreateEvent event, long messageId) {
    log.debug("Attempting to delete messageId={}", messageId);
    return event.getMessage().getChannel()
      .flatMap(channel -> channel.getMessageById(Snowflake.of(messageId)))
      .onErrorResume(e -> {
        log.warn("Error getting message from channel: {}/{}, {}", event.getMessage().getChannelId().asLong(), messageId, e.getMessage());
        return Mono.empty();
      })
      .flatMap(msg -> msg.delete().delaySubscription(Duration.ofSeconds(1)))
      .onErrorResume(e -> {
        log.warn("Error deleting message: {}, {}", messageId, e.getMessage());
        return Mono.empty();
      });
  }

  Mono<?> sendMessage(MessageCreateEvent event, String msg) {
    return sendMessage(event, msg, empty -> {
    });
  }

  Mono<?> sendMessage(MessageCreateEvent event, String msg, Consumer<Message> messageCallback) {
    log.debug("Sending Message: {}", msg);
    return event
      .getMessage()
      .getChannel()
      .flatMap(c -> c.createMessage(msg)
        .onErrorResume(e -> {
          log.error("Error creating message: {}", msg);
          return Mono.empty();
        })
        .map(message -> {
          messageCallback.accept(message);
          return message;
        })
      )
      .then(event.getMessage().delete().delaySubscription(Duration.ofSeconds(1)).onErrorResume(e -> {
        log.warn("Error deleting beer++ message from channel: {}, {}", event.getMessage().getChannelId().asLong(), e.getMessage());
        return Mono.empty();
      }));
  }
}
