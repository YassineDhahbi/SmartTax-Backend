package tn.esprit.arabsoftback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicationSchedulingService {

    private final PublicationService publicationService;

    @Scheduled(fixedDelay = 60000)
    public void publishScheduledPublicationsTask() {
        try {
            publicationService.publishScheduledPublications();
        } catch (Exception e) {
            log.error("Erreur lors de la publication automatique des publications programm�es", e);
        }
    }
}
