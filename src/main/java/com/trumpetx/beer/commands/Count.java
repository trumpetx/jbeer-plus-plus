package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;

import java.util.Comparator;
import java.util.List;

import static com.trumpetx.beer.TextProvider.getText;

class Count extends AbstractCommand {
  Count(DaoProvider daoProvider) {
    super("count", "Who has how many beers", daoProvider);
  }

  @Override
  InteractionApplicationCommandCallbackReplyMono handleItem(ChatInputInteractionEvent event, Snowflake guildId, discord4j.core.object.entity.Member sender, Item item) {
    Guild guild = sender.getGuild().block();
    if(guild == null){
      log.error("Error {}.getGuild()", getClass().getSimpleName());
      return event.reply();
    }
    List<MemberItem> memberItems = daoProvider.memberItemDao.queryForByItem(item);
    if (memberItems.isEmpty()) {
      return event.reply(getText("reply.none", item.getEmojiPlural()));
    }
    StringBuilder sb = new StringBuilder();
    long topX = memberItems.stream()
      .filter(i -> i.getCount() > 0)
      .sorted(Comparator.comparing(MemberItem::getCount).reversed())
      .limit(20)
      .peek(memberItem -> sb.append(getText("top.member", displayNameOrMention(guild, memberItem), getCountString(memberItem))))
      .count();
    String reply = getText("top.header", topX, item.getEmojiPlural(), guild.getName()) + sb;
    if(reply.length() > 2000) {
      reply = reply.substring(0, 1900) + "\n\nReply too long, truncating for now (will fix this later).";
    }
    return event.reply(reply);
  }

  private String getCountString(MemberItem memberItem) {
    return memberItem.getCount() + " " + (memberItem.getCount() > 1 ? memberItem.getItem().getEmojiPlural() : memberItem.getItem().getEmoji());
  }
}
