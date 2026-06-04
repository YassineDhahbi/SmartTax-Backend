package tn.esprit.arabsoftback.service;

import org.springframework.stereotype.Service;
import tn.esprit.arabsoftback.repository.IUtilisateurRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class UserPresenceService {

    public static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);

    private final ConcurrentHashMap<Integer, Instant> lastActivityByUserId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> userIdByEmail = new ConcurrentHashMap<>();
    private final IUtilisateurRepository utilisateurRepository;

    public UserPresenceService(IUtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public void markPresent(Integer userId) {
        if (userId != null) {
            lastActivityByUserId.put(userId, Instant.now());
        }
    }

    public void markPresentByEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        Integer userId = userIdByEmail.computeIfAbsent(email, this::resolveUserId);
        if (userId != null) {
            markPresent(userId);
        }
    }

    private Integer resolveUserId(String email) {
        return utilisateurRepository.findByEmail(email)
                .map(u -> u.getIdUtilisateur())
                .orElse(null);
    }

    public boolean isOnline(Integer userId) {
        if (userId == null) {
            return false;
        }
        Instant last = lastActivityByUserId.get(userId);
        return last != null && last.isAfter(Instant.now().minus(ONLINE_THRESHOLD));
    }

    public Set<Integer> getOnlineUserIds() {
        Instant cutoff = Instant.now().minus(ONLINE_THRESHOLD);
        return lastActivityByUserId.entrySet().stream()
                .filter(entry -> entry.getValue().isAfter(cutoff))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public int getOnlineCount() {
        return getOnlineUserIds().size();
    }
}
