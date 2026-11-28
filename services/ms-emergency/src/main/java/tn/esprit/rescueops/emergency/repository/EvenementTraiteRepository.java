package tn.esprit.rescueops.emergency.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.esprit.rescueops.emergency.domain.EvenementTraite;

public interface EvenementTraiteRepository
        extends JpaRepository<EvenementTraite, UUID> {
}