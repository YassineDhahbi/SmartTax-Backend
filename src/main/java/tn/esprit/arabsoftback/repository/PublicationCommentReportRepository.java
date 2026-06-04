package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.PublicationCommentReport;

import java.util.List;

@Repository
public interface PublicationCommentReportRepository extends JpaRepository<PublicationCommentReport, Long> {
    List<PublicationCommentReport> findByPublication_IdOrderByCreatedAtDesc(Long publicationId);

    @Modifying
    void deleteByComment_Id(Long commentId);

    @Modifying
    @Query("DELETE FROM PublicationCommentReport r WHERE r.comment.id IN :commentIds")
    void deleteByCommentIds(List<Long> commentIds);

    void deleteByUtilisateur_IdUtilisateur(Integer utilisateurId);
}
