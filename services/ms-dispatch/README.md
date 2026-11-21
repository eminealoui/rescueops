# 🚑 ms-dispatch : Module 3, Dispatching intelligent

> **Ton guide personnel.** Suis les étapes **dans l'ordre**, sans en sauter.
> Chaque étape dit **quoi faire**, puis **ce que tu dois voir** ✅.
> Si ce que tu vois est différent, **arrête-toi** et va voir la section 🆘 en bas.

---

## 📇 Ta fiche d'identité

| | |
|---|---|
| **Nom du service** (Eureka) | `ms-dispatch` |
| **Port** | `8083` |
| **Adresse de ton API** (via la Gateway) | `http://localhost:8080/api/dispatch/...` |
| **Ta base MySQL** | `dispatch_db` (port **3307**) |
| **Ton compte MySQL** | `dispatch_svc` / `dev_dispatch` |
| **Ton package Java** | `tn.esprit.rescueops.dispatch` |
| **Ton compte de test** | `operateur@rescueops.test` / `Demo1234!` (rôle OPERATEUR) |

**Ce que fait ton module :**
- BF-07 : calculer et présenter un classement de ressources compatibles
- BF-08 : permettre à l'opérateur de valider une affectation proposée
- BF-09 : gérer les conflits de réservation et proposer une autre ressource

**Classes essentielles (diagramme UML) :** Recommandation, Affectation
**Le défi technique :** Algorithme de scoring multicritère (distance, disponibilité, compétences) avec justification et arbitrage humain.
**Les écrans Angular (plus tard) :** File des urgences ; recommandations classées ; carte ; validation des affectations.
**Les rôles qui utilisent ton module :** OPERATEUR (consulte les recommandations, valide les affectations) · ADMIN

---

## Étape 1 : installer les outils (une seule fois)

Installe Java 21 (Temurin), Docker Desktop, Git, IntelliJ IDEA et Node.js 22. **Maven n'est pas nécessaire.**

Ouvre **PowerShell** et vérifie :

```powershell
java -version
```
```powershell
docker --version
```
```powershell
git --version
```

✅ **Tu dois voir :** `version "21...` pour Java, et un numéro de version pour Docker et Git.

---

## Étape 2 : récupérer le projet

```powershell
git clone https://github.com/eminealoui/rescueops.git
```
```powershell
cd rescueops
```
```powershell
git checkout develop
```

Crée **ta branche** (tu ne travailles jamais directement sur `develop`) :

```powershell
git checkout -b feature/ms-dispatch
```

✅ **Tu dois voir :** `Switched to a new branch 'feature/ms-dispatch'`.

> ⚠️ **À partir d'ici, toutes les commandes se tapent dans le dossier `rescueops`.**

---

## Étape 3 : créer ton fichier de configuration

```powershell
copy .env.example .env
```

✅ **Tu dois voir :** `1 file(s) copied.` Ce fichier contient les mots de passe de **développement**. Il reste sur ton PC et n'est jamais envoyé sur GitHub.

---

## Étape 4 : allumer le socle (Keycloak, Gateway, Eureka, MySQL, RabbitMQ)

1. Lance **Docker Desktop** et attends que la baleine soit verte.
2. Démarre le socle. La première fois, compte 3 à 5 minutes :

```powershell
docker compose -f docker-compose.infra.yml up -d --build
```

3. Vérifie son état :

```powershell
docker compose -f docker-compose.infra.yml ps
```

✅ **Tu dois voir :** 6 lignes (`mysql`, `keycloak`, `rabbitmq`, `discovery-server`, `api-gateway`, `ms-user`), toutes en **`(healthy)`**. Keycloak peut mettre une minute.

> 💡 Les jours suivants : `docker compose -f docker-compose.infra.yml up -d` (sans `--build`).
> Pour éteindre : `docker compose -f docker-compose.infra.yml down`. **Jamais `-v`**, qui efface toutes les données.

---

## Étape 5 : visiter le socle dans ton navigateur

