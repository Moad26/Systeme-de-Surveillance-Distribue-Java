# Script de Parole - Soutenance Finale
## Système de Surveillance Distribué

**Durée totale : 10 minutes**

---

## INTERVENANT A — Chef de Projet / Architecte
*Slides 1-4 | Durée : ~3 min 30s*

---

### SLIDE 1 : Page de Titre (~20 secondes)

> *[Se lever, sourire, regard vers le jury]*

**Bonjour à tous,**

Nous sommes **[Membre A]**, **[Membre B]** et **[Membre C]**, et nous avons le plaisir de vous présenter aujourd'hui notre projet de **Système de Surveillance Distribué**.

Ce système permet le monitoring temps réel de parcs informatiques via une architecture multi-agents.

---

### SLIDE 2 : Sommaire (~15 secondes)

> *[Pointer brièvement les sections]*

Voici notre plan de présentation : nous commencerons par la **problématique**, puis l'**architecture globale**, ensuite le **focus technique** sur nos choix d'implémentation, l'**interface utilisateur**, une **démonstration live**, et enfin notre **conclusion**.

---

### SLIDE 3 : Problématique (~1 minute 15 secondes)

> *[Ton engagé, souligner les problèmes]*

**Pourquoi ce projet ?**

Dans les environnements IT modernes, les administrateurs système font face à plusieurs défis majeurs :

- Les **pannes système** peuvent coûter des milliers d'euros par heure d'indisponibilité
- La **détection tardive** des problèmes : on réagit souvent *après* l'incident
- Les données sont **éparpillées** sur différentes machines, rendant le diagnostic difficile
- Et surtout, le défi de **l'évolutivité** : comment surveiller des centaines de machines efficacement ?

**Notre solution** répond à ces besoins : une supervision **temps réel** et **centralisée**, avec des alertes **proactives** et configurables, le tout sur une architecture **distribuée** et **scalable**.

Nous surveillons trois métriques essentielles : le **CPU**, la **RAM** et le **Disque**, avec deux niveaux d'alerte : **WARNING** et **CRITICAL**.

---

### SLIDE 4 : Architecture Globale (~1 minute 30 secondes)

> *[Pointer le diagramme de gauche à droite]*

Passons maintenant à l'**architecture globale** de notre système.

Notre système suit un modèle **client-serveur distribué** à trois tiers :

**À gauche**, vous avez les **Agents**. Ce sont des collecteurs autonomes déployés sur chaque machine à surveiller. Ils collectent les métriques CPU, RAM et Disque toutes les **5 secondes**.

**Au centre**, le **Serveur Central**. C'est le cœur du système. Il contient le **registre RMI**, les **listeners UDP et TCP**, et le **DataManager** pour la gestion des données.

**À droite**, les **Clients UI**. Ce sont les interfaces JavaFX qui permettent aux administrateurs de visualiser les données en temps réel.

Concernant la **communication** :
- Les métriques sont envoyées via **UDP** — protocole rapide, adapté aux données périodiques
- Les alertes critiques utilisent **TCP** — protocole fiable qui garantit la livraison
- Et les clients UI communiquent avec le serveur via **RMI**, le Remote Method Invocation de Java

Le projet est organisé en **4 modules Maven** : `monitoring-common` pour les modèles partagés, `monitoring-agent`, `monitoring-server`, et `monitoring-ui`.

Je passe maintenant la parole à **[Membre B]** pour les détails techniques.

---

## INTERVENANT B — Backend Specialist
*Slides 5-7 | Durée : ~3 min 30s*

---

### SLIDE 5 : Focus Agent - Pattern Strategy & Threads (~1 minute 15 secondes)

> *[Pointer le code, expliquer les concepts]*

**Merci [Membre A].**

Parlons de l'**architecture de l'agent**, et particulièrement du **Pattern Strategy** que nous avons implémenté.

Nous avons défini une **interface `ICollector`** avec deux méthodes : `collect()` qui retourne la valeur de la métrique, et `getName()` pour identifier le collecteur.

Cette interface est implémentée par trois classes concrètes :
- **`CpuCollector`** qui utilise l'`OperatingSystemMXBean` pour obtenir la charge CPU
- **`MemoryCollector`** avec l'objet `Runtime` pour calculer l'usage mémoire
- Et **`DiskCollector`** qui interroge les `FileStore` du système

