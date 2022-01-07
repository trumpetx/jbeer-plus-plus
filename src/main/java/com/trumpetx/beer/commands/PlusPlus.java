package com.trumpetx.beer.commands;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Member;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.trumpetx.beer.TextProvider.getText;

class PlusPlus extends AbstractCommand {
  private final GuildInitializer guildInitializer;

  PlusPlus(DaoProvider daoProvider, GuildInitializer guildInitializer) {
    super("++", daoProvider);
    this.guildInitializer = guildInitializer;
  }

  @Override
  Mono<?> handleItem(String command, MessageCreateEvent event, Snowflake guildId, User sender, Item item) {
    log.debug("{} command received, sender={}, item={}", command, sender.getUsername(), item);
    List<User> userMentions = event.getMessage().getUserMentions();
    if (!item.isSelfIncrement()) {
      if (userMentions.removeIf(u -> sender.getId().equals(u.getId())) && userMentions.isEmpty()) {
        return sendMessage(event, getText("reply.greedy", sender.getMention(), item.getEmoji()));
      }
    }
    Member senderMember = toMember(sender);
    if (userMentions.isEmpty()) {
      return sendMessage(event, getText("reply.++share", senderMember.toMention(), item.getEmoji()));
    }
    if (userMentions.stream().anyMatch(User::isBot)) {
      return sendMessage(event, getText("reply.botslike", item.getEmoji(), ":beer:".equals(item.getEmoji()) ? ":wine_glass:" : ":beer:"));
    }
    String toMembersString = guildInitializer.toMembersString(userMentions, guildId, item, MemberItem::incrementCount);
    return sendMessage(event, getText("reply.++", senderMember.toMention(), item.getEmoji(), toMembersString));
  }
}
