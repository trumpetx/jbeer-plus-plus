package com.trumpetx.beer;

import com.trumpetx.beer.commands.Command;
import com.trumpetx.beer.commands.CommandFactory;
import com.trumpetx.beer.domain.DaoProvider;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import discord4j.discordjson.json.ActivityUpdateRequest;
import discord4j.discordjson.json.gateway.StatusUpdate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import static com.trumpetx.beer.LogConfigurer.setProgramLogging;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;

public class DiscordApp implements Runnable {
  private static final String PROP_FILE = "beer.properties";
  private static Logger LOG;
  private final List<Command> commands;
  private final GuildInitializer guildInitializer;
  private final GatewayDiscordClient gateway;
  private final AtomicLong guildCount = new AtomicLong();

  public DiscordApp(String token, DaoProvider daoProvider) {
    gateway = DiscordClient.create(token).login().block();
    Objects.requireNonNull(gateway, "The DiscordClientGateway could not login.");
    guildInitializer = new GuildInitializer(daoProvider);
    commands = new CommandFactory(daoProvider, guildInitializer).getCommands();
  }

  public static void main(String[] args) {
    Properties properties = new Properties();
    try (InputStream is = new FileInputStream(PROP_FILE)) {
      properties.load(is);
    } catch (IOException e) {
      e.printStackTrace(System.err);
      exit(2, "Properties file not found, expected: " + PROP_FILE);
    }
    try {
      setProgramLogging(properties.getProperty("rootLogLevel"), properties.getProperty("logLevel"));
      LOG = LoggerFactory.getLogger(DiscordApp.class);
    } catch (Exception e) {
      e.printStackTrace(System.err);
      exit(3, "Error configuring logging");
    }
    try {
      String token = properties.getProperty("token");
      if (isBlank(token)) {
        exit(2, "Discord token is required in " + PROP_FILE);
      }
      LOG.info("https://discordapp.com/oauth2/authorize?client_id=" + properties.getProperty("clientId") + "&scope=bot&permissions=" + properties.getProperty("permissions"));
      DiscordApp discordApp = new DiscordApp(token, DaoProvider.initializeInstance(properties.getProperty("databaseUrl")));
      discordApp.run();
    } catch (Exception e) {
      LOG.error("Unexpected Program Error", e);
      exit(1, e.getMessage());
    }
  }

  static void exit(int exitCode, String msg) {
    (exitCode == 0 ? System.out : System.err).println(msg);
    System.exit(exitCode);
  }

  private void updateGuildCount(long guildCount) {
    LOG.debug("Updating guild count to {}", guildCount);
    gateway.updatePresence(Presence.online(Activity.playing("on " + guildCount + " servers"))).subscribe();
  }

  @Override
  public void run() {
    gateway.getGuilds()
      .count()
      .subscribe(this::updateGuildCount);
    gateway.getGuilds()
      .flatMap(guildInitializer)
      .subscribe();
    gateway.on(GuildCreateEvent.class)
      .map(GuildCreateEvent::getGuild)
      .flatMap(guildInitializer::apply)
      .subscribe();
    gateway.on(GuildDeleteEvent.class)
      .map(GuildDeleteEvent::getGuild)
      .flatMap(g -> {
        LOG.info("{} has disconnected", g.map(Guild::getName).orElse("A deleted guild"));
        return Mono.empty(); //updateGuildCount(guildCount.decrementAndGet());
      })
      .subscribe();
    gateway
      .getEventDispatcher()
      .on(MessageCreateEvent.class)
      .flatMap(
        event ->
          Mono.just(event.getMessage().getContent())
            .flatMap(
              content ->
                Flux.fromIterable(commands)
                  .map(entry -> {
                    Matcher m = Regex.command(entry.keyword()).matcher(event.getMessage().getContent());
                    return Pair.of(entry, m);
                  })
                  .filter(p -> p.getValue().matches())
                  .flatMap(entry -> {
                    try {
                      String command = lowerCase(entry.getValue().group(1));
                      return entry.getKey().execute(command, event);
                    } catch (RuntimeException e) {
                      LOG.error("Error processing eventType: {}", entry.getClass(), e);
                      return Mono.empty();
                    }
                  })
                  .next()))
      .onErrorResume(e -> {
        LOG.error("Unexpected error doing something: {}", e.getMessage());
        return Mono.empty();
      })
      .subscribe();
    gateway.getApplicationInfo().subscribe(info -> LOG.info("Started: {}", info.getName()));
    gateway.onDisconnect().block();
  }
}
