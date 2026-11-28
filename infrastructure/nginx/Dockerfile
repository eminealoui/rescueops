# =====================================================================
#  Load balancer RescueOps : Nginx devant les 2 instances de la Gateway.
#  openssl sert à créer un certificat auto-signé si aucun n'est fourni.
# =====================================================================
FROM nginx:1.29-alpine
RUN apk add --no-cache openssl
COPY 20-certificats.sh /docker-entrypoint.d/20-certificats.sh
COPY rescueops-lb.conf /etc/nginx/conf.d/default.conf
COPY rescueops-api.inc rescueops-headers.inc rescueops-429.inc /etc/nginx/conf.d/
COPY 00-rescueops-http.conf /etc/nginx/conf.d/00-rescueops-http.conf
RUN chmod +x /docker-entrypoint.d/20-certificats.sh
EXPOSE 80 443
HEALTHCHECK --interval=10s --timeout=3s --retries=6 CMD wget -q -O /dev/null http://127.0.0.1/nginx-health || exit 1
