package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Member;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.trumpetx.beer.TextProvider.getText;

class PlusPlus extends AbstractCommand {
  PlusPlus(DaoProvider daoProvider) {
    super("++", daoProvider);
  }

  @Override
  Mono<?> handleItem(String command, MessageCreateEvent event, User sender, Item item) {
    log.debug("{} command received, sender={}, item={}", command, sender.getUsername(), item);
    List<User> userMentions = userMentions(event);
    if (!item.isSelfIncrement()) {
      userMentions.removeIf(u -> sender.getId().equals(u.getId()));
    }
    Member senderMember = toMember(sender);
    if (userMentions.isEmpty()) {
      return sendMessage(event, getText("reply.++share", senderMember.toMention(), item.getEmoji()));
    }
    AtomicLong count = new AtomicLong();
    String toMembersString = toMembers(userMentions).peek(toMember -> {
      MemberItem memberItem = daoProvider.memberItemDao.queryForByMemberAndItem(toMember, item);
      daoProvider.memberItemDao.update(memberItem.incrementCount());
      count.set(memberItem.getCount());
    }).map(Member::toMention).collect(Collectors.joining(", "));
    String optionalCount = userMentions.size() == 1 ? (" [ " + count + " ]") : "";
    return sendMessage(event, getText("reply.++", senderMember.toMention(), item.getEmoji(), toMembersString) + optionalCount);
  }
}
