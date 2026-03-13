package com.pulse.client.account;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Manages offline (cracked) accounts and handles session switching.
 *
 * Accounts are persisted to .minecraft/pulse_accounts.json
 */
public class AccountManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("PulseClient/Accounts");
    private static final String FILE_NAME = "pulse_accounts.json";

    private final List<OfflineAccount> accounts = new ArrayList<>();
    private final Path saveFile;

    public AccountManager() {
        saveFile = MinecraftClient.getInstance().runDirectory.toPath().resolve(FILE_NAME);
        load();
    }

    // ──────────────────────── public API ─────────────────────────────────── //

    /** Add a new offline account (no-op if name already exists). */
    public boolean addAccount(String username) {
        if (username == null || username.isBlank() || username.length() > 16) return false;
        if (accounts.stream().anyMatch(a -> a.username().equalsIgnoreCase(username))) return false;
        accounts.add(OfflineAccount.of(username));
        save();
        return true;
    }

    /** Remove account at list index. */
    public boolean removeAccount(int index) {
        if (index < 0 || index >= accounts.size()) return false;
        accounts.remove(index);
        save();
        return true;
    }

    /**
     * Switch the active Minecraft session to the given offline account.
     * Uses reflection to mutate the private {@code session} field inside MinecraftClient.
     */
    public boolean login(OfflineAccount account) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();

            // Build an offline Session (Minecraft 1.21.x uses record Session)
            Session newSession = new Session(
                account.username(),
                account.uuid(),
                "",                      // accessToken – empty for offline
                Optional.empty(),        // xuid
                Optional.empty(),        // clientId
                Session.AccountType.LEGACY
            );

            // MinecraftClient#session is private – use reflection
            Field f = MinecraftClient.class.getDeclaredField("session");
            f.setAccessible(true);
            f.set(mc, newSession);

            LOGGER.info("Switched session to offline account: {}", account.username());
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to switch session: {}", e.getMessage());
            return false;
        }
    }

    public List<OfflineAccount> getAccounts() { return Collections.unmodifiableList(accounts); }

    // ──────────────────────── persistence ────────────────────────────────── //

    private void save() {
        try {
            JsonArray arr = new JsonArray();
            for (OfflineAccount a : accounts) {
                JsonObject o = new JsonObject();
                o.addProperty("username", a.username());
                o.addProperty("uuid",     a.uuid().toString());
                arr.add(o);
            }
            Files.writeString(saveFile, new GsonBuilder().setPrettyPrinting().create().toJson(arr),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not save accounts: {}", e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(saveFile)) return;
        try {
            String json = Files.readString(saveFile, StandardCharsets.UTF_8);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                String username = o.get("username").getAsString();
                UUID   uuid     = UUID.fromString(o.get("uuid").getAsString());
                accounts.add(new OfflineAccount(username, uuid));
            }
        } catch (Exception e) {
            LOGGER.error("Could not load accounts: {}", e.getMessage());
        }
    }
}
