package tn.esprit.rescueops.hospital.domain;

import java.util.Set;

/**
 * Cycle de vie d'une demande de transfert.
 * <pre>
 * DEMANDEE ─reserver→ PLACE_RETENUE ─confirmer→ CONFIRMEE ─admettre→ ADMIS ─sortie→ SORTI
 *    │                    ├─refuser→ REFUSEE   (place rendue)
 *    │                    ├─délai→   EXPIREE   (place rendue)
 *    └─annuler→ ANNULEE   └─annuler→ ANNULEE   (place rendue ; aussi depuis CONFIRMEE)
 * </pre>
 */
public enum StatutTransfert {
    DEMANDEE, PLACE_RETENUE, CONFIRMEE, ADMIS, SORTI, REFUSEE, EXPIREE, ANNULEE;

    /** Statuts dans lesquels la demande occupe une place du service. */
    public static final Set<StatutTransfert> OCCUPE_UNE_PLACE = Set.of(PLACE_RETENUE, CONFIRMEE, ADMIS);

    /** Statuts depuis lesquels une annulation est permise. */
    public static final Set<StatutTransfert> ANNULABLE = Set.of(DEMANDEE, PLACE_RETENUE, CONFIRMEE);
}