**L'avantage de ce pattern ?** L'**extensibilité**. Si demain on veut surveiller la température GPU ou le trafic réseau, on crée simplement une nouvelle classe qui implémente `ICollector`.

Pour la **planification périodique**, nous utilisons un `ScheduledExecutorService`. Toutes les 5 secondes, la méthode `collectAndSend` est exécutée. C'est **thread-safe**, **non-bloquant**, et le pool de threads est géré automatiquement.

---

### SLIDE 6 : Focus Communication - UDP vs TCP (~1 minute 15 secondes)

> *[Comparer les deux colonnes, ton didactique]*

Parlons maintenant de notre **stratégie de communication réseau**.

Nous avons fait un **choix délibéré** d'utiliser deux protocoles différents selon le type de données.

**Pour les métriques, nous utilisons UDP.** Pourquoi ?
- La **latence est minimale** : environ 1 milliseconde
- **Pas de handshake** : on envoie directement, overhead faible
- C'est **hautement scalable** : on peut gérer plus de 10 000 paquets par seconde
- Et si une métrique est perdue ? Ce n'est pas grave : la suivante arrive dans 5 secondes

Côté implémentation, la classe `UdpSender` sérialise les objets `Metric` et les envoie sur le port **9876**.

**Pour les alertes, nous utilisons TCP.** Pourquoi ?
- La **fiabilité est garantie** : chaque paquet reçoit un accusé de réception
- L'**ordre est préservé**
- L'**intégrité** des données est vérifiée par checksum
- Une alerte **CRITICAL ne doit jamais être perdue** — c'est non négociable

Le `TcpAlertHandler` côté serveur utilise un **ThreadPool** pour gérer plusieurs connexions simultanées sur le port **9877**.

---

### SLIDE 7 : Focus Serveur - Service RMI (~1 minute)

