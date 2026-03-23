package hyperpaint.mc.telegram_adapter;

import hyperpaint.mc.telegram_adapter.api.TelegramAdapterV1;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.util.DefaultGetUpdatesGenerator;
import org.telegram.telegrambots.meta.TelegramUrl;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TelegramAdapter implements TelegramAdapterV1, LongPollingUpdateConsumer, AutoCloseable {
    private static TelegramAdapter INSTANCE;

    public static TelegramAdapterV1 get() {
        return INSTANCE;
    }

    private final TelegramUrl telegramUrl;
    private final TelegramClient client;
    private final TelegramBotsLongPollingApplication telegramBots;
    private final List<Consumer> consumers = new ArrayList<>();

    private TelegramAdapter(String url, String token) {
        telegramUrl = getTelegramUrl(url);
        client = new OkHttpTelegramClient(token, telegramUrl);
        telegramBots = new TelegramBotsLongPollingApplication();

        try {
            telegramBots.registerBot(token, () -> telegramUrl, new DefaultGetUpdatesGenerator(), this);
        } catch (TelegramApiException e) {
            log.error(e.toString(), e);
        }
    }

    private static TelegramUrl getTelegramUrl(String url) {
        final Pattern pattern = Pattern.compile("(.+)://(.+):(.+)");
        final Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            try {
                return new TelegramUrl(matcher.group(1), matcher.group(2), Integer.parseInt(matcher.group(3)), false);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Invalid Telegram URL: " + url);
            }
        } else {
            throw new IllegalStateException("Invalid Telegram URL: " + url);
        }
    }

    public static void initialize() {
        log.debug("Initializing");

        final File file = new File(FabricLoader.getInstance().getConfigDir() + "/telegram-adapter.yaml");
        final TelegramAdapterConfig config;

        try {
            log.debug("Started: load config; path=\"{}\"", file.getAbsolutePath());

            if (!file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IOException("Can't make directories for file \"%s\", check your permissions".formatted(file.getParentFile()));
                }
            }

            if (!file.exists()) {
                log.info("Config file not exists, saving default");

                try (var inputStream = TelegramAdapterConfig.class.getClassLoader().getResourceAsStream(file.getName())) {
                    if (inputStream == null) {
                        throw new IOException("Can't read resource \"%s\", check your resources".formatted(file.getName()));
                    }

                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(inputStream.readAllBytes());
                    }
                } catch (IOException e) {
                    throw new IOException("Can't write config to file \"%s\", check your permissions".formatted(file), e);
                }
            }

            try (FileInputStream inputStream = new FileInputStream(file)) {
                final Yaml yaml = new Yaml(new Constructor(TelegramAdapterConfig.class, new LoaderOptions()));
                config = yaml.loadAs(inputStream, TelegramAdapterConfig.class);
            } catch (IOException e) {
                throw new IOException("Can't read config from file \"%s\", check your permissions".formatted(file), e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            log.debug("Finished: load config; path=\"{}\"", file.getAbsolutePath());
        }

        INSTANCE = new TelegramAdapter(
                config.getTelegramAdapter().getUrl(), config.getTelegramAdapter().getToken()
        );

        log.debug("Initialized");
    }

    public static void terminate() {
        log.debug("Terminating");

        try {
            INSTANCE.close();
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        log.debug("Terminated");
    }

    @Override
    public void produce(String identifier, BotApiMethod<?> method) {
        try {
            client.execute(method);
            log.debug("Message sent; identifier={}, method={}", identifier, method);
        } catch (Exception e) {
            log.error(e.toString(), e);
            log.error("Can't send message; identifier={}", identifier);
        }
    }

    @Override
    public void subscribe(Consumer consumer) {
        consumers.add(consumer);
        log.debug("Consumer subscribed; consumer={}", consumer);
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(update -> {
            log.debug("Message received; identifier={}, update={}", update.getUpdateId(), update);
            consumers.forEach(consumer -> {
                try {
                    consumer.consume(update.getUpdateId().toString(), update);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                    log.error("Can't handle message; identifier={}, consumer={}", update.getUpdateId(), consumer);
                }
            });
        });
    }

    @Override
    public void close() throws Exception {
        telegramBots.close();
    }
}
