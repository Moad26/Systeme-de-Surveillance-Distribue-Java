# Système de Surveillance Distribué

![Status](https://img.shields.io/badge/Status-En%20d%C3%A9veloppement-yellow)
![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-C71A36)
![JavaFX](https://img.shields.io/badge/JavaFX-MVC-blue)
![RMI](https://img.shields.io/badge/Protocol-RMI-lightgrey)

Un système complet de surveillance de parc informatique permettant la collecte de métriques (CPU, Disque, Mémoire) via des agents distribués et leur visualisation centralisée.

## 🏗 Architecture Modulaire

Le projet est divisé en 4 modules Maven distincts :

- **`monitoring-common`** : Contient les modèles de données partagés (Objets `Metric`, `Alert`, etc.) et les interfaces communes utilisées par les autres modules.
- **`monitoring-agent`** : L'agent installé sur les machines à surveiller. Il est responsable de la collecte des données système (CPU, Disk, Memory) et de leur envoi vers le serveur via UDP et TCP.
- **`monitoring-server`** : Le cœur du système. Il centralise la réception des données, assure la persistance (format JSON) et expose des services RMI pour les clients.
- **`monitoring-ui`** : L'interface utilisateur cliente développée en JavaFX (MVC). Elle permet aux administrateurs de visualiser les tableaux de bord en temps réel.

## 📋 Prérequis

- **Java JDK 21+**
- **Maven 3.8+**

## 🛠 Installation et Compilation

Pour compiler l'ensemble du projet et générer les exécutables (JARs), exécutez la commande suivante à la racine du projet :

```bash
mvn clean install
```

## 🚀 Guide de Démarrage (Run)

Une fois la compilation terminée, vous devez lancer les composants dans l'ordre suivant. Assurez-vous d'ouvrir un terminal séparé pour chaque commande.

### Étape 1 : Lancer le Serveur

Le serveur doit être démarré en premier pour écouter les agents et les clients.

```bash
java -jar monitoring-server/target/monitoring-server-1.0-SNAPSHOT.jar
```

### Étape 2 : Lancer un Agent

Lancez un ou plusieurs agents pour commencer à collecter des données.

```bash
java -jar monitoring-agent/target/monitoring-agent-1.0-SNAPSHOT.jar
```

### Étape 3 : Lancer le Client UI

Enfin, lancez l'interface graphique pour visualiser les données.

```bash
java -jar monitoring-ui/target/monitoring-ui-1.0-SNAPSHOT.jar
```

## 👥 Auteurs

- Mouaad El Yalaoui
- Yassine Chaoui
- Id Ali Abdelali
