package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.arabsoftback.entity.DownloadDocumentCategory;
import tn.esprit.arabsoftback.entity.DownloadLibraryDocument;

import java.util.List;

public interface DownloadLibraryDocumentRepository extends JpaRepository<DownloadLibraryDocument, Long> {

    List<DownloadLibraryDocument> findAllByCategoryOrderByUpdatedAtDesc(DownloadDocumentCategory category);

    List<DownloadLibraryDocument> findAllByOrderByCategoryAscUpdatedAtDesc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE DownloadLibraryDocument d SET d.downloadCount = d.downloadCount + 1 WHERE d.id = :id")
    int incrementDownloadCount(@Param("id") Long id);
}
