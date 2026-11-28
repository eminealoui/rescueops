package tn.esprit.rescueops.hospital.service;

import org.junit.jupiter.api.Test;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.event.TransfertStatutChange;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FluxDirectTest {

    private final FluxService flux = mock(FluxService.class);
    private final FluxDirect direct = new FluxDirect(flux);

    @Test
    void abonnementEnvoieLaListeActuelleEtGardeLAbonne() {
        given(flux.derniers(null)).willReturn(List.of(
                new FluxService.Evenement(UUID.randomUUID(), StatutTransfert.DEMANDEE, "CHU Test", Instant.now())));

        direct.abonner();

        verify(flux).derniers(null);
        assertThat(direct.nombreAbonnes()).isEqualTo(1);
    }

    @Test
    void diffusionRelitLaListeUneFoisPourTousLesAbonnes() {
        given(flux.derniers(null)).willReturn(List.of());
        direct.abonner();
        direct.abonner();

        direct.diffuser();

        // 2 envois à l'abonnement + 1 seule relecture pour la diffusion
        verify(flux, times(3)).derniers(null);
    }

    @Test
    void sansAbonneAucuneDiffusion() {
        direct.surChangement(new TransfertStatutChange(UUID.randomUUID(), UUID.randomUUID(), null, StatutTransfert.DEMANDEE));

        verify(flux, never()).derniers(null);
    }
}
