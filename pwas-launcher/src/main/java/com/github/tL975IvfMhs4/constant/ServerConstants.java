package com.github.tL975IvfMhs4.constant;

public final class ServerConstants {
    /**
     * Dossier réservé contenant les données nécessaires au bon fonctionnement du serveur.
     * On y placera entre autres les certificats et autres fichiers utilitaires dont on pourrait avoir besoin.<p>
     * L’utilisateur disposera d’un bouton permettant de tout nettoyer, donc il ne faut pas porter trop
     * d’attachement au contenu de ce dossier, penser à vérifier son contenu avant de travailler avec et le régénérer s’il
     * manque.
     */
    public static final String SERVER_FOLDER = ".pwas-server";



    /**
     * Dossier des certificats tls du serveur. Il est recommandé de nettoyer ces certificats le moins souvent possible
     * puisqu’ils seront importés sur les mobiles pour faire fonctionner les PWA, donc un changement de certificat
     * invaliderait intégralement la PWA et forcerait sa réinstallation.
     */
    public static final String CERTIFICATE_FOLDER = "certificats";

    /**
     * Nom des fichiers contenant la clé publique et la clé privée.
     */
    public static final String FICHIER_CLE_CHIFFREMENT = "cle";

    /**
     * Nom des fichiers contenant les certificats aux différents formats.
     */
    public static final String FICHIER_CERTIFICAT = "certificat";



    /**
     * Dossier dist de l’application. Il contiendra ce qui est à servir, à savoir index.html, apps.json et la liste des PWA compilées.<p>
     * On essaie d’abord de voir s’il est présent en interne dans le jar, sinon on part du principe qu’on est en mode développement
     * et donc on le recherche sur le disque à l’endroit prévu (racine du projet).
     */
    public static final String DIST_FOLDER = "dist";
}