| Ouvre | Identifiants | ✅ Tu dois voir |
|---|---|---|
| http://localhost:8761 | aucun | **Eureka** : la ligne `API-GATEWAY` au statut UP |
| http://localhost:8180 | `admin` / `dev_keycloak` | **Keycloak**. Choisis le realm **rescueops** : 8 rôles et 8 utilisateurs |
| http://localhost:15672 | `rescueops` / `dev_rabbitmq` | **RabbitMQ**, onglet Exchanges : `rescueops.events` |

Le message rouge « SELF PRESERVATION MODE IS TURNED OFF » dans Eureka est **normal** en développement.

---

## Étape 6 : compiler tout le projet (une seule fois)

```powershell
.\mvnw.cmd install -DskipTests
```

✅ **Tu dois voir :** `BUILD SUCCESS`. Le premier lancement télécharge Maven et les bibliothèques, ce qui prend quelques minutes.

---

## Étape 7 : ouvrir le projet dans IntelliJ

1. **File → Open** → choisis le dossier `rescueops` (celui qui contient `pom.xml`) → **Open as Project** → **Trust Project**.
2. Attends la fin de l'import Maven (barre de progression en bas).
3. Dans l'arborescence, ouvre `services/ms-dispatch`.

✅ **Tu dois voir :** `services/ms-dispatch/src/main/java/tn/esprit/rescueops/dispatch/DispatchServiceApplication.java`.

---

## Étape 8 : lancer TON service

**Dans IntelliJ (recommandé) :**
1. Ouvre `DispatchServiceApplication.java`.
2. Clique sur la **flèche verte ▶** à gauche de `public class DispatchServiceApplication` → **Modify Run Configuration...**
3. Dans **Active profiles**, écris : `local`. Clique **OK**.
4. Clique sur ▶ **Run**.

**Ou dans un terminal :**

```powershell
.\mvnw.cmd -f services\ms-dispatch spring-boot:run "-Dspring-boot.run.profiles=local"
```

✅ **Tu dois voir**, à la fin des logs : `Started DispatchServiceApplication in ... seconds`.

> ⚠️ Le profil **`local`** est obligatoire : sans lui, la Gateway (dans Docker) ne trouvera pas ton service.

---

## Étape 9 : vérifier que ton service est inscrit dans Eureka

Rafraîchis **http://localhost:8761**.

✅ **Tu dois voir :** une nouvelle ligne **`MS-DISPATCH`** au statut **UP**, avec l'adresse `host.docker.internal:8083`.

---

## Étape 10 : appeler ton service à travers la Gateway

Ouvre un **deuxième** terminal PowerShell dans `rescueops`.

**1. Sans badge, la porte est fermée :**

```powershell
curl.exe -i http://localhost:8080/api/dispatch/ping
```

✅ **Tu dois voir :** `HTTP/1.1 401` et `"code":"NON_AUTHENTIFIE"`.

**2. Prends un badge (token) avec ton compte de test :**

```powershell
$TOKEN = powershell -ExecutionPolicy Bypass -File .\scripts\get-token.ps1 operateur@rescueops.test
```

**3. Appelle ton service avec le badge :**

```powershell
curl.exe -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/dispatch/ping
```

✅ **Tu dois voir :**

```json
{"service":"ms-dispatch","statut":"UP","utilisateur":"<ton identifiant Keycloak>","roles":["ROLE_OPERATEUR","ROLE_CITOYEN"]}
```

🎉 **Toute la chaîne fonctionne :** Keycloak → Gateway → Eureka → **ton service**. Tu peux commencer à coder.

> Si tu obtiens `503 SERVICE_INDISPONIBLE`, attends 30 secondes et réessaie : la Gateway met sa liste à jour toutes les 30 secondes.

---

## Étape 11 : coder ta première fonctionnalité, couche par couche

On prend l'exemple de **Affectation**. Fais exactement ces 8 petites étapes, puis refais-les pour tes autres classes (Recommandation, Affectation).

Tous les fichiers vont dans `services/ms-dispatch/src/main/java/tn/esprit/rescueops/dispatch/`, dans ces dossiers :

