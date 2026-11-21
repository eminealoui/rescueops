# 🚑 ms-training : Module 6, Formations, certifications et simulations

> **Ton guide personnel.** Suis les étapes **dans l'ordre**, sans en sauter.
> Chaque étape dit **quoi faire**, puis **ce que tu dois voir** ✅.
> Si ce que tu vois est différent, **arrête-toi** et va voir la section 🆘 en bas.

---

## 📇 Ta fiche d'identité

| | |
|---|---|
| **Nom du service** (Eureka) | `ms-training` |
| **Port** | `8086` |
| **Adresse de ton API** (via la Gateway) | `http://localhost:8080/api/trainings/...` |
| **Ta base MySQL** | `training_db` (port **3307**) |
| **Ton compte MySQL** | `training_svc` / `dev_training` |
| **Ton package Java** | `tn.esprit.rescueops.training` |
| **Ton compte de test** | `formateur@rescueops.test` / `Demo1234!` (rôle FORMATEUR) |

**Ce que fait ton module :**
- BF-16 : créer formations, sessions, inscriptions et prérequis
- BF-17 : évaluer les apprenants et délivrer les certifications valides
- BF-18 : exécuter des scénarios simulés et calculer les résultats

**Classes essentielles (diagramme UML) :** Formation, SessionFormation, Evaluation, Certification, ScenarioSimulation
**Le défi technique :** Scoring des évaluations, gestion des prérequis et validité des certifications.
**Les écrans Angular (plus tard) :** Catalogue ; gestion de sessions ; espace apprenant ; quiz ; simulateur ; certificats.
**Les rôles qui utilisent ton module :** FORMATEUR (formations, sessions, évaluations, certifications) · APPRENANT (s'inscrit, passe les évaluations) · ADMIN

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
git checkout -b feature/ms-training
```

✅ **Tu dois voir :** `Switched to a new branch 'feature/ms-training'`.

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

✅ **Tu dois voir :** 5 lignes (`mysql`, `keycloak`, `rabbitmq`, `discovery-server`, `api-gateway`), toutes en **`(healthy)`**. Keycloak peut mettre une minute.

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
3. Dans l'arborescence, ouvre `services/ms-training`.

✅ **Tu dois voir :** `services/ms-training/src/main/java/tn/esprit/rescueops/training/TrainingServiceApplication.java`.

---

## Étape 8 : lancer TON service

**Dans IntelliJ (recommandé) :**
1. Ouvre `TrainingServiceApplication.java`.
2. Clique sur la **flèche verte ▶** à gauche de `public class TrainingServiceApplication` → **Modify Run Configuration...**
3. Dans **Active profiles**, écris : `local`. Clique **OK**.
4. Clique sur ▶ **Run**.

**Ou dans un terminal :**

```powershell
.\mvnw.cmd -f services\ms-training spring-boot:run "-Dspring-boot.run.profiles=local"
```

✅ **Tu dois voir**, à la fin des logs : `Started TrainingServiceApplication in ... seconds`.

> ⚠️ Le profil **`local`** est obligatoire : sans lui, la Gateway (dans Docker) ne trouvera pas ton service.

---

## Étape 9 : vérifier que ton service est inscrit dans Eureka

Rafraîchis **http://localhost:8761**.

✅ **Tu dois voir :** une nouvelle ligne **`MS-TRAINING`** au statut **UP**, avec l'adresse `host.docker.internal:8086`.

---

## Étape 10 : appeler ton service à travers la Gateway

Ouvre un **deuxième** terminal PowerShell dans `rescueops`.

**1. Sans badge, la porte est fermée :**

```powershell
curl.exe -i http://localhost:8080/api/trainings/ping
```

✅ **Tu dois voir :** `HTTP/1.1 401` et `"code":"NON_AUTHENTIFIE"`.

**2. Prends un badge (token) avec ton compte de test :**

```powershell
$TOKEN = powershell -ExecutionPolicy Bypass -File .\scripts\get-token.ps1 formateur@rescueops.test
```

**3. Appelle ton service avec le badge :**

```powershell
curl.exe -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/trainings/ping
```

✅ **Tu dois voir :**

```json
{"service":"ms-training","statut":"UP","utilisateur":"<ton identifiant Keycloak>","roles":["ROLE_FORMATEUR","ROLE_CITOYEN"]}
```

🎉 **Toute la chaîne fonctionne :** Keycloak → Gateway → Eureka → **ton service**. Tu peux commencer à coder.

> Si tu obtiens `503 SERVICE_INDISPONIBLE`, attends 30 secondes et réessaie : la Gateway met sa liste à jour toutes les 30 secondes.

---

## Étape 11 : coder ta première fonctionnalité, couche par couche

On prend l'exemple de **Formation**. Fais exactement ces 8 petites étapes, puis refais-les pour tes autres classes (Formation, SessionFormation, Evaluation, Certification, ScenarioSimulation).

Tous les fichiers vont dans `services/ms-training/src/main/java/tn/esprit/rescueops/training/`, dans ces dossiers :

```
training/
├── domain/      les entités JPA et les enums
├── repository/  l'accès à la base
├── dto/         ce qui entre et sort de l'API (jamais l'entité !)
├── service/     les règles métier
└── web/         les contrôleurs REST (PingController y est déjà)
```

### 11.1 La table (Flyway)

Crée **`src/main/resources/db/migration/V2__creation_formation.sql`** :

```sql
CREATE TABLE formation (
    id          BINARY(16)   NOT NULL PRIMARY KEY,
    prerequis   VARCHAR(500) NOT NULL,
    statut      VARCHAR(30)  NOT NULL,
    cree_par    VARCHAR(36)  NOT NULL,   -- le "sub" Keycloak de l'utilisateur
    cree_le     DATETIME(6)  NOT NULL,
    version     BIGINT       NOT NULL
);
```

> ⚠️ Ne modifie **jamais** un fichier `V...sql` déjà exécuté. Pour changer une table, crée `V3__...sql`.
> ⚠️ **Jamais de clé étrangère vers la base d'un autre service.** Stocke seulement l'identifiant (ex. `mission_id BINARY(16)`).

### 11.2 L'enum : `domain/StatutFormation.java`

```java
package tn.esprit.rescueops.training.domain;

