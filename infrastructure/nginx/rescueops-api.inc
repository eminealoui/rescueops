# Relais /api/** vers les Gateways (inclus par les deux blocs server).

# Rate limit (zones dans rescueops-ratelimit.conf) ; réponse 429 en JSON.
limit_req  zone=api_par_ip burst=40 nodelay;
limit_conn connexions_par_ip 50;
error_page 429 = @trop_de_requetes;

proxy_pass http://gateways;
proxy_http_version 1.1;
proxy_set_header Connection "";
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;

# Bascule : erreur réseau, délai dépassé ou Gateway indisponible -> essai sur l'autre.
# Les POST/PUT ne sont jamais rejoués (pas de double création).
proxy_next_upstream error timeout http_502 http_503 http_504;
proxy_next_upstream_tries 2;
proxy_connect_timeout 2s;

# Démo : quelle instance de Gateway a répondu (adresse interne Docker).
add_header X-Gateway-Instance $upstream_addr always;
# Les en-têtes de sécurité de la Gateway sont remplacés par ceux de Nginx (pas de doublon).
proxy_hide_header X-Frame-Options;
proxy_hide_header X-Content-Type-Options;
proxy_hide_header Content-Security-Policy;
proxy_hide_header Referrer-Policy;
proxy_hide_header Strict-Transport-Security;
include /etc/nginx/conf.d/rescueops-headers.inc;
