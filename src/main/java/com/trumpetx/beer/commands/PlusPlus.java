package com.trumpetx.beer.commands;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.User;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static com.trumpetx.beer.TextProvider.getText;

class PlusPlus extends AbstractCommand {
  private final GuildInitializer guildInitializer;

  PlusPlus(DaoProvider daoProvider, GuildInitializer guildInitializer) {
    super("plusplus", "Give beer to someone", daoProvider);
    this.guildInitializer = guildInitializer;
  }

  @Override
  public Possible<List<ApplicationCommandOptionData>> options() {
    return Possible.of(Collections.singletonList(
      ApplicationCommandOptionData.builder()
        .name("member")
        .description("Who do you want give beer to?")
        .type(ApplicationCommandOption.Type.USER.getValue())
        .required(true)
        .build()
    ));
  }

  @Override
  InteractionApplicationCommandCallbackReplyMono handleItem(ChatInputInteractionEvent event, Snowflake guildId, discord4j.core.object.entity.Member sender, Item item) {
    log.debug("{} command received, sender={}, item={}", keyword(), sender.getUsername(), item);
    if(event.getOptions().isEmpty( )) {
      return event.reply(getText("reply.++share", sender.getMention(), item.getEmoji()));
    }
    User userMention = event.getOptions().iterator().next().getValue()
      .map(ApplicationCommandInteractionOptionValue::asUser)
      .map(Mono::block).orElseThrow(RuntimeException::new);
    if (!item.isSelfIncrement()) {
      if (sender.getId().equals(userMention.getId())) {
        return event.reply(getText("reply.greedy", sender.getMention(), item.getEmoji()));
      }
    }
    if (userMention.isBot()) {
      return event.reply(getText("reply.botslike", item.getEmoji(), ":beer:".equals(item.getEmoji()) ? ":wine_glass:" : ":beer:"));
    }
    String toMembersString = guildInitializer.toMembersString(Collections.singletonList(userMention), guildId, item, MemberItem::incrementCount);
    return event.reply(getText("reply.++", sender.getMention(), item.getEmoji(), toMembersString));
  }
}
