package com.trumpetx.beer.commands;

import com.j256.ormlite.dao.ForeignCollection;
import com.trumpetx.beer.domain.DaoProvider;
import com.trumpetx.beer.domain.Item;
import com.trumpetx.beer.domain.MemberItem;
import com.trumpetx.beer.domain.Server;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.rest.http.client.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Optional.ofNullable;

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

  abstract InteractionApplicationCommandCallbackReplyMono handleItem(ChatInputInteractionEvent event, Snowflake guildId, discord4j.core.object.entity.Member sender, Item item);

  @Override
  public InteractionApplicationCommandCallbackReplyMono execute(ChatInputInteractionEvent event) {
    Optional<Snowflake> guildId = event.getInteraction().getGuildId();
    Optional<discord4j.core.object.entity.Member> sender = event.getInteraction().getMember();
    String onlyBeerIsSupportedForNow = "beer";
    return sender.map(member -> guildId
      .map(Snowflake::asLong)
      .map(daoProvider.serverDao::queryForId)
      .map(Server::getItems)
      .map(ForeignCollection::stream)
      .flatMap(itemStream -> itemStream.filter(i -> i.getKeyword().equals(onlyBeerIsSupportedForNow)).findFirst())
      .map(item -> {
        log.debug("{} command received, sender={}, item={}", keyword(), member.getUsername(), item);
        return handleItem(event, guildId.get(), member, item);
      }).orElseGet(() -> invalid(event)))
      .orElseGet(() -> invalid(event));
  }

  private InteractionApplicationCommandCallbackReplyMono invalid(ChatInputInteractionEvent event) {
    return event
      .reply("An error occurred processing the command: " + event.getCommandName( ))
      .withEphemeral(true);
  }

  String displayNameOrMention(Guild guild, MemberItem memberItem) {
    discord4j.core.object.entity.Member member = null;
    try{
       member = guild.getMemberById(Snowflake.of(memberItem.getMember().getId())).block();
    } catch (ClientException e) {
      log.debug("Error looking up {}/{} : {}", guild.getName(), memberItem.getMember().getId(), e.getMessage());
    }
    return ofNullable(member)
      .map(discord4j.core.object.entity.Member::getDisplayName)
      .orElseGet(memberItem.getMember()::toMention);
  }
}
