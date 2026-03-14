package com.pulse.client;

import com.pulse.client.account.AccountManager;
import com.pulse.client.command.CommandManager;
import com.pulse.client.config.ConfigManager;
import com.pulse.client.event.EventBus;
import com.pulse.client.gui.ClickGUI;
import com.pulse.client.gui.HUD;
import com.pulse.client.module.ModuleManager;
import com.pulse.client.render.ShaderRegistry;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulseClient implements ClientModInitializer {

    public static final String NAME    = "PulseClient";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER  = LoggerFactory.getLogger(NAME);

    private static PulseClient instance;

    private EventBus       eventBus;
    private ModuleManager  moduleManager;
    private CommandManager commandManager;
    private ClickGUI       clickGUI;
    private HUD            hud;
    private AccountManager accountManager;
    private ConfigManager  configManager;

    @Override
    public void onInitializeClient() {
        instance       = this;

        // Шейдеры регистрируются ДО загрузки ресурсов — callback будет вызван позже
        ShaderRegistry.init();

        eventBus       = new EventBus();
        moduleManager  = new ModuleManager();
        commandManager = new CommandManager();
        clickGUI       = new ClickGUI();

        hud            = new HUD();
        accountManager = new AccountManager();

        // Создаем конфиг только ПОСЛЕ того, как ModuleManager зарегистрировал модули
        configManager  = new ConfigManager();
        configManager.load(); // Загружаем сохраненные настройки

        // Хук: автоматически сохраняем конфиг при выходе из игры
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Saving {} config...", NAME);
            configManager.save();
        }));

        LOGGER.info("{} {} initialized", NAME, VERSION);
    }

    public static PulseClient getInstance()    { return instance; }
    public EventBus       getEventBus()        { return eventBus; }
    public ModuleManager  getModuleManager()   { return moduleManager; }
    public CommandManager getCommandManager()  { return commandManager; }
    public ClickGUI       getClickGUI()        { return clickGUI; }
    public HUD            getHud()             { return hud; }
    public AccountManager getAccountManager()  { return accountManager; }
    public ConfigManager  getConfigManager()   { return configManager; }
}