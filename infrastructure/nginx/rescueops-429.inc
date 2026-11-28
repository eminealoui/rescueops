# Réponse 429 au format ProblemDetail (comme les erreurs des microservices).
default_type application/problem+json;
charset utf-8;
add_header Retry-After 1 always;
include /etc/nginx/conf.d/rescueops-headers.inc;
return 429 '{"type":"about:blank","title":"Trop de requêtes","status":429,"detail":"Limite de débit dépassée : réessayez dans un instant.","code":"TROP_DE_REQUETES"}';
