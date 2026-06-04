package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.PublicationComment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicationCommentRepository extends JpaRepository<PublicationComment, Long> {

    List<PublicationComment> findByPublication_IdOrderByCreatedAtDesc(Long publicationId);

    long countByPublication_Id(Long publicationId);

    Optional<PublicationComment> findByIdAndPublication_Id(Long commentId, Long publicationId);

    @Modifying
    long deleteByIdAndPublication_IdAndUtilisateur_IdUtilisateur(Long commentId, Long publicationId, Integer userId);

    @Query("SELECT c.id FROM PublicationComment c WHERE c.utilisateur.idUtilisateur = :userId")
    List<Long> findIdsByUtilisateur_IdUtilisateur(Integer userId);

    @Modifying
    long deleteByUtilisateur_IdUtilisateur(Integer userId);
}
