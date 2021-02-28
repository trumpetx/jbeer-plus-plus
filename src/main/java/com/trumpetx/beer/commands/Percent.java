package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.trumpetx.beer.TextProvider.getText;

class Percent extends AbstractCommand {
  Percent(DaoProvider daoProvider) {
    super("%%", daoProvider);
  }

  @Override
  Mono<Message> handleItem(String command, MessageCreateEvent event, User sender, Item item) {
    Guild guild = event.getGuild().block();
    List<MemberItem> memberItems = daoProvider.memberItemDao.queryForByItem(item);
    if (memberItems.isEmpty()) {
      return sendMessage(event, getText("reply.none", item.getEmojiPlural()));
    }
    StringBuilder sb = new StringBuilder();
    AtomicLong total = new AtomicLong();
    long topX = memberItems.stream()
      .filter(i -> i.getCount() > 0)
      .peek(i -> total.addAndGet(i.getCount()))
      .sorted(Comparator.comparing(MemberItem::getCount).reversed())
      .limit(20)
      .peek(memberItem -> sb.append(getText("top.member", guild.getMemberById(Snowflake.of(memberItem.getMember().getId())).block().getDisplayName(), getPercentString(memberItem, total.get()))))
      .count();
    return sendMessage(event, getText("top.header", topX, item.getEmojiPlural(), guild.getName()) + sb.toString());
  }

  private String getPercentString(MemberItem memberItem, long total) {
    double percent = (double) memberItem.getCount() / (double) total;
    return getText("top.percent", Math.round(percent * 100), memberItem.getItem().getEmojiPlural());
  }
}
