package com.trumpetx.beer.commands;

import com.trumpetx.beer.GuildInitializer;
import com.trumpetx.beer.domain.DaoProvider;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandFactory {
  private final Map<String, Command> commands;

  public CommandFactory(DaoProvider daoProvider, GuildInitializer guildInitializer) {
    commands =
        Stream.of(
                new PlusPlus(daoProvider, guildInitializer),
                new MinusMinus(daoProvider, guildInitializer),
                new Count(daoProvider),
                new Percent(daoProvider),
                new Help(daoProvider))
            .collect(Collectors.toMap(Command::keyword, Function.identity()));
  }

  public Map<String, Command> getCommands() {
    return commands;
  }
}
