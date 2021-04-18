package com.trumpetx.beer.commands;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;

import java.util.Arrays;
import java.util.List;

public class CommandFactory {
  private final List<Command> commands;

  public CommandFactory(DaoProvider daoProvider, GuildInitializer guildInitializer) {
    commands = Arrays.asList(
      new PlusPlus(daoProvider, guildInitializer),
      new MinusMinus(daoProvider, guildInitializer),
      new Count(daoProvider),
      new Percent(daoProvider),
      new Help(daoProvider)
    );
  }

  public List<Command> getCommands() {
    return commands;
  }
}
