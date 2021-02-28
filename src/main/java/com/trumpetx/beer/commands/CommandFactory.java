package com.trumpetx.beer.commands;

import com.trumpetx.beer.domain.DaoProvider;

import java.util.Arrays;
import java.util.List;

public class CommandFactory {
  private final DaoProvider daoProvider;

  public CommandFactory(DaoProvider daoProvider) {
    this.daoProvider = daoProvider;
  }

  public List<Command> getCommands() {
    return Arrays.asList(
      new PlusPlus(daoProvider),
      new MinusMinus(daoProvider),
      new Count(daoProvider),
      new Percent(daoProvider)
    );
  }
}