```
dispatch/
├── domain/      les entités JPA et les enums
├── repository/  l'accès à la base
├── dto/         ce qui entre et sort de l'API (jamais l'entité !)
├── service/     les règles métier
└── web/         les contrôleurs REST (PingController y est déjà)
```

### 11.1 La table (Flyway)

Crée **`src/main/resources/db/migration/V2__creation_affectation.sql`** :

```sql
CREATE TABLE affectation (
    id          BINARY(16)   NOT NULL PRIMARY KEY,
    justification VARCHAR(500) NOT NULL,
    statut      VARCHAR(30)  NOT NULL,
    cree_par    VARCHAR(36)  NOT NULL,   -- le "sub" Keycloak de l'utilisateur
    cree_le     DATETIME(6)  NOT NULL,
    version     BIGINT       NOT NULL
);
```

> ⚠️ Ne modifie **jamais** un fichier `V...sql` déjà exécuté. Pour changer une table, crée `V3__...sql`.
> ⚠️ **Jamais de clé étrangère vers la base d'un autre service.** Stocke seulement l'identifiant (ex. `mission_id BINARY(16)`).

### 11.2 L'enum : `domain/StatutAffectation.java`

```java
package tn.esprit.rescueops.dispatch.domain;

public enum StatutAffectation {
    PROPOSEE, CONFIRMEE, ANNULEE
}
```

### 11.3 L'entité : `domain/Affectation.java`

```java
package tn.esprit.rescueops.dispatch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "affectation")
@Getter
@Setter
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "justification", nullable = false, length = 500)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutAffectation statut;

    @Column(name = "cree_par", nullable = false, length = 36)
    private String creePar;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Version
    private long version;
}
```

### 11.4 Le repository : `repository/AffectationRepository.java`

```java
package tn.esprit.rescueops.dispatch.repository;

import tn.esprit.rescueops.dispatch.domain.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AffectationRepository extends JpaRepository<Affectation, UUID> {
}
```

### 11.5 Les DTO : `dto/AffectationRequest.java` et `dto/AffectationResponse.java`

```java
package tn.esprit.rescueops.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AffectationRequest(@NotBlank @Size(max = 500) String justification) {
}
```

```java
package tn.esprit.rescueops.dispatch.dto;

import tn.esprit.rescueops.dispatch.domain.StatutAffectation;

import java.time.Instant;
import java.util.UUID;

public record AffectationResponse(UUID id, String justification, StatutAffectation statut, Instant creeLe) {
}
```

### 11.6 Le service : `service/AffectationService.java`

```java
package tn.esprit.rescueops.dispatch.service;

import tn.esprit.rescueops.dispatch.domain.Affectation;
import tn.esprit.rescueops.dispatch.domain.StatutAffectation;
import tn.esprit.rescueops.dispatch.dto.AffectationRequest;
import tn.esprit.rescueops.dispatch.dto.AffectationResponse;
import tn.esprit.rescueops.dispatch.repository.AffectationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;

import java.time.Instant;
import java.util.UUID;

@Service
public class AffectationService {

    private final AffectationRepository repository;

    public AffectationService(AffectationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AffectationResponse creer(AffectationRequest request) {
        Affectation affectation = new Affectation();
        affectation.setJustification(request.justification());
        affectation.setStatut(StatutAffectation.PROPOSEE);
        affectation.setCreePar(CurrentUser.current().orElseThrow().keycloakId());   // identité tirée du TOKEN
        affectation.setCreeLe(Instant.now());
        return toResponse(repository.save(affectation));
    }

    @Transactional(readOnly = true)
    public AffectationResponse trouver(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Affectation introuvable"));   // → 404 automatique
    }

    private AffectationResponse toResponse(Affectation x) {
        return new AffectationResponse(x.getId(), x.getJustification(), x.getStatut(), x.getCreeLe());
    }
}
```

### 11.7 Le contrôleur : `web/AffectationController.java`

