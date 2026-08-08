package com.github.tL975IvfMhs4.security;

import com.github.tL975IvfMhs4.constant.DNSConstants;
import com.github.tL975IvfMhs4.serveur.Server;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Date;

import static com.github.tL975IvfMhs4.constant.DNSConstants.LOCAL_DNS_NAME;
import static com.github.tL975IvfMhs4.constant.SecurityConstants.*;

public final class SecurityGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGenerator.class);

    public static KeyPair generateRSAKeyPair() {
        try {
            final KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA", SECURITY_PROVIDER);
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

    // TODO pas fini du tout
    public static KeyPair generateECDSAKeyPair() {
        try {
            final KeyPairGenerator gen = KeyPairGenerator.getInstance("ECDSA", SECURITY_PROVIDER);
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

    // TODO
    //     La clock devra être sur utc pour être tranquille (éviter les pièges foireux)
    //     C’est marqué ici puisque c’est le premier endroit où la clock est mentionnée dans le code

    // TODO retourner un objet contenant un certificat et une erreur/liste d’erreurs à afficher, ou alors lever une exception maison ? et faire la même chose pour l’autre endroit où on est obligés de se coltiner un return null qui ne devrait pas exister
    public static X509Certificate generateCACertificate(KeyPair cles, Clock clock) {

        // --------------------------------------
        // DONNÉES DE BASE DU CERTIFICAT
        // --------------------------------------

        // Sujet du certificat, on met un peu ce qu’on veut sauf pour le CN où on met le nom de domaine
        // TODO utiliser le builder pour rendre ça plus propre
        final X500Name sujet = new X500Name("C=FR, L=Maison, O=PWAS, CN=" + LOCAL_DNS_NAME + " CA");

        // On utilise le timestamp comme numéro de série pour éviter tout risque de conflit si on doit régénérer les certificats plus tard
        final BigInteger serialNumber = BigInteger.valueOf(clock.millis());

        // Le certificat commence 10 secondes avant le démarrage du serveur, et périme dans 100 ans
        final Date notBefore = Date.from(clock.instant().minusSeconds(10));
        final Date notAfter = Date.from(OffsetDateTime.now(clock).plusYears(100).toInstant());

        // Le sujet est en premier paramètre à la place de l’issuer puisqu’on construit une CA, donc on auto-signe le certificat
        // TODO : la clé publique du serveur sera une RSA, à ne pas confondre avec la clé privée du ContentSigner qui sera la clé privée ECDSA de la CA
        //        à voir : j’aurai en fait probablement jamais besoin de RSA, ECDSA fera le boulot tout du long, et donc même pas besoin de keyEncipherment dans le keyUsage du certificat serveur
        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
            sujet,
            serialNumber,
            notBefore,
            notAfter,
            sujet,
            cles.getPublic()
        );



        // --------------------------------------
        // EXTENSIONS
        // --------------------------------------

        // On rajoute des SAN pour les normes de certificat récentes
        // Pour l’instant on a que le nom stable local
        // Attention à ne pas mettre localhost ou 127.0.0.1 (ou toute IP locale), puisqu’à terme
        // la CA sera importée sur un téléphone donc localhost y sera une faiblesse
        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName[]{
            new GeneralName(GeneralName.dNSName, DNSConstants.LOCAL_DNS_NAME)
        });
        try {
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        } catch (CertIOException e) {
            Server.crash(() -> LOGGER.error("Impossible de rajouter un SAN de certificat de la CA", e));
        }

        // On est une CA donc on a le droit de signer des certificats et des CRL (listes noires), rien de plus
        // TODO le certificat serveur aura digitalSignature + keyEncipherment
        try {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.cRLSign | KeyUsage.keyCertSign));
        } catch (CertIOException e) {
            Server.crash(() -> LOGGER.error("Impossible de renseigner les usages de clé dans le certificat de la CA", e));
        }

        // Pas de extendedKeyUsage pour la CA
        // TODO il faudra donner serverAuth au certificat serveur, non critique

        // Basic constraints : on génère une CA donc CA:true
        // TODO faux pour le certificat serveur
        try {
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        } catch (CertIOException e) {
            Server.crash(() -> LOGGER.error("Impossible de marquer le certificat comme CA dans les contraintes de base", e));
        }

        // Signature du certificat, on utilise ECDSA P-256
        // TODO pareil pour le certificat serveur
        try {
            final ContentSigner contentSigner = new JcaContentSignerBuilder(CA_KEY_ALGORITHM).setProvider(SECURITY_PROVIDER).build(cles.getPrivate());

            final X509CertificateHolder holder = certificateBuilder.build(contentSigner);
            final X509Certificate certificat = new JcaX509CertificateConverter().setProvider(SECURITY_PROVIDER).getCertificate(holder);

            certificat.checkValidity();
            // TODO : le serveur devra utiliser sa propre clé publique
            certificat.verify(cles.getPublic());

            return certificat;
        } catch (OperatorCreationException e) {
            Server.crash(() -> LOGGER.error("Impossible de générer la signature du certificat de la CA", e));
        } catch (CertificateExpiredException e) {
            Server.crash(() -> LOGGER.error("Échec de validation du certificat CA généré : déjà expiré", e));
        } catch (CertificateNotYetValidException e) {
            Server.crash(() -> LOGGER.error("Échec de validation du certificat CA généré :  pas encore rentré dans sa période de validité qui aurait dû déjà commencer", e));
        } catch (CertificateException e) {
            Server.crash(() -> LOGGER.error("Impossible de générer le X509Certificate de la CA depuis le holder", e));
        } catch (NoSuchAlgorithmException e) {
            Server.crash(() -> LOGGER.error("Échec de validation du certificat CA généré : algorithme de la signature invalide", e));
        } catch (SignatureException e) {
            Server.crash(() -> LOGGER.error("Échec de validation du certificat CA généré : erreur de signature", e));
        } catch (InvalidKeyException e) {
            Server.crash(() -> LOGGER.error("Échec de validation du certificat CA généré : clé invalide", e));
        } catch (NoSuchProviderException e) {
            Server.crash(() -> LOGGER.error("Échec de validation du certificat CA généré : le provider bouncycastle n’a pas été trouvé", e));
        }

        // Ce return ne sera jamais atteint parce que tous les catch mènent à un arrêt de la jvm
        return null;
    }
}
