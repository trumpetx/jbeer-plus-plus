package com.trumpetx.beer.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.possible.Possible;

import java.util.List;

public interface Command {
  String keyword();
  String description();
  InteractionApplicationCommandCallbackReplyMono execute(ChatInputInteractionEvent event);
  default Possible<List<ApplicationCommandOptionData>> options() {
    return Possible.absent();
  }
}
