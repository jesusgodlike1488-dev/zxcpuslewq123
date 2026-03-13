package com.pulse.client.account;

import java.util.UUID;

/**
 * Represents a saved offline (cracked) Minecraft account.
 */
public record OfflineAccount(String username, UUID uuid) {

    /** Create an offline account with a deterministic UUID (matches vanilla offline UUID). */
    public static OfflineAccount of(String username) {
        UUID uuid = UUID.nameUUIDFromBytes(
            ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
        return new OfflineAccount(username, uuid);
    }

    @Override
    public String toString() { return username; }
}
