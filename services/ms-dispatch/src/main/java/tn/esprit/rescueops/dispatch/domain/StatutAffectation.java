package tn.esprit.rescueops.dispatch.domain;

/**
 * Cycle de vie d'une affectation : PROPOSEE → CONFIRMEE ou ANNULEE.
 * BF-08 / BF-09 : transitions gérées par AffectationService.
 */
public enum StatutAffectation {
    /** L'algorithme de scoring a proposé cette affectation, en attente de validation humaine. */
    PROPOSEE,
    /** L'opérateur a validé l'affectation ; la ressource est réservée et l'événement affectation.confirmee est publié. */
    CONFIRMEE,
    /** L'affectation a été annulée (conflit ou décision de l'opérateur). */
    ANNULEE
}
