import { Dialog, DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, Injectable, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, map } from 'rxjs';

export interface ConfirmData {
  titre: string;
  texte: string;
  action: string;
  danger?: boolean;
  /** Si défini : un champ texte facultatif (ex. motif de refus) est proposé. */
  champ?: string;
}

/** Résultat : null si annulé, sinon le texte saisi (chaîne vide si aucun champ). */
@Component({
  selector: 'app-confirm',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="dlg" role="alertdialog" aria-labelledby="dlg-titre" aria-describedby="dlg-texte">
      <h2 id="dlg-titre">{{ data.titre }}</h2>
      <p id="dlg-texte">{{ data.texte }}</p>
      @if (data.champ) {
        <div class="field">
          <label for="dlg-champ">{{ data.champ }}</label>
          <textarea id="dlg-champ" class="textarea" rows="3" maxlength="500" [(ngModel)]="saisie"></textarea>
        </div>
      }
      <div class="actions">
        <button type="button" class="btn btn-outline" (click)="ref.close(null)">Annuler</button>
        <button type="button" class="btn" [class.btn-danger]="data.danger" [class.btn-primary]="!data.danger" (click)="ref.close(saisie.trim())" cdkFocusInitial>
          {{ data.action }}
        </button>
      </div>
    </div>
  `,
  styles: `
    .dlg { background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: var(--radius); padding: 22px;
           box-shadow: var(--shadow-deep); width: min(440px, calc(100vw - 32px)); display: grid; gap: 12px; animation: in var(--t) var(--ease); }
    h2 { font-size: 18px; font-weight: 600; }
    p { color: var(--text-2); }
    .actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 4px; }
    @keyframes in { from { opacity: 0; transform: translateY(10px) scale(.98); } }
  `,
})
export class ConfirmDialog {
  protected readonly data = inject<ConfirmData>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<string | null>>(DialogRef);
  protected saisie = '';
}

@Injectable({ providedIn: 'root' })
export class Confirm {
  private readonly dialog = inject(Dialog);

  demander(data: ConfirmData): Observable<string | null> {
    const ref = this.dialog.open<string | null, ConfirmData>(ConfirmDialog, { data, backdropClass: 'dlg-fond' });
    return ref.closed.pipe(map((r) => (r === undefined ? null : r)));
  }
}
