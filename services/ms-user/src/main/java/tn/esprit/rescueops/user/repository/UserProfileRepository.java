package tn.esprit.rescueops.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.rescueops.user.domain.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByKeycloakId(String keycloakId);

    /** Recherche insensible à la casse sur prénom, nom ou email ; filtres facultatifs (null = ignoré). */
    @Query("""
            SELECT p FROM UserProfile p
            WHERE (:query IS NULL
                   OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(p.email)     LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:active IS NULL OR p.active = :active)
            """)
    Page<UserProfile> search(@Param("query") String query, @Param("active") Boolean active, Pageable pageable);
}
