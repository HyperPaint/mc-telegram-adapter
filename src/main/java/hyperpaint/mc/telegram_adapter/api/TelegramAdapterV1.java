package hyperpaint.mc.telegram_adapter.api;

import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Адаптер для Telegram
 */
public interface TelegramAdapterV1 {
    /**
     * Отправить сообщение в Telegram
     * @param identifier любой идентификатор, связанный с сообщением
     * @param method исходящее сообщение
     */
    void produce(String identifier, BotApiMethod<?> method);

    /**
     * Подписаться на получение сообщений из Telegram
     * @param consumer потребитель сообщений из Telegram
     */
    void subscribe(Consumer consumer);

    /**
     * Потребитель сообщений из Telegram
     */
    interface Consumer {
        /**
         * Потребить сообщение из Telegram
         * @param identifier идентификатор, связанный с сообщением
         * @param update входящее сообщение
         */
        void consume(String identifier, Update update);
    }
}