```java
package tn.esprit.rescueops.dispatch.web;

import tn.esprit.rescueops.dispatch.dto.AffectationRequest;
import tn.esprit.rescueops.dispatch.dto.AffectationResponse;
import tn.esprit.rescueops.dispatch.service.AffectationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/dispatch/affectations")
public class AffectationController {

    private final AffectationService service;

    public AffectationController(AffectationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public AffectationResponse creer(@Valid @RequestBody AffectationRequest request) {
        return service.creer(request);
    }

    @GetMapping("/{id}")
    public AffectationResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }
}
```

### 11.8 Relancer et tester

1. **Arrête** ton service (carré rouge ■ dans IntelliJ), puis **relance-le** (▶). Flyway crée ta table au démarrage.
2. Dans le terminal, reprends un badge (il expire après 15 minutes) :

```powershell
$TOKEN = powershell -ExecutionPolicy Bypass -File .\scripts\get-token.ps1 operateur@rescueops.test
```

   Puis crée ton premier élément :

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/dispatch/affectations -Headers @{ Authorization = "Bearer $TOKEN" } -ContentType "application/json; charset=utf-8" -Body '{"justification":"Premier essai"}'
```

✅ **Tu dois voir :** un tableau avec les colonnes `id`, `justification` (Premier essai), `statut` (PROPOSEE) et `creeLe`.

> 💡 Pour envoyer du JSON, utilise `Invoke-RestMethod` plutôt que `curl.exe` : avec PowerShell 5.1, `curl.exe -d` casse le JSON dès qu'il contient une espace.

3. **Teste une erreur**, avec un texte vide :

```powershell
curl.exe -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{\"justification\":\"\"}' http://localhost:8080/api/dispatch/affectations
```

   ✅ **Tu dois voir :** `"status":400`, `"code":"VALIDATION"` et `"errors":[{"field":"justification",...}]`. Tu n'as rien codé pour ça : c'est `rescueops-common` qui le gère.

4. **Regarde ta table :** phpMyAdmin (serveur *RescueOps Docker 3307*) ou IntelliJ **Database**, avec `dispatch_svc` / `dev_dispatch`.
   ✅ **Tu dois voir :** ta ligne dans `affectation`, et la table `flyway_schema_history` avec V1 et V2.

5. **Documentation automatique :** ouvre http://localhost:8083/swagger-ui.html.

---

## Étape 12 : prévenir les autres modules (RabbitMQ)

Tes événements :
- 📤 `affectation.confirmee` (une ambulance est affectée à un incident)
- 📥 `incident.priorise` (venant de ms-emergency : calculer des recommandations)

**Appel direct :** Appel REST (OpenFeign) vers ms-resource pour vérifier et réserver une ambulance : c'est un appel synchrone, car tu as besoin de la réponse tout de suite.

**Publier un événement** (dans ton service, après avoir sauvegardé) :

```java
// champ à ajouter dans ton service :  private final RabbitTemplate rabbitTemplate;   (+ constructeur)
rabbitTemplate.convertAndSend("affectation.confirmee",
        RescueOpsEvent.of("affectation.confirmee", Map.of("affectationId", affectation.getId().toString())));
```

**Écouter un événement** (une classe `event/...Listener.java`) :

```java
@Configuration
class MessagingConfig {
    @Bean Queue maFile() { return QueueBuilder.durable("ms-dispatch.evenements").build(); }
    @Bean Binding liaison(Queue maFile, TopicExchange rescueOpsEventsExchange) {
        return BindingBuilder.bind(maFile).to(rescueOpsEventsExchange).with("incident.priorise");
    }
}

@Component
class EvenementListener {
    @RabbitListener(queues = "ms-dispatch.evenements")
    void recevoir(RescueOpsEvent<Map<String, Object>> event) {
        // ⚠️ un message peut arriver 2 fois : vérifie event.eventId() avant d'agir
    }
}
```

✅ **Pour vérifier :** dans RabbitMQ (http://localhost:15672), onglet **Queues**, ta file `ms-dispatch.evenements` apparaît.

> ⚠️ Pas de secrets ni de données personnelles inutiles dans un événement : des identifiants suffisent.

---

## Étape 13 : écrire tes tests

Copie le modèle `src/test/java/.../web/PingControllerTest.java`. Pour chaque endpoint, teste au moins :
- **sans token**, la réponse est `401` ;
- **avec un mauvais rôle**, la réponse est `403` ;
- **avec le bon rôle**, la réponse est `200` ou `201` ;
- **avec une donnée invalide**, la réponse est `400` ;
- **pour une ressource absente**, la réponse est `404`.

```powershell
.\mvnw.cmd -f services\ms-dispatch test
```

✅ **Tu dois voir :** `Tests run: ..., Failures: 0, Errors: 0` puis `BUILD SUCCESS`.

---

## Étape 14 : partager ton travail

```powershell
git add services/ms-dispatch
```
```powershell
git commit -m "feat(dispatch): création de Affectation"
```
```powershell
git push -u origin feature/ms-dispatch
```

Puis, sur GitHub, clique **Compare & pull request** et choisis la base **`develop`**. Un membre de l'équipe relit, puis on fusionne.

✅ **Tu dois voir :** ta Pull Request ouverte vers `develop`.

---

## 📏 Les règles d'or

| ✅ À faire | ❌ Interdit |
|---|---|
| Tout coder dans `services/ms-dispatch` | Modifier la Gateway, `rescueops-common`, le realm ou le docker-compose sans l'accord de l'équipe |
| Garder le port `8083`, le nom `ms-dispatch` et le préfixe `/api/dispatch` | Changer le port ou le nom (la Gateway ne te trouverait plus) |
| `@PreAuthorize("hasRole('...')")` avec les 8 rôles officiels | Inventer un rôle |
| Identifier l'utilisateur avec `CurrentUser.current()` | Croire un identifiant envoyé par le navigateur |
| Exposer des DTO | Renvoyer une entité JPA |
| Lever `NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422) | Renvoyer des messages d'erreur faits maison |
| Créer les tables avec Flyway (`V2__`, `V3__`...) | `ddl-auto: update`, ou modifier un `V...sql` déjà exécuté |
| Appeler un autre service par son API ou écouter ses événements | Lire la base d'un autre service |

