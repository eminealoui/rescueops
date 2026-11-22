
package tn.esprit.rescueops.intervention.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tn.esprit.rescueops.intervention.dto.RapportResponse;
import tn.esprit.rescueops.intervention.service.RapportInterventionService;

@RestController
@RequestMapping("/api/interventions/rapports")
public class RapportListeController {

    private final RapportInterventionService service;

    public RapportListeController(
        RapportInterventionService service
    ) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public Page<RapportResponse> lister(
        @PageableDefault(
            size = 10,
            sort = "creeLe",
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        return service.lister(pageable);
    }
}
