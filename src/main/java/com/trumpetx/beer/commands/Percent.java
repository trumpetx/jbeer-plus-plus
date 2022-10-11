package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.trumpetx.beer.TextProvider.getText;

class Percent extends AbstractCommand {
  Percent(DaoProvider daoProvider) {
    super("percent", "Who has how much beer", daoProvider);
  }

  @Override
  InteractionApplicationCommandCallbackReplyMono handleItem(ChatInputInteractionEvent event, Snowflake guildId, discord4j.core.object.entity.Member sender, Item item) {
    Guild guild = sender.getGuild().block(Duration.ofSeconds(10));
    if(guild == null){
      log.error("Error {}.getGuild()", getClass().getSimpleName());
      return event.reply();
    }
    List<MemberItem> memberItems = daoProvider.memberItemDao.queryForByItem(item);
    if (memberItems.isEmpty()) {
      return event.reply(getText("reply.none", item.getEmojiPlural()));
    }
    StringBuilder sb = new StringBuilder();
    AtomicLong total = new AtomicLong();
    long topX = memberItems.stream()
      .filter(i -> i.getCount() > 0)
      .peek(i -> total.addAndGet(i.getCount()))
      .sorted(Comparator.comparing(MemberItem::getCount).reversed())
      .limit(20)
      .peek(memberItem -> sb.append(getText("top.member", displayNameOrMention(guild, memberItem), getPercentString(memberItem, total.get()))))
      .count();
    String reply = getText("top.header", topX, item.getEmojiPlural(), guild.getName()) + sb;
    if(reply.length() > 2000) {
      reply = reply.substring(0, 1900) + "\n\nReply too long, truncating (will fix this later).";
    }
    return event.reply(reply);
  }

  private String getPercentString(MemberItem memberItem, long total) {
    double percent = (double) memberItem.getCount() / (double) total;
    return getText("top.percent", Math.round(percent * 100), memberItem.getItem().getEmojiPlural());
  }
}