Les 8 rôles : `CITOYEN`, `OPERATEUR`, `GESTIONNAIRE_RESSOURCES`, `AMBULANCIER`, `COORDINATEUR_HOSPITALIER`, `FORMATEUR`, `APPRENANT`, `ADMIN`.

---

## 🆘 Dépannage

| Ce que tu vois | Solution |
|---|---|
| `docker-compose.infra.yml: The system cannot find the file` | Tu n'es pas dans le dossier `rescueops` : `cd` dedans. |
| Un service reste en `starting` ou `unhealthy` | Attends 1 à 2 minutes, puis `docker compose -f docker-compose.infra.yml logs <service>`. |
| `Communications link failure` au démarrage du service | Le socle n'est pas lancé (étape 4), ou tu utilises le port 3306 au lieu de **3307**. |
| `Validate failed: Migration checksum mismatch` | Tu as modifié un `V...sql` déjà exécuté : remets-le comme avant et crée un nouveau fichier. |
| `Schema-validation: missing table` | La table n'existe pas dans ta migration, ou son nom diffère de `@Table(name = ...)`. |
| La Gateway répond `503` pour ton service | Le service n'est pas UP dans Eureka, ou il est lancé **sans** le profil `local`. Attends 30 s. |
| `401` avec un token | Le token a expiré (15 min) : reprends-en un (étape 10). |
| `403 ACCES_REFUSE` | Ton compte n'a pas le rôle exigé par `@PreAuthorize`. |
| `Port 8083 was already in use` | Ton service tourne déjà : arrête l'autre instance. |
| Erreurs rouges dans IntelliJ | Panneau Maven → **Reload All Maven Projects**. |

Bloqué ? Envoie à l'équipe la commande lancée, la **dernière ligne d'erreur**, et le `correlationId` si la réponse en contient un.
