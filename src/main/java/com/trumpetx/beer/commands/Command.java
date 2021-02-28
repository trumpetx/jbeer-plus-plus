package com.trumpetx.beer.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public interface Command {
  String keyword();

  Mono<?> execute(String command, MessageCreateEvent event);
}
