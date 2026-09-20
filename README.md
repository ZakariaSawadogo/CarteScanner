# CardScan AI

CardScan AI est une application Android professionnelle conçue pour numériser des cartes de visite, extraire les informations hors-ligne via OCR, et enrichir automatiquement les profils de contacts grâce à l'Intelligence Artificielle et des techniques de recherche OSINT (Open Source Intelligence).

##  Fonctionnalités Principales

*   **Extraction Hors-ligne (OCR) :** Détection instantanée du texte et des QR Codes via Google ML Kit. Analyse syntaxique locale par expressions régulières (Regex) pour isoler les emails et numéros de téléphone sans connexion internet.
*   **Enrichissement OSINT (Furtif) :** Moteur de recherche personnalisé contournant les protections anti-bot (via requêtes POST simulées) pour récupérer les traces numériques du contact sur le web.
*   **Correction & Synthèse par IA :** Intégration de l'API Groq (LLM) pour croiser le texte brut de l'OCR et les résultats OSINT. L'IA corrige les fautes de frappe et identifie les URLs exactes des réseaux sociaux (LinkedIn, Twitter/X, Instagram, Facebook).
*   **Architecture Offline-First :** Sauvegarde instantanée dans une base de données locale (Room) garantissant l'accès aux cartes scannées même sans réseau. Les données sont mises à jour de manière réactive (Kotlin Flows) une fois l'enrichissement terminé en arrière-plan.
*   **Export Natif :** Ajout du profil complet, incluant les notes et tous les liens des réseaux sociaux, directement dans le répertoire natif du téléphone en un clic.
*   **Interface Moderne & Multilingue :** Design basé sur Material Design 2, navigation par Drawer (F-Shape pattern), support des thèmes Clair/Sombre, et internationalisation complète (Français, Anglais, Turc).

##  Stack Technique

*   **Langage :** Kotlin
*   **Architecture :** Single-Activity, Fragments, Navigation Component, MVVM/Repository Pattern
*   **Base de données :** Room Database (SQLite) avec Coroutines & Flows
*   **Réseau & Web Scraping :** OkHttp3, Jsoup, HTML Parsing
*   **Intelligence Artificielle :** Groq Cloud API (Llama 3), Google ML Kit (Vision API)
*   **UI/UX :** Material Components, XML Layouts, AppCompatDelegate (Changement de langue à la volée)

##  Installation & Configuration

1. Clonez ce dépôt :
   ```bash
   git clone [https://github.com/ZakariaSawadogo/CarteScanner.git](https://github.com/votre-nom/CardScan-AI.git)
2. Ouvrez le projet dans **Android Studio**.
3. Compilez et installez l'application sur un émulateur ou un appareil physique.
4. Au premier lancement, rendez-vous dans l'onglet **Paramètres** de l'application.
5. Saisissez votre clé API Groq (obtenue gratuitement sur `console.groq.com`). Un voyant vert confirmera la validité de la clé grâce à un test réseau en arrière-plan.
6. Scannez votre première carte !

## 🔒 Confidentialité des Données

L'approche *Offline-First* garantit que les informations extraites de la carte sont stockées localement sur l'appareil. La clé API de l'utilisateur est conservée dans les préférences chiffrées (`EncryptedSharedPreferences`) de l'application.

## 🤝 Contribution

Les contributions, signalements de bugs et suggestions sont les bienvenus ! N'hésitez pas à ouvrir une *Issue* ou à soumettre une *Pull Request*.
```eof

N'oublie pas le conseil sur la révocation de ta clé API si tu la passes en public sur GitHub. Si tout est bon pour toi, ton projet est prêt à être partagé avec le monde ! As-tu d'autres détails à régler ?
