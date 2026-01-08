# Architecture et Fonctionnement du Code - Système de Surveillance Distribué

Ce document détaille le fonctionnement interne du système, module par module. Il est destiné aux développeurs souhaitant comprendre l'implémentation, les flux de données et les choix techniques.

---

## 1. Module `monitoring-common`
Ce module contient les briques de base partagées entre le serveur, l'agent et l'UI. Il n'a aucune dépendance vers les autres modules du projet.

### Fichiers Clés
- **`Metric.java`** : Structure de données immuable représentant une capture instantanée de l'état du système.
- **`Alert.java`** : Structure représentant un événement critique.
- **`IMonitoringService.java`** : Le contrat (interface) RMI qui permet à l'UI de dialoguer avec le serveur.

### Points Techniques Importants

#### Interface RMI (`IMonitoringService`)
L'utilisation de RMI (Remote Method Invocation) permet d'abstraire la communication réseau complexe entre l'UI et le Serveur. Le serveur expose ses méthodes comme si elles étaient locales.

```java
// Extrais de IMonitoringService.java
public interface IMonitoringService extends Remote {
    // Authentification
    String authenticate(String username, String password) throws RemoteException;
    
    // Récupération des métriques (utilisé par le dashboard pour les graphes)
    List<Metric> getMetrics(String agentId, int limit) throws RemoteException;
    
    // Récupération des statistiques calculées (Moyenne, Tendance)
    MetricStatistics getStatistics(String agentId, long fromTime, long toTime) throws RemoteException;
}
```

---

## 2. Module `monitoring-agent`
L'agent est un service léger déployé sur les machines surveillées. Son rôle est de collecter périodiquement des données et de transmettre des alertes.

### Fichiers Clés
- **`AgentApp.java`** : Orchestrateur principal.
- **`UdpSender.java`** : Envoi de métriques "Fire-and-forget".
- **`TcpClient.java`** : Envoi garanti pour les alertes.
- **`collectors/*`** : Classes spécialisées utilisant la librairie OSHI.

### Logique de Collecte (AgentApp)
L'agent utilise un `ScheduledExecutorService` pour garantir une régularité parfaite des mesures, sans bloquer le thread principal.

```java
// Dans AgentApp.java : La boucle de collecte
scheduler.scheduleAtFixedRate(
    this::collectAndSend,  // Méthode à exécuter
    0,                     // Délai initial
    COLLECTION_INTERVAL_SECONDS, // Période (ex: 5s)
    TimeUnit.SECONDS
);

private void collectAndSend() {
    // 1. Collecte via les sondes OSHI
    double cpu = cpuCollector.collect();
    double ram = memoryCollector.collect();
    
    // 2. Envoi léger en UDP (perte tolérable)
    Metric metric = new Metric(agentId, cpu, ram, disk);
    udpSender.sendMetric(metric);
    
    // 3. Vérification critique et envoi TCP (fiable)
    checkAndSendAlerts(cpu, ram, disk);
}
```

### Communication Réseau Hybride
L'architecture utilise deux protocoles pour optimiser la charge :
1.  **UDP pour les Métriques** : Rapide, pas de "handshake", idéal pour des données envoyées toutes les secondes. Si un paquet est perdu, ce n'est pas grave (la prochaine seconde en enverra un autre).
2.  **TCP pour les Alertes** : Plus lourd mais fiable. Une alerte critique ne doit jamais être perdue.

```java
// UdpSender.java : Envoi performant
public void sendMetric(Metric metric) {
    byte[] data = serialize(metric);
    // Packet non connecté, direct vers IP:PORT
    DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
    socket.send(packet);
}

// TcpClient.java : Connexion dédiée par alerte
public void sendAlert(Alert alert) {
    // Ouverture d'une connexion TCP synchrone
    try (Socket socket = new Socket(serverHost, serverPort);
         ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
        oos.writeObject(alert);
        oos.flush(); // Garantie d'envoi
    }
}
```

