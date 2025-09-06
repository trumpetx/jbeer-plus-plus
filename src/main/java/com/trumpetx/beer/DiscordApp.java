package com.trumpetx.beer;

import static com.trumpetx.beer.LogConfigurer.setProgramLogging;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.trumpetx.beer.commands.Command;
import com.trumpetx.beer.commands.CommandFactory;
import com.trumpetx.beer.domain.DaoProvider;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.presence.ClientPresence;
import discord4j.discordjson.json.ApplicationCommandRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class DiscordApp implements Runnable {
  private static final String PROP_FILE = "beer.properties";
  private static Logger LOG;
  private final Map<String, Command> commands;
  private final GuildInitializer guildInitializer;
  private GatewayDiscordClient gateway;
  private final String token;

  private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

  public DiscordApp(String token, DaoProvider daoProvider) {
    // Initialize non-blocking components first
    guildInitializer = new GuildInitializer(daoProvider);
    commands = new CommandFactory(daoProvider, guildInitializer).getCommands();

    // Store token for later use in run() method
    this.token = token;
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
      LOG.info(
          "https://discordapp.com/oauth2/authorize?client_id="
              + properties.getProperty("clientId")
              + "&scope=bot&permissions="
              + properties.getProperty("permissions"));
      DiscordApp discordApp =
          new DiscordApp(
              token, DaoProvider.initializeInstance(properties.getProperty("databaseUrl")));
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

  @Override
  public void run() {
    // Create gateway connection reactively
    DiscordClient.create(token)
        .gateway()
        .setInitialPresence(s -> ClientPresence.invisible())
        .login()
        .doOnNext(
            gateway -> {
              this.gateway = gateway;
              LOG.info("Discord gateway connected successfully");
            })
        .doOnError(
            error -> {
              LOG.error("Failed to connect to Discord gateway", error);
              System.exit(1);
            })
        .flatMapMany(
            gateway -> {
              // Setup all event handlers and scheduled tasks
              setupEventHandlers(gateway);
              setupScheduledTasks(gateway);
              setupApplicationCommands(gateway);

              // Return the disconnect stream to keep the application running
              return gateway.onDisconnect();
            })
        .doOnNext(disconnect -> LOG.info("Discord gateway disconnected"))
        .doOnError(error -> LOG.error("Error in Discord gateway", error))
        .blockLast(); // Block the main thread to keep the application running
  }

  private void setupEventHandlers(GatewayDiscordClient gateway) {
    // Initialize existing guilds
    gateway.getGuilds().flatMap(guildInitializer).subscribe();

    // Handle guild creation
    gateway
        .on(GuildCreateEvent.class)
        .map(GuildCreateEvent::getGuild)
        .flatMap(guildInitializer::apply)
        .subscribe();

    // Handle guild deletion
    gateway
        .on(GuildDeleteEvent.class)
        .map(GuildDeleteEvent::getGuild)
        .flatMap(
            g -> {
              LOG.info("{} has disconnected", g.map(Guild::getName).orElse("A deleted guild"));
              return Mono.empty();
            })
        .subscribe();

    // Handle slash commands
    gateway
        .getEventDispatcher()
        .on(
            ChatInputInteractionEvent.class,
            event -> {
              String commandName = event.getCommandName();
              if (commandName.length() > 4) {
                commandName = commandName.substring(4);
              }
              Command command = commands.get(commandName);
              if (command == null) {
                return event
                    .reply("The command " + event.getCommandName() + " is invalid")
                    .withEphemeral(true);
              }
              return command
                  .execute(event)
                  .flatMap(mono -> mono); // Flatten the Mono<Mono<Reply>> to Mono<Reply>
            })
        .onErrorResume(
            e -> {
              LOG.error("Unexpected error processing command: {}", e.getMessage());
              return Mono.empty();
            })
        .subscribe();
  }

  private void setupScheduledTasks(GatewayDiscordClient gateway) {
    executor.scheduleWithFixedDelay(new StatusUpdater(gateway), 10, 900, TimeUnit.SECONDS);
  }

  private void setupApplicationCommands(GatewayDiscordClient gateway) {
    gateway.getApplicationInfo().subscribe(info -> LOG.info("Started: {}", info.getName()));

    gateway
        .getRestClient()
        .getApplicationId()
        .subscribe(
            applicationId ->
                gateway
                    .getRestClient()
                    .getApplicationService()
                    .bulkOverwriteGlobalApplicationCommand(
                        applicationId,
                        commands.values().stream()
                            .map(
                                command ->
                                    ApplicationCommandRequest.builder()
                                        .name("beer" + command.keyword())
                                        .description(command.description())
                                        .options(command.options())
                                        .build())
                            .peek(cmd -> LOG.info("{} registered", cmd.name()))
                            .collect(Collectors.toList()))
                    .onErrorStop()
                    .subscribe());
  }
}