> *[Pointer l'interface RMI, les structures de données]*

Côté serveur, nous exposons un **service RMI** via l'interface `IMonitoringService`.

Cette interface définit toutes les opérations que le client UI peut invoquer à distance :
- `getActiveAgents()` pour lister les agents connectés
- `getMetrics()` et `getAlerts()` pour récupérer les données
- `getStatistics()` pour les calculs statistiques
- Et `authenticate()` pour la connexion sécurisée

**Pourquoi RMI ?** L'appel de méthode est **transparent** — le code client est identique à un appel local. La sérialisation est **automatique**, et c'est **intégré à Java** sans dépendance externe.

Pour la **gestion de la concurrence**, nous utilisons des structures **thread-safe** :
- `ConcurrentHashMap` pour les métriques — accès concurrent efficace
- `CopyOnWriteArrayList` pour les alertes — optimisé pour les lectures fréquentes

La **persistance** est assurée en **JSON** : fichiers de métriques par agent, alertes, configurations, et utilisateurs avec mots de passe hachés en SHA-256.

Je passe la parole à **[Membre C]** pour l'interface utilisateur.

---

## INTERVENANT C — Frontend & Démo
*Slides 8-12 | Durée : ~3 min*

---

### SLIDE 8 : Interface JavaFX - Dashboard (~1 minute)

> *[Pointer les fonctionnalités, ton enthousiaste]*

**Merci [Membre B].**

Notre interface utilisateur est développée en **JavaFX** selon le pattern **MVC**.

- Le **Model** contient nos classes partagées : `Metric`, `Alert`, `User`
- La **View** est définie en **FXML**, un format déclaratif
- Et le **Controller** — `DashboardController` — gère toute la logique

Le **rafraîchissement automatique** est géré par un `Timeline` JavaFX qui exécute la méthode `refreshData()` toutes les 2 secondes. Cela permet d'avoir un tableau de bord véritablement **temps réel**.

Côté fonctionnalités, nous proposons :
- Des **graphiques temps réel** pour CPU, RAM et Disque sous forme de `LineChart`
- Un **tableau des alertes** avec filtres par sévérité et par date
- Des **statistiques avancées** : moyenne, min, max, écart-type
- Un système d'**authentification** avec trois rôles : Admin, Opérateur, Lecteur
- La **configuration des seuils** d'alerte
- Et l'**export des données** en CSV ou JSON

Toutes les mises à jour UI passent par `Platform.runLater()` pour garantir qu'elles s'exécutent sur le thread JavaFX.

---

### SLIDE 9 : Démonstration Live (~30 secondes d'intro + DÉMO)

> *[Se tourner vers l'écran de démo]*

**Place maintenant à la démonstration !**

Je vais vous montrer le système en action. Nous allons :

1. **Démarrer le serveur** qui initialise le registre RMI et les listeners
2. **Lancer un ou plusieurs agents** qui commencent à collecter et envoyer les métriques
3. **Ouvrir le client UI** pour visualiser le dashboard en temps réel

> *[Exécuter les commandes, montrer le dashboard]*
> *[Provoquer une alerte si possible : charge CPU élevée]*

Comme vous pouvez le voir, les graphiques se mettent à jour automatiquement. Si une métrique dépasse le seuil configuré, une alerte apparaît immédiatement dans le tableau.

---

### SLIDE 10 : Bilan Technique (~45 secondes)

> *[Ton satisfait, énumérer les réalisations]*

Faisons le **bilan de nos réalisations**.

Tous nos objectifs ont été **atteints** :
- Le monitoring **temps réel multi-agents** fonctionne parfaitement
- Notre architecture **distribuée** combine efficacement UDP, TCP et RMI
- Le **Pattern Strategy** rend le système facilement extensible
- Les alertes sont entièrement **configurables**
- Le dashboard **JavaFX** est moderne et réactif
- L'**authentification** et les **statistiques avancées** sont opérationnels
- Et l'**export de données** permet l'intégration avec d'autres outils

Ce projet nous a permis d'acquérir des compétences solides en **programmation réseau**, **RMI**, **concurrence Java**, **patterns de conception**, et **JavaFX**.

---

### SLIDE 11 : Perspectives (~30 secondes)

> *[Regard vers le futur, ton ambitieux]*

Pour **aller plus loin**, plusieurs évolutions sont envisageables :

- Passer à une **base de données SQL** pour une meilleure scalabilité
- Exposer une **API REST** avec Spring Boot pour d'autres clients
- **Conteneuriser** avec Docker pour faciliter le déploiement
- Intégrer du **Machine Learning** pour la prédiction de pannes

Côté fonctionnalités : une **application mobile**, des **notifications email/SMS**, des métriques **réseau**, et des **plugins** pour Grafana ou Prometheus.

---

### SLIDE 12 : Questions (~15 secondes)

> *[Sourire, ouvrir au dialogue]*

**Nous vous remercions pour votre attention.**

Nous sommes maintenant à votre disposition pour répondre à toutes vos **questions**.

Le code source complet et la documentation détaillée sont disponibles sur demande.

**Merci !**

---

## RÉCAPITULATIF DES TEMPS

| Intervenant | Slides | Durée Estimée |
|------------|--------|---------------|
| **A** (Archi) | 1-4 | ~3 min 30s |
| **B** (Backend) | 5-7 | ~3 min 30s |
| **C** (Frontend/Démo) | 8-12 | ~3 min |
| **TOTAL** | 12 | **~10 min** |

---

## CONSEILS POUR LA PRÉSENTATION

### Avant la soutenance
- [ ] Tester la compilation LaTeX du fichier `presentation.tex`
- [ ] Préparer l'environnement de démo (3 terminaux prêts)
- [ ] Vérifier que le serveur, l'agent et l'UI fonctionnent

### Pendant la soutenance
- [ ] **Contact visuel** avec le jury
- [ ] **Pointer** les éléments pertinents sur les slides
- [ ] **Transitions fluides** entre intervenants
- [ ] **Parler lentement** et clairement
- [ ] Avoir un **plan B** si la démo échoue (screenshots)

### Commandes pour la démo

```bash
# Terminal 1 - Serveur
java -jar monitoring-server/target/monitoring-server-1.0-SNAPSHOT.jar

# Terminal 2 - Agent
java -jar monitoring-agent/target/monitoring-agent-1.0-SNAPSHOT.jar

# Terminal 3 - Client UI
java -jar monitoring-ui/target/monitoring-ui-1.0-SNAPSHOT.jar
```

---

*Script préparé pour la soutenance du projet Système de Surveillance Distribué*
*Année Universitaire 2025-2026*
