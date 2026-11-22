import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/theme/theme.service';
import { ToastHost } from './core/ui/toast-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastHost],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<router-outlet /><app-toast-host />`,
})
export class App {
  // Instancié dès le démarrage pour appliquer le thème mémorisé.
  protected readonly theme = inject(ThemeService);
}