public enum StatutFormation {
    BROUILLON, PUBLIEE, ARCHIVEE
}
```

### 11.3 L'entité : `domain/Formation.java`

```java
package tn.esprit.rescueops.training.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "formation")
@Getter
@Setter
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "prerequis", nullable = false, length = 500)
    private String prerequis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutFormation statut;

    @Column(name = "cree_par", nullable = false, length = 36)
    private String creePar;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Version
    private long version;
}
```

### 11.4 Le repository : `repository/FormationRepository.java`

```java
package tn.esprit.rescueops.training.repository;

import tn.esprit.rescueops.training.domain.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, UUID> {
}
```

### 11.5 Les DTO : `dto/FormationRequest.java` et `dto/FormationResponse.java`

```java
package tn.esprit.rescueops.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FormationRequest(@NotBlank @Size(max = 500) String prerequis) {
}
```

```java
package tn.esprit.rescueops.training.dto;

import tn.esprit.rescueops.training.domain.StatutFormation;

import java.time.Instant;
import java.util.UUID;

public record FormationResponse(UUID id, String prerequis, StatutFormation statut, Instant creeLe) {
}
```

### 11.6 Le service : `service/FormationService.java`

```java
package tn.esprit.rescueops.training.service;

import tn.esprit.rescueops.training.domain.Formation;
import tn.esprit.rescueops.training.domain.StatutFormation;
import tn.esprit.rescueops.training.dto.FormationRequest;
import tn.esprit.rescueops.training.dto.FormationResponse;
import tn.esprit.rescueops.training.repository.FormationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;

import java.time.Instant;
import java.util.UUID;

@Service
public class FormationService {

    private final FormationRepository repository;

    public FormationService(FormationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FormationResponse creer(FormationRequest request) {
        Formation formation = new Formation();
        formation.setPrerequis(request.prerequis());
        formation.setStatut(StatutFormation.BROUILLON);
        formation.setCreePar(CurrentUser.current().orElseThrow().keycloakId());   // identité tirée du TOKEN
        formation.setCreeLe(Instant.now());
        return toResponse(repository.save(formation));
    }

    @Transactional(readOnly = true)
    public FormationResponse trouver(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Formation introuvable"));   // → 404 automatique
    }

    private FormationResponse toResponse(Formation x) {
        return new FormationResponse(x.getId(), x.getPrerequis(), x.getStatut(), x.getCreeLe());
    }
}
```

### 11.7 Le contrôleur : `web/FormationController.java`

```java
package tn.esprit.rescueops.training.web;

import tn.esprit.rescueops.training.dto.FormationRequest;
import tn.esprit.rescueops.training.dto.FormationResponse;
import tn.esprit.rescueops.training.service.FormationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/trainings/formations")
public class FormationController {

    private final FormationService service;

