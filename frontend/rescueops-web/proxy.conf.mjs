// Proxy de développement : l'application appelle /api/** (même origine, pas de CORS),
// et ng serve relaie vers la Gateway. Le port est lu dans le .env racine (GATEWAY_HOST_PORT),
// 8080 par défaut : chaque membre de l'équipe garde le même code, seul son .env change.
import { existsSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const envFile = fileURLToPath(new URL('../../.env', import.meta.url));
let port = '8080';
if (existsSync(envFile)) {
  const ligne = readFileSync(envFile, 'utf8').split(/\r?\n/).find((l) => l.startsWith('GATEWAY_HOST_PORT='));
  if (ligne) port = ligne.split('=')[1].trim() || port;
}

export default {
  '/api': { target: `http://localhost:${port}`, changeOrigin: true, secure: false },
};
