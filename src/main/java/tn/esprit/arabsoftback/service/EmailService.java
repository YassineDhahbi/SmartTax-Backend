package tn.esprit.arabsoftback.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

@Service
public class EmailService {

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${spring.mail.port}")
    private int smtpPort;

    @Value("${spring.mail.username}")
    private String emailUsername;

    @Value("${spring.mail.password}")
    private String emailPassword;

    @Value("${spring.mail.username}")
    private String emailFrom;

    /**
     * Envoyer un email de validation avec code de sécurité
     */
    public boolean sendValidationEmail(String to, String subject, String body, String securityCode, String registrationLink) {
        try {
            System.out.println("📧 Configuration du service email:");
            System.out.println("   SMTP Host: " + smtpHost);
            System.out.println("   SMTP Port: " + smtpPort);
            System.out.println("   Username: " + emailUsername);
            System.out.println("   From: " + emailFrom);
            System.out.println("   To: " + to);
            System.out.println("   Security Code: " + securityCode);

            // Configuration des propriétés SMTP pour Gmail
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            // Supprimer les configurations SSL qui causent des problèmes
            // props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            // props.put("mail.smtp.socketFactory.fallback", "false");
            // props.put("mail.smtp.socketFactory.port", smtpPort);

            System.out.println("🔧 Propriétés SMTP configurées");

            // Créer la session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });

            session.setDebug(true); // Activer le debug pour voir les détails SMTP
            System.out.println("🔐 Session SMTP créée avec authentification");

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Configurer le contenu HTML
            message.setContent(body, "text/html; charset=utf-8");
            System.out.println("📝 Email message créé");

            // Envoyer l'email
            System.out.println("🚀 Envoi de l'email en cours...");
            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à: " + to);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoyer un email avec le TIN généré
     */
    public boolean sendTINEmail(String to, String subject, String body, String tin, String registrationLink) {
        try {
            System.out.println("📧 Configuration du service email:");
            System.out.println("   SMTP Host: " + smtpHost);
            System.out.println("   SMTP Port: " + smtpPort);
            System.out.println("   Username: " + emailUsername);
            System.out.println("   From: " + emailFrom);
            System.out.println("   To: " + to);
            System.out.println("   TIN: " + tin);

            // Configuration des propriétés SMTP pour Gmail
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            System.out.println("🔧 Propriétés SMTP configurées");

            // Créer la session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });

            session.setDebug(true); // Activer le debug pour voir les détails SMTP
            System.out.println("🔐 Session SMTP créée avec authentification");

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Configurer le contenu HTML
            message.setContent(body, "text/html; charset=utf-8");
            System.out.println("📝 Email message créé");
            System.out.println("📧 TIN à envoyer: " + tin);

            // Envoyer l'email
            System.out.println("🚀 Envoi de l'email en cours...");
            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à: " + to);
            System.out.println("📋 TIN inclus: " + tin);

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tester la configuration email
     */
    public String testEmailConfiguration() {
        try {
            StringBuilder result = new StringBuilder();
            result.append("📧 Test de configuration email:\n");
            result.append("   SMTP Host: ").append(smtpHost).append("\n");
            result.append("   SMTP Port: ").append(smtpPort).append("\n");
            result.append("   Username: ").append(emailUsername).append("\n");
            result.append("   From: ").append(emailFrom).append("\n");

            // Test de connexion SMTP
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });

            Transport transport = session.getTransport("smtp");
            transport.connect(smtpHost, emailUsername, emailPassword);
            transport.close();

            result.append("   ✅ Connexion SMTP réussie !");
            return result.toString();

        } catch (Exception e) {
            return "❌ Erreur de configuration email: " + e.getMessage();
        }
    }

    /**
     * Envoyer un email générique
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            System.out.println("Email générique - To: " + to + ", Subject: " + subject);
            
            // Configuration des propriétés SMTP
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", smtpHost);
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            // Créer la session avec authentification
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailUsername, emailPassword);
                }
            });

            // Créer le message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailFrom));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            // Configurer le contenu HTML
            message.setContent(body, "text/html; charset=utf-8");

            // Envoyer l'email
            Transport.send(message);
            System.out.println("Email générique envoyé avec succès à: " + to);

            return true;

        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email générique: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Générer un code de sécurité aléatoire
     */
    public String generateSecurityCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i > 0 && i % 2 == 0) {
                code.append("-");
            }
            code.append(characters.charAt((int) (Math.random() * characters.length())));
        }
        return code.toString();
    }
}
