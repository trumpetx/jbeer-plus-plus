package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

public class Help extends AbstractCommand {
  Help(DaoProvider daoProvider) {
    super("??", daoProvider);
  }

  @Override
  Mono<?> handleItem(String command, MessageCreateEvent event, Snowflake guildId, User sender, Item item) {
    Mono<?> helpMessage = sendMessage(event, "How to Beer-Plus-Plus:```" +
      "beer++ @Someone @SomeoneElse\tGive beer to someone!\n" +
      "beer-- @Someone @SomeoneElse\tTake beer from someone!\n" +
      "beer##                      \tWho has how many beers\n" +
      "beer%%                      \tWho has how much beer\n" +
      "beer??                      \tThis message\n" +
      "```\n" +
      "Beer++ is undergoing some changes - it's been entirely rewritten March 1, 2021 - new features coming soon: CUSTOMIZED items!\n" +
      "Feedback: <https://discordbots.org/bot/405811389748346881>", sentMessage -> {
      Server server = daoProvider.serverDao.queryForSameId(item.getServer());
      server.setLastHelpMessage(sentMessage.getId().asLong());
      daoProvider.serverDao.update(server);
    });
    Long lastHelpMessage = item.getServer().getLastHelpMessage();
    if (lastHelpMessage != null) {
      return deleteMessageOfChannel(event, lastHelpMessage).then(helpMessage);
    }
    return helpMessage;
  }
}