---

## 3. Module `monitoring-server`
Le serveur centralise tout. Il est multithreadé pour gérer simultanément la réception UDP, TCP et les requêtes RMI.

### Fichiers Clés
- **`ServerApp.java`** : Initialise tous les composants.
- **`UdpListener.java`** : Thread d'écoute haute performance.
- **`TcpAlertHandler.java`** : ThreadPool pour les alertes.
- **`MetricsPersistence.java`** : Sauvegarde JSON.
- **`MonitoringServiceImpl.java`** : Implémentation de la logique métier exposée.

### Réception Asynchrone (UdpListener)
Le serveur écoute en permanence sur le port UDP dans un thread dédié pour ne jamais bloquer le reste de l'application.

```java
// UdpListener.java
public void run() {
    byte[] buffer = new byte[8192];
    while (running) {
        // Bloquant jusqu'à réception d'un paquet
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        
        // Désérialisation immédiate et stockage en mémoire
        Metric metric = deserialize(packet.getData(), packet.getLength());
        dataManager.addMetric(metric); 
    }
}
```

### Persistence des Données
Les données sont périodiquement flushées sur disque au format JSON pour survivre à un redémarrage serveur.

```java
// MetricsPersistence.java : Sauvegarde par lot
private void saveAgentMetrics(String agentId) {
    // Écriture manuelle du JSON pour éviter la surcharge de librairies lourdes
    writer.write("[\n");
    for (Metric m : metrics) {
        writer.write(String.format(
            "  {\"agentId\":\"%s\",\"timestamp\":%d,\"cpu\":%.2f...}",
            m.getAgentId(), m.getTimestamp(), m.getCpuUsage()...
        ));
    }
    writer.write("]");
}
```

---

## 4. Module `monitoring-ui`
L'interface est une application JavaFX riche. Elle ne stocke aucune données métier, elle n'est qu'une "vue" sur le serveur via RMI.

### Fichiers Clés
- **`DashboardController.java`** : Contrôleur unique gérant toute la logique d'affichage.

### Mise à jour en Temps Réel
L'UI ne maintient pas de connexion ouverte permanente (comme un WebSocket). Elle utilise le **Polling** via RMI. Un `Timeline` JavaFX appelle le serveur toutes les 2 secondes.

```java
// DashboardController.java
private void startPolling() {
    // Timeline JavaFX pour rester dans le thread UI
    pollingTimeline = new Timeline(new KeyFrame(
        Duration.millis(2000), // Intervalle de rafraîchissement
        event -> refreshData() // Action
    ));
    pollingTimeline.play();
}

private void refreshData() {
    // Comparaison : On demande juste les N dernières mesures au serveur
    List<Metric> metrics = monitoringService.getMetrics(selectedAgentId, 30);
    
    // Mise à jour atomique de l'interface graphique (Thread Safety)
    Platform.runLater(() -> {
        cpuSeries.getData().clear();
        for (Metric m : metrics) {
            cpuSeries.getData().add(new XYChart.Data<>(formatTime(m), m.getCpuUsage()));
        }
    });
}
```

---

## 5. Structure des Données (Stockage)
Les fichiers dans `monitoring-server/data/` suivent une structure JSON stricte.

### Fichier Métriques (`metrics/agent-id.json`)
Stocke l'historique brut des points de mesure.
```json
[
  {
    "agentId": "moad-wsl-13555",
    "timestamp": 1704739200000,
    "cpu": 12.5,  // Pourcentage d'usage
    "ram": 45.2,
    "disk": 60.1
  },
  ...
]
```

### Fichier Alertes (`alert_configs.json`)
Stocke les seuils de déclenchement configurés par l'administrateur.
```json
[
  {
    "metricType": "CPU",
    "warningThreshold": 70.0,
    "criticalThreshold": 90.0
  },
  ...
]
```
