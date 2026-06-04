package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.PublicationReport;

import java.util.List;

@Repository
public interface PublicationReportRepository extends JpaRepository<PublicationReport, Long> {
    List<PublicationReport> findByPublication_IdOrderByCreatedAtDesc(Long publicationId);
}
