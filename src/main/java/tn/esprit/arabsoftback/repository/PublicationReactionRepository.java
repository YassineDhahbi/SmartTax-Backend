package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.PublicationReaction;

import java.util.Optional;

@Repository
public interface PublicationReactionRepository extends JpaRepository<PublicationReaction, Long> {
    Optional<PublicationReaction> findByPublication_IdAndUtilisateur_IdUtilisateur(Long publicationId, Integer utilisateurId);
}
