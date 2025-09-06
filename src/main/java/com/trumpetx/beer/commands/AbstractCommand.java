package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

abstract class AbstractCommand implements Command {
  final String keyword;
  final String description;
  final DaoProvider daoProvider;
  final Logger log = LoggerFactory.getLogger(getClass());

  AbstractCommand(String keyword, String description, DaoProvider daoProvider) {
    this.keyword = keyword;
    this.description = description;
    this.daoProvider = daoProvider;
  }

  @Override
  public String keyword() {
    return keyword;
  }

  @Override
  public String description() {
    return description;
  }

  abstract Mono<InteractionApplicationCommandCallbackReplyMono> handleItem(
      ChatInputInteractionEvent event,
      Snowflake guildId,
      discord4j.core.object.entity.Member sender,
      Item item);

  @Override
  public Mono<InteractionApplicationCommandCallbackReplyMono> execute(
      ChatInputInteractionEvent event) {
    Optional<Snowflake> guildId = event.getInteraction().getGuildId();
    Optional<discord4j.core.object.entity.Member> sender = event.getInteraction().getMember();
    String onlyBeerIsSupportedForNow = "beer";

    if (!sender.isPresent() || !guildId.isPresent()) {
      return Mono.just(invalid(event));
    }

    // Find the beer item
    Server server = daoProvider.serverDao.queryForId(guildId.get().asLong());
    if (server == null) {
      return Mono.just(invalid(event));
    }

    Item beerItem =
        server.getItems().stream()
            .filter(i -> i.getKeyword().equals(onlyBeerIsSupportedForNow))
            .findFirst()
            .orElse(null);

    if (beerItem == null) {
      return Mono.just(invalid(event));
    }

    log.debug(
        "{} command received, sender={}, item={}", keyword(), sender.get().getUsername(), beerItem);

    return handleItem(event, guildId.get(), sender.get(), beerItem);
  }

  private InteractionApplicationCommandCallbackReplyMono invalid(ChatInputInteractionEvent event) {
    return event
        .reply("An error occurred processing the command: " + event.getCommandName())
        .withEphemeral(true);
  }

  Mono<String> displayNameOrMention(Guild guild, MemberItem memberItem) {
    // Use mention directly instead of making API calls to get display name
    // This is much faster and avoids Discord API rate limits and timeouts
    return Mono.just(memberItem.getMember().toMention());
  }
}
