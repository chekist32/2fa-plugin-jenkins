package io.jenkins.plugins.twofactor.jenkins.user;

import java.util.*;

public class UserTfaCookieTokenService {
    private static UserTfaCookieTokenService instance;

    private final Map<String, Set<UUID>> userTfaCookieTokens = new HashMap<>();

    private UserTfaCookieTokenService() { }

    public boolean contains(String userId, UUID token) {
        var set = userTfaCookieTokens.getOrDefault(userId, null);
        if (set == null) return false;

        return set.contains(token);
    }

    public UUID addToken(String userId) {
        var set = userTfaCookieTokens.getOrDefault(userId, new HashSet<UUID>());
        var token = UUID.randomUUID(); set.add(token);
        userTfaCookieTokens.put(userId, set);

        return token;
    }

    public Set<UUID> getAllTokens(String userId) {
        var set = userTfaCookieTokens.getOrDefault(userId, new HashSet<UUID>());

        return Collections.unmodifiableSet(set);
    }

    public void invalidateToken(String userId, UUID token) {
        var set = userTfaCookieTokens.getOrDefault(userId, null);
        if (set == null) return;

        set.remove(token);
    }

    public void invalidateAllTokens(String userId) {
        var set = userTfaCookieTokens.getOrDefault(userId, null);
        if (set == null) return;

        set.clear();
    }

    public synchronized static UserTfaCookieTokenService getInstance() {
        if (instance == null) instance = new UserTfaCookieTokenService();

        return instance;
    }
}
