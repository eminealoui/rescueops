import { CdkMenu, CdkMenuItem, CdkMenuTrigger } from '@angular/cdk/menu';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { LIBELLES_ROLE, ROLES_OPERATIONNELS } from '../core/auth/roles';
import { Brand, ThemeToggle } from '../core/ui/brand';
import { Ticker } from '../core/ui/ticker';
import { LiveFeed } from './live-feed.service';
import { NAVIGATION } from './navigation';

/** Coquille commune à tous les écrans connectés : bandeau EN DIRECT, en-tête, onglets par rôle, thème, menu du compte. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Brand, ThemeToggle, Ticker, CdkMenuTrigger, CdkMenu, CdkMenuItem],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly auth = inject(AuthService);
  protected readonly feed = inject(LiveFeed);

  protected readonly onglets = computed(() => NAVIGATION.filter((n) => this.auth.hasAnyRole(n.roles)));
  protected readonly operationnel = computed(() => this.auth.hasAnyRole(ROLES_OPERATIONNELS));
  protected readonly libelleRoles = computed(() =>
    this.auth.roles().filter((r) => r !== 'CITOYEN' || this.auth.roles().length === 1).map((r) => LIBELLES_ROLE[r]).join(' · '),
  );
}
