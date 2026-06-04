package tn.esprit.arabsoftback.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "demande_information")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeInformation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_complet", nullable = false, length = 120)
    private String nomComplet;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "telephone", length = 30)
    private String telephone;

    @Column(name = "sujet", nullable = false, length = 200)
    private String sujet;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "urgent", nullable = false)
    @Builder.Default
    private Boolean urgent = false;

    @Column(name = "traitement_status", length = 20)
    @Builder.Default
    private String traitementStatus = "NON_TRAITE";

    @Column(name = "assigned_agent_id")
    private Integer assignedAgentId;

    @Column(name = "assigned_agent_name", length = 180)
    private String assignedAgentName;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
}

