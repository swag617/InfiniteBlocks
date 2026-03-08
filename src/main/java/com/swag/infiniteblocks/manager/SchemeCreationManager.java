package com.swag.infiniteblocks.manager;

import java.util.*;

/**
 * Simple session manager for interactive scheme creation.
 * Stores sessions keyed by player UUID.
 */
public class SchemeCreationManager {

    public enum Step {
        NAME,
        BIRDFLOP,
        LORE
    }

    public static class Session {
        public final String key;
        public Step step = Step.NAME;
        public String displayName;
        public String birdflopRaw;
        public final List<String> lore = new ArrayList<>();

        public Session(String key) {
            this.key = key;
        }
    }

    private final Map<UUID, Session> sessions = new HashMap<>();

    /** Start a session for a player with a given scheme key. */
    public void start(String key, UUID playerUuid) {
        Session s = new Session(key);
        sessions.put(playerUuid, s);
    }

    /** End and remove a session for a player. */
    public void end(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    /** Returns true if the player currently has an active session. */
    public boolean isActive(UUID playerUuid) {
        return sessions.containsKey(playerUuid);
    }

    /** Get the session for the player (or null). */
    public Session get(UUID playerUuid) {
        return sessions.get(playerUuid);
    }
}