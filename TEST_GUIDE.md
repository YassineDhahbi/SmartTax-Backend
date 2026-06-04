# Guide de Test - API Immatriculation

## 🚀 Démarrage

1. **Démarrez votre backend Spring Boot**
2. **Vérifiez que la base de données est configurée**
3. **Testez les endpoints suivants**

## 🧪 Tests de base

### 1. Test de santé
```bash
curl -X GET http://localhost:8080/api/test/health
```
**Réponse attendue :**
```json
{
  "status": "OK",
  "message": "Backend SmartTax is running",
  "timestamp": "2024-03-16T15:20:00",
  "version": "1.0.0"
}
```

### 2. Test de disponibilité de l'API immatriculation
```bash
curl -X GET http://localhost:8080/api/test/immatriculation-ready
```

### 3. Test de création de dossier (sans fichiers)
```bash
curl -X POST http://localhost:8080/api/immatriculation/create \
  -H "Content-Type: application/json" \
  -d '{
    "typeContribuable": "PHYSIQUE",
    "nom": "Test",
    "prenom": "User",
    "cin": "99999999",
    "dateNaissance": "1990-01-01",
    "email": "test@example.com",
    "telephone": "21612345678",
    "adresse": "Test Address",
    "typeActivite": "Commerce",
    "secteur": "Commerce de détail",
    "adresseProfessionnelle": "Test Work Address",
    "dateDebutActivite": "2024-01-01",
    "descriptionActivite": "Test activity description",
    "submissionMode": "SUBMIT",
    "confirmed": true
  }'
```

### 4. Test de récupération de dossier
```bash
curl -X GET http://localhost:8080/api/immatriculation/1
```

### 5. Test de recherche
```bash
curl -X GET "http://localhost:8080/api/immatriculation/search?nom=Test&email=test@example.com"
```

### 6. Test des statistiques
```bash
curl -X GET http://localhost:8080/api/immatriculation/statistics
```

### 7. Test du tableau de bord
```bash
curl -X GET http://localhost:8080/api/immatriculation/dashboard
```

## 🔧 Dépannage

### Problème : 401 Unauthorized
**Cause** : Le filtre JWT bloque encore la requête
**Solution** : Vérifiez que les endpoints sont bien dans `shouldNotFilter()` et `permitAll()`

### Problème : 404 Not Found
**Cause** : L'endpoint n'existe pas
**Solution** : Vérifiez le mapping dans le controller

### Problème : 500 Internal Server Error
**Cause** : Erreur dans le code ou base de données
**Solution** : Consultez les logs du backend

### Problème : CORS
**Cause** : Le frontend ne peut pas accéder à l'API
**Solution** : Vérifiez la configuration CORS dans `WebConfig.java`

## 📊 Vérification de la base de données

Après avoir créé un dossier, vérifiez dans votre base de données :

```sql
USE votre_base_de_donnees;
SELECT * FROM immatriculation ORDER BY date_creation DESC LIMIT 5;
```

## 🌐 Test avec le frontend

1. **Démarrez votre frontend Angular** : `ng serve`
2. **Allez sur** : `http://localhost:4200`
3. **Naviguez vers** : Espace Contribuable → Immatriculation en ligne
4. **Remplissez le formulaire** et soumettez

## ✅ Checklist de validation

- [ ] Backend démarré sans erreur
- [ ] Test de santé OK
- [ ] Création de dossier fonctionne
- [ ] Récupération de dossier fonctionne
- [ ] Recherche fonctionne
- [ ] Statistiques fonctionnent
- [ ] Frontend peut appeler l'API
- [ ] Les fichiers sont bien sauvegardés
- [ ] Les scores sont calculés correctement
- [ ] Le workflow fonctionne (brouillon → soumis → validé)

## 📝 Logs utiles

Dans les logs du backend, vous devriez voir :
```
Checking if URI should be filtered: /api/immatriculation/create
Nouveau dossier d'immatriculation créé: TN-DG-2024-XXXXXX
```

## 🚨 Erreurs communes

1. **"Table immatriculation doesn't exist"**
   - Exécutez le script SQL de migration

2. **"Connection refused"**
   - Vérifiez que le backend est bien démarré sur le bon port

3. **"CORS policy error"**
   - Vérifiez la configuration CORS dans `WebConfig.java`

4. **"Field 'xxx' doesn't have a default value"**
   - Vérifiez que tous les champs requis sont bien envoyés

---

**Si tout fonctionne, votre API est prête à être utilisée avec le frontend !** 🎉
