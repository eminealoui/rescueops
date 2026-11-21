package tn.esprit.rescueops.hospital.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rescueops.hospital.WebSliceTest;
import tn.esprit.rescueops.hospital.service.CompatibiliteService;
import tn.esprit.rescueops.hospital.service.EtablissementService;
import tn.esprit.rescueops.hospital.service.ExpirationJob;
import tn.esprit.rescueops.hospital.service.FluxService;
import tn.esprit.rescueops.hospital.service.TableauDeBordService;
import tn.esprit.rescueops.hospital.service.TransfertService;

/** Base des tests d'API : sécurité réelle, TOUS les services métier simulés (pas de base, pas de broker). */
@WebSliceTest
abstract class ApiTestBase {

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    protected EtablissementService etablissements;

    @MockitoBean
    protected TableauDeBordService tableauDeBord;

    @MockitoBean
    protected TransfertService transferts;

    @MockitoBean
    protected CompatibiliteService compatibilite;

    @MockitoBean
    protected FluxService flux;

    @MockitoBean
    protected ExpirationJob expirationJob;
}
