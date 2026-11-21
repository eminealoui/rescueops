package tn.esprit.rescueops.hospital.service;

import org.junit.jupiter.api.Test;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpirationJobTest {

    @Test
    void expireLesReservationsEchues() {
        DemandeTransfertRepository repo = mock(DemandeTransfertRepository.class);
        TransfertService transferts = mock(TransfertService.class);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        when(repo.idsEchus(any(), any())).thenReturn(List.of(a, b));
        when(transferts.expirer(any())).thenReturn(true);

        int n = new ExpirationJob(repo, transferts).executer();

        assertThat(n).isEqualTo(2);
        verify(transferts).expirer(a);
        verify(transferts).expirer(b);
    }

    @Test
    void uneErreurSurUneDemandeNEmpechePasLesAutres() {
        DemandeTransfertRepository repo = mock(DemandeTransfertRepository.class);
        TransfertService transferts = mock(TransfertService.class);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        when(repo.idsEchus(any(), any())).thenReturn(List.of(a, b));
        when(transferts.expirer(a)).thenThrow(new ConflictException("X", "déjà traitée"));
        when(transferts.expirer(b)).thenReturn(true);

        int n = new ExpirationJob(repo, transferts).executer();

        assertThat(n).isEqualTo(1);
        verify(transferts).expirer(b);
    }
}
