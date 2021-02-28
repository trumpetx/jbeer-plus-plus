package com.trumpetx.beer.commands;

import com.j256.ormlite.dao.ForeignCollection;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Member;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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

  abstract Mono<?> handleItem(String command, MessageCreateEvent event, User sender, Item item);

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
        return handleItem(command, event, sender, item);
      })
      .orElseGet(Mono::empty))
      .orElseGet(Mono::empty);
  }

  List<User> userMentions(MessageCreateEvent event) {
    return ofNullable(event.getMessage().getUserMentions().collectList().block()).orElseGet(Collections::emptyList);
  }

  Stream<Member> toMembers(Collection<User> users) {
    return users == null ? Stream.empty() : users.stream().map(this::toMember);
  }

  Member toMember(User users) {
    return daoProvider.memberDao.createIfNotExists(new Member(users.getId().asLong()));
  }

  Mono<Message> sendMessage(MessageCreateEvent event, String msg) {
    log.debug("Sending Message: {}", msg);
    event.getMessage().delete().delaySubscription(Duration.ofSeconds(1)).subscribe();
    return event.getMessage().getChannel().block().createMessage(msg);
  }
}
