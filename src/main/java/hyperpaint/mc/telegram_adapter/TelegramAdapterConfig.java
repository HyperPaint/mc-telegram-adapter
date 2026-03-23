package hyperpaint.mc.telegram_adapter;

import lombok.Data;

@Data
public class TelegramAdapterConfig {
    private Config telegramAdapter;

    @Data
    public static class Config {
        private String url;
        private String token;
    }
}
