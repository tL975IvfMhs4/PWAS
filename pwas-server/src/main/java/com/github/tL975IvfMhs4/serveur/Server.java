package com.github.tL975IvfMhs4.serveur;

import com.github.tL975IvfMhs4.security.SecurityService;
import com.github.tL975IvfMhs4.dns.LocalDNSManager;
import com.github.tL975IvfMhs4.pwas.PWASManager;

public class Server {
    // Déterminer l’os pour construire les dossiers qu’on vise (linux/mac d’un côté, windows de l’autre)
    // Aller voir le contenu du dossier de certificats, les générer le cas échéant, en utilisant un nom stable
    // Générer un .mobileconfig pour distribuer le certificat pour ios, trouver un truc pour android
    // Chercher le dist en interne, s’il est absent le chercher dans le projet
    // Une fois qu’on a fini de déterminer tout ça, utiliser mDNS (JmDNS ?) pour générer et exposer le nom stable
    // Créer les handler

    public static void run() {
        final PWASManager pwasManager = new PWASManager();

        // Chargement des certificats avec génération si nécessaire
        loadCertificates();

        // Recherche du dist, mode release ou dev
        loadPWAS(pwasManager);

        // Génération et exposition du nom stable pour le dns local
        prepareLocalDNS();

        // Attribution des handlers pour les routes
        assignRequestHandlers(pwasManager);
    }

    public static void stop() {
        System.exit(0);
    }

    public static void crash() {
        System.exit(1);
    }

    public static void crash(Runnable beforeCrash) {
        beforeCrash.run();
        crash();
    }

    private static void loadCertificates() {
        new SecurityService().loadCertificates();
    }

    private static void loadPWAS(PWASManager pwasManager) {
        pwasManager.loadPWAS();
    }

    private static void prepareLocalDNS() {
        new LocalDNSManager().prepareLocalDNS();
    }

    private static void assignRequestHandlers(PWASManager pwasManager) {
        pwasManager.assignRequestHandlers();
    }
}
