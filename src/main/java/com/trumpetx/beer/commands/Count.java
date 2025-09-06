package com.trumpetx.beer.commands;

import static com.trumpetx.beer.TextProvider.getText;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import java.util.Comparator;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class Count extends AbstractCommand {
  Count(DaoProvider daoProvider) {
    super("count", "Who has how many beers", daoProvider);
  }

  @Override
  Mono<InteractionApplicationCommandCallbackReplyMono> handleItem(
      ChatInputInteractionEvent event,
      Snowflake guildId,
      discord4j.core.object.entity.Member sender,
      Item item) {
    return sender
        .getGuild()
        .flatMap(
            guild -> {
              return processCountCommand(event, guild, item);
            })
        .switchIfEmpty(Mono.just(event.reply("Error: Guild not found")));
  }

  private Mono<InteractionApplicationCommandCallbackReplyMono> processCountCommand(
      ChatInputInteractionEvent event, Guild guild, Item item) {
    List<MemberItem> memberItems = daoProvider.memberItemDao.queryForByItem(item);
    if (memberItems.isEmpty()) {
      return Mono.just(event.reply(getText("reply.none", item.getEmojiPlural())));
    }

    // Process member items reactively
    return Flux.fromIterable(memberItems)
        .filter(i -> i.getCount() > 0)
        .sort(Comparator.comparing(MemberItem::getCount).reversed())
        .take(20)
        .flatMap(
            memberItem ->
                displayNameOrMention(guild, memberItem)
                    .map(
                        displayName ->
                            getText("top.member", displayName, getCountString(memberItem))))
        .collectList()
        .map(
            memberStrings -> {
              StringBuilder sb = new StringBuilder();
              memberStrings.forEach(sb::append);
              long topX = memberStrings.size();
              String reply =
                  getText("top.header", topX, item.getEmojiPlural(), guild.getName()) + sb;
              if (reply.length() > 2000) {
                reply =
                    reply.substring(0, 1900)
                        + "\n\nReply too long, truncating for now (will fix this later).";
              }
              return event.reply(reply);
            });
  }

  private String getCountString(MemberItem memberItem) {
    return memberItem.getCount()
        + " "
        + (memberItem.getCount() > 1
            ? memberItem.getItem().getEmojiPlural()
            : memberItem.getItem().getEmoji());
  }
}
