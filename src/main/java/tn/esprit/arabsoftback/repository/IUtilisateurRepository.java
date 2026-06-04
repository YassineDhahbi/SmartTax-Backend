package tn.esprit.arabsoftback.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.arabsoftback.entity.Utilisateur;
import tn.esprit.arabsoftback.entity.Role;

import java.util.List;
import java.util.Optional;

public interface IUtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    List<Utilisateur> findByEmailIsNotNull();

    List<Utilisateur> findByRole(tn.esprit.arabsoftback.entity.Role role);

    Optional<Utilisateur> findFirstByRoleAndIdUtilisateurNot(Role role, Integer idUtilisateur);
}