    public FormationController(FormationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FORMATEUR', 'ADMIN')")
    public FormationResponse creer(@Valid @RequestBody FormationRequest request) {
        return service.creer(request);
    }

    @GetMapping("/{id}")
    public FormationResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }
}
```

### 11.8 Relancer et tester

1. **Arrête** ton service (carré rouge ■ dans IntelliJ), puis **relance-le** (▶). Flyway crée ta table au démarrage.
2. Dans le terminal, reprends un badge (il expire après 15 minutes) :

```powershell
$TOKEN = powershell -ExecutionPolicy Bypass -File .\scripts\get-token.ps1 formateur@rescueops.test
```

   Puis crée ton premier élément :

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/trainings/formations -Headers @{ Authorization = "Bearer $TOKEN" } -ContentType "application/json; charset=utf-8" -Body '{"prerequis":"Premier essai"}'
```

✅ **Tu dois voir :** un tableau avec les colonnes `id`, `prerequis` (Premier essai), `statut` (BROUILLON) et `creeLe`.

> 💡 Pour envoyer du JSON, utilise `Invoke-RestMethod` plutôt que `curl.exe` : avec PowerShell 5.1, `curl.exe -d` casse le JSON dès qu'il contient une espace.

3. **Teste une erreur**, avec un texte vide :

```powershell
curl.exe -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{\"prerequis\":\"\"}' http://localhost:8080/api/trainings/formations
```

   ✅ **Tu dois voir :** `"status":400`, `"code":"VALIDATION"` et `"errors":[{"field":"prerequis",...}]`. Tu n'as rien codé pour ça : c'est `rescueops-common` qui le gère.

4. **Regarde ta table :** phpMyAdmin (serveur *RescueOps Docker 3307*) ou IntelliJ **Database**, avec `training_svc` / `dev_training`.
   ✅ **Tu dois voir :** ta ligne dans `formation`, et la table `flyway_schema_history` avec V1 et V2.

5. **Documentation automatique :** ouvre http://localhost:8086/swagger-ui.html.

---

## Étape 12 : prévenir les autres modules (RabbitMQ)

Tes événements :
- 📤 `certification.maj` (une certification est délivrée ou expire)
- 📥 aucun pour l'instant

**Publier un événement** (dans ton service, après avoir sauvegardé) :

```java
// champ à ajouter dans ton service :  private final RabbitTemplate rabbitTemplate;   (+ constructeur)
rabbitTemplate.convertAndSend("certification.maj",
        RescueOpsEvent.of("certification.maj", Map.of("formationId", formation.getId().toString())));
```

**Écouter un événement** (une classe `event/...Listener.java`) :

```java
@Configuration
class MessagingConfig {
    @Bean Queue maFile() { return QueueBuilder.durable("ms-training.evenements").build(); }
    @Bean Binding liaison(Queue maFile, TopicExchange rescueOpsEventsExchange) {
        return BindingBuilder.bind(maFile).to(rescueOpsEventsExchange).with("cle.a.ecouter");
    }
}

@Component
class EvenementListener {
    @RabbitListener(queues = "ms-training.evenements")
    void recevoir(RescueOpsEvent<Map<String, Object>> event) {
        // ⚠️ un message peut arriver 2 fois : vérifie event.eventId() avant d'agir
    }
}
```

✅ **Pour vérifier :** dans RabbitMQ (http://localhost:15672), onglet **Queues**, ta file `ms-training.evenements` apparaît.

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
.\mvnw.cmd -f services\ms-training test
```

✅ **Tu dois voir :** `Tests run: ..., Failures: 0, Errors: 0` puis `BUILD SUCCESS`.

---

## Étape 14 : partager ton travail

```powershell
git add services/ms-training
```
```powershell
git commit -m "feat(training): création de Formation"
```
```powershell
git push -u origin feature/ms-training
```

Puis, sur GitHub, clique **Compare & pull request** et choisis la base **`develop`**. Un membre de l'équipe relit, puis on fusionne.

✅ **Tu dois voir :** ta Pull Request ouverte vers `develop`.

---

## 📏 Les règles d'or

| ✅ À faire | ❌ Interdit |
|---|---|
| Tout coder dans `services/ms-training` | Modifier la Gateway, `rescueops-common`, le realm ou le docker-compose sans l'accord de l'équipe |
| Garder le port `8086`, le nom `ms-training` et le préfixe `/api/trainings` | Changer le port ou le nom (la Gateway ne te trouverait plus) |
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
| `Port 8086 was already in use` | Ton service tourne déjà : arrête l'autre instance. |
| Erreurs rouges dans IntelliJ | Panneau Maven → **Reload All Maven Projects**. |

Bloqué ? Envoie à l'équipe la commande lancée, la **dernière ligne d'erreur**, et le `correlationId` si la réponse en contient un.
