package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;

public class Help extends AbstractCommand {
  Help(DaoProvider daoProvider) {
    super("about", "About this bot", daoProvider);
  }

  @Override
  InteractionApplicationCommandCallbackReplyMono handleItem(ChatInputInteractionEvent event, Snowflake guildId, discord4j.core.object.entity.Member sender, Item item) {
    return event.reply("How to Beer-Plus-Plus:```" +
      "/beerplusplus @Someone\n" +
      "/beerminusminus @Someone\n" +
      "/beercount\n" +
      "/beerpercent\n" +
      "/beerabout\n" +
      "```\n" +
      "Beer++ has been rewritten with slash commands!!  Some behaviors have changed, but the gist should remain the same.  Enjoy :beers:!\n" +
      "Feedback: <https://discordbots.org/bot/405811389748346881>");
  }
}
