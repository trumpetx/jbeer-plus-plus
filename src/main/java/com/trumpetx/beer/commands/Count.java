package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

import static com.trumpetx.beer.TextProvider.getText;

class Count extends AbstractCommand {
  Count(DaoProvider daoProvider) {
    super("##", daoProvider);
  }

  @Override
  Mono<?> handleItem(String command, MessageCreateEvent event, Snowflake guildId, User sender, Item item) {
    Guild guild = event.getGuild().block();
    if(guild == null){
      log.error("Error {}.getGuild()", getClass().getSimpleName());
      return Mono.empty();
    }
    List<MemberItem> memberItems = daoProvider.memberItemDao.queryForByItem(item);
    if (memberItems.isEmpty()) {
      return sendMessage(event, getText("reply.none", item.getEmojiPlural()));
    }
    StringBuilder sb = new StringBuilder();
    long topX = memberItems.stream()
      .filter(i -> i.getCount() > 0)
      .sorted(Comparator.comparing(MemberItem::getCount).reversed())
      .limit(20)
      .peek(memberItem -> sb.append(getText("top.member", displayNameOrMention(guild, memberItem), getCountString(memberItem))))
      .count();

    Mono<?> message = sendMessage(event, getText("top.header", topX, item.getEmojiPlural(), guild.getName()) + sb.toString(), sentMessage -> {
      Server server = daoProvider.serverDao.queryForSameId(item.getServer());
      server.setLastCountMessage(sentMessage.getId().asLong());
      daoProvider.serverDao.update(server);
    });
    Long lastCountMessage = item.getServer().getLastCountMessage();
    if(lastCountMessage != null){
      return deleteMessageOfChannel(event, lastCountMessage).then(message);
    }
    return message;
  }

  private String getCountString(MemberItem memberItem) {
    return memberItem.getCount() + " " + (memberItem.getCount() > 1 ? memberItem.getItem().getEmojiPlural() : memberItem.getItem().getEmoji());
  }
}
