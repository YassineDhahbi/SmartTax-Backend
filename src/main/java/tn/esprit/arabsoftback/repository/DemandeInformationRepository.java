package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import tn.esprit.arabsoftback.entity.DemandeInformation;

import java.util.List;

@Repository
public interface DemandeInformationRepository extends JpaRepository<DemandeInformation, Long>,
        JpaSpecificationExecutor<DemandeInformation> {
    List<DemandeInformation> findAllByOrderByDateCreationDesc();

    long countByTraitementStatus(String traitementStatus);

    long countByUrgentTrue();
}

