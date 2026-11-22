import { Injectable, computed, inject, signal } from '@angular/core';
import { Hopital } from '../../core/api/models';
import { HospitalApi } from '../../core/api/hospital.api';

/** Liste des hôpitaux (coordonnées, services), chargée une fois et partagée par les écrans du module. */
@Injectable({ providedIn: 'root' })
export class ReferentielHopitaux {
  private readonly api = inject(HospitalApi);
  readonly hopitaux = signal<Hopital[]>([]);
  private charge = false;

  /** Hôpital qui contient un service donné. */
  readonly parService = computed(() => {
    const m = new Map<string, Hopital>();
    for (const h of this.hopitaux()) for (const s of h.services) m.set(s.id, h);
    return m;
  });
  readonly parId = computed(() => new Map(this.hopitaux().map((h) => [h.id, h])));

  charger(force = false): void {
    if (this.charge && !force) return;
    this.charge = true;
    this.api.hopitaux().subscribe({ next: (h) => this.hopitaux.set(h), error: () => (this.charge = false) });
  }
}
