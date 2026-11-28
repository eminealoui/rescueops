# En-têtes de sécurité envoyés avec chaque réponse (inclus dans chaque location :
# avec Nginx, un add_header dans une location annule ceux du niveau server).

# CSP : le navigateur n'exécute que ce qui vient de ces sources.
#  - script-src : nos fichiers + 2 petits scripts inline d'index.html autorisés par leur empreinte
#    (thème clair/sombre et chargement des feuilles de style). Si index.html change, recalculer l'empreinte.
#  - style-src 'unsafe-inline' : Angular injecte les styles de ses composants dans des balises <style>.
#  - Keycloak (localhost:8180) : appels du jeton et formulaire de connexion.
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'sha256-UjCU9zJj37SkFISUjAWmGzU6RVZX+6CM+MciibeCZ/w=' 'sha256-LMY6wYoFV9I4wWzxaq1N/dTpl4iurQktw706UCHK3vM='; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://tile.openstreetmap.org https://*.tile.openstreetmap.org; connect-src 'self' http://localhost:8180; form-action 'self' http://localhost:8180; frame-ancestors 'none'; base-uri 'self'; object-src 'none'" always;

# Interdit d'afficher RescueOps dans une iframe d'un autre site (clickjacking).
add_header X-Frame-Options "DENY" always;
# Le navigateur ne devine pas le type d'un fichier (un .txt ne devient pas un script).
add_header X-Content-Type-Options "nosniff" always;
# L'adresse complète de la page n'est pas transmise aux autres sites.
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
# Fonctions du navigateur inutiles à l'application : coupées.
add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=(), usb=()" always;
add_header Cross-Origin-Opener-Policy "same-origin" always;

# Pas de HSTS : il s'applique au nom "localhost" sur TOUS les ports, il forcerait donc aussi
# Keycloak (http://localhost:8180) et ng serve (http://localhost:4200) en HTTPS et les casserait.
# En production, sur un vrai nom de domaine : add_header Strict-Transport-Security "max-age=31536000" always;
