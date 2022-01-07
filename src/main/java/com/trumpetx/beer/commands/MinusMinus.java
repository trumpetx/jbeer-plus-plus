package com.trumpetx.beer.commands;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Member;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.trumpetx.beer.TextProvider.getText;

class MinusMinus extends AbstractCommand {
  private final GuildInitializer guildInitializer;

  MinusMinus(DaoProvider daoProvider, GuildInitializer guildInitializer) {
    super("--", daoProvider);
    this.guildInitializer = guildInitializer;
  }

  @Override
  Mono<?> handleItem(String command, MessageCreateEvent event, Snowflake guildId, User sender, Item item) {
    List<User> userMentions = event.getMessage().getUserMentions();
    if (!item.isSelfDecrement()) {
      userMentions.removeIf(u -> sender.getId().equals(u.getId()));
    }
    Member senderMember = toMember(sender);
    if (userMentions.isEmpty()) {
      return sendMessage(event, getText("reply.--share", item.getEmoji(), senderMember.toMention()));
    }
    if (userMentions.stream().anyMatch(User::isBot)) {
      return sendMessage(event, getText("reply.dontmess", item.getEmoji()));
    }
    String toMembersString = guildInitializer.toMembersString(userMentions, guildId, item, MemberItem::decrementCount);
    return sendMessage(event, getText("reply.--", senderMember.toMention(), item.getEmoji(), toMembersString));
  }
}
