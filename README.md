# SmartCampus

Plateforme de gestion et de réservation des salles du campus — projet fil rouge DevOps, ENSA Khouribga.

**Équipe :** Aya Hanaf · Douaa Ait Bella · Aymen Igri · Mustapha Fahmi
**Suivi du travail :** Jira, projet `SC`

## Lancer le projet

Prérequis : Java 21, Maven 3.9+.

```bash
mvn clean install          # compile, teste et produit target/smartcampus-0.0.1-SNAPSHOT.jar
java -jar target/smartcampus-0.0.1-SNAPSHOT.jar
```

L'API démarre sur le port **8090** :
- `GET http://localhost:8090/api/health` → `{"status":"UP", ...}`
- Console H2 (profil dev) : `http://localhost:8090/h2-console` (JDBC URL : `jdbc:h2:mem:smartcampus`)

Profil production (PostgreSQL) :
```bash
DB_URL=jdbc:postgresql://host:5432/smartcampus DB_USER=... DB_PASSWORD=... \
  java -jar target/smartcampus-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Organisation du code

```
src/main/java/ma/ensak/smartcampus/
├── controller/   # endpoints REST (/api/...)
├── service/      # logique métier
├── repository/   # accès base de données (Spring Data JPA)
├── model/        # entités JPA (User, Room, Booking...)
├── dto/          # objets échangés avec le client
└── config/       # configuration (sécurité, CORS...)
```

## Convention Git (traçabilité Jira → code)

Chaque modification est liée à un ticket Jira `SC-<n>`.

| Élément | Format | Exemple |
|---|---|---|
| Branche | `feature/SC-<n>-<description>` | `feature/SC-9-login` |
| Correctif | `fix/SC-<n>-<description>` | `fix/SC-13-message-echecs` |
| Commit | `SC-<n> <message>` | `SC-9 Ajoute la validation des identifiants` |
| Pull Request | `SC-<n> — <titre>` | `SC-9 — Implémenter la connexion utilisateur` |

Règles :
- On ne pousse jamais directement sur `main` : on passe par une Pull Request.
- Chaque Pull Request est relue par au moins un autre membre avant fusion.
- Le build Jenkins doit être vert avant la fusion.

## Répartition par domaine

| Domaine (Epic Jira) | Responsable |
|---|---|
| AUTH (SC-6) + NOTIF (SC-20) | Douaa |
| ROOM (SC-7) + ADMIN (SC-19) | Aymen |
| BOOK (SC-8) + DEVOPS (SC-22) | Aya |
| UI — frontend (SC-21) | Mustapha |
