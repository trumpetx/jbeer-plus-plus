package com.trumpetx.beer.commands;

import static com.trumpetx.beer.TextProvider.getText;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import java.util.Collections;
import java.util.List;
import reactor.core.publisher.Mono;

class MinusMinus extends AbstractCommand {
  private final GuildInitializer guildInitializer;

  MinusMinus(DaoProvider daoProvider, GuildInitializer guildInitializer) {
    super("minusminus", "Take beer from someone", daoProvider);
    this.guildInitializer = guildInitializer;
  }

  @Override
  public Possible<List<ApplicationCommandOptionData>> options() {
    return Possible.of(
        Collections.singletonList(
            ApplicationCommandOptionData.builder()
                .name("member")
                .description("Who do you want to take beer from?")
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(true)
                .build()));
  }

  @Override
  Mono<InteractionApplicationCommandCallbackReplyMono> handleItem(
      ChatInputInteractionEvent event,
      Snowflake guildId,
      discord4j.core.object.entity.Member sender,
      Item item) {
    log.debug("{} command received, sender={}, item={}", keyword(), sender.getUsername(), item);
    return event
        .getOptions()
        .iterator()
        .next()
        .getValue()
        .map(ApplicationCommandInteractionOptionValue::asUser)
        .orElse(Mono.empty())
        .flatMap(
            userMention -> {
              if (!item.isSelfDecrement()) {
                if (sender.getId().equals(userMention.getId())) {
                  return Mono.just(
                      event.reply(getText("reply.--share", item.getEmoji(), sender.getMention())));
                }
              }
              if (userMention.isBot()) {
                return Mono.just(event.reply(getText("reply.dontmess", item.getEmoji())));
              }

              return guildInitializer
                  .toMembersString(
                      Collections.singletonList(sender), guildId, item, MemberItem::decrementCount)
                  .map(
                      toMembersString ->
                          event.reply(
                              getText(
                                  "reply.--",
                                  sender.getMention(),
                                  item.getEmoji(),
                                  toMembersString)))
                  .switchIfEmpty(Mono.just(event.reply("Error processing member")));
            })
        .switchIfEmpty(Mono.just(event.reply("Invalid user specified")));
  }
}
