package com.github.tL975IvfMhs4.security;

import com.github.tL975IvfMhs4.serveur.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.RSAKeyGenParameterSpec;

import static com.github.tL975IvfMhs4.constant.SecurityConstants.TAILLE_CLE_RSA;

public class SecurityGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGenerator.class);

    public static KeyPair generateRSAKeyPair() {
        try {
            final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", "BC");
            gen.initialize(new RSAKeyGenParameterSpec(TAILLE_CLE_RSA, RSAKeyGenParameterSpec.F4));
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            Server.crash(() -> LOGGER.error("Impossible de construire le générateur de clés de chiffrement RSA avec le provider BC (bouncy castle), le serveur ne peut pas continuer de fonctionner", e));
        } catch (InvalidAlgorithmParameterException e) {
            Server.crash(() -> LOGGER.error("Impossible d’initialiser le générateur RSA avec l’exposant F4 (65537) pour la clé de taille {}", TAILLE_CLE_RSA, e));
        }
        // On ne passe jamais dans ce return null en pratique puisqu’on arrête le serveur en cas d’erreur
        return null;
    }
}
