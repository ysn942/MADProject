# MAD Project — Mobile App Development (UPM)

Application Android en **Jetpack Compose** développée dans le cadre du cours Mobile App Development.

## Architecture

```
MainActivity (Compose)
├── Page 1 — Home       : GPS + météo (Retrofit) + icône (Coil)
├── Page 2 — Collection : Liste Room DB, save/delete
└── Page 3 — Settings   : SharedPreferences (User ID + API Key)

OpenStreetMapsActivity  : Carte OSM, marqueurs, tracé, points Room DB
FirebaseActivity (W7)   : Auth Firebase + Realtime Database
```

## Fonctionnalités par semaine

| Semaine | Fonctionnalité | Fichiers |
|---------|---------------|---------|
| W2–W3 | GPS + OpenStreetMap + marqueurs | `MainActivity.kt`, `OpenStreetMapsActivity.kt` |
| W4 | SharedPreferences, Dialogs, Toast | `MainActivity.kt` (Page 3) |
| W5 | ListView (Compose LazyColumn), Navigation | `MainActivity.kt` (Page 2) |
| W6 | **Room DB**, **Retrofit (OpenWeatherMap)**, **Coil (images)** | `room/`, `network/`, `MainActivity.kt` |
| W7 | **Firebase Auth + Realtime DB** | `FirebaseActivity.kt` |

## Configuration requise

### OpenWeatherMap (météo)
1. Créer un compte sur [openweathermap.org](https://openweathermap.org)
2. Copier votre API Key gratuite
3. Dans l'app → **Settings (Page 3)** → coller la clé

### Firebase (semaine 7)
1. Créer un projet sur [console.firebase.google.com](https://console.firebase.google.com)
2. Activer **Authentication** → Email/Password
3. Activer **Realtime Database** (mode test)
4. Télécharger `google-services.json` → placer dans `app/`
5. Dans `build.gradle.kts` (app) : décommenter les lignes Firebase
6. Dans `build.gradle.kts` (root) : décommenter le plugin google-services
7. Dans `app/build.gradle.kts` : décommenter les dépendances Firebase

## Dépendances principales

- **Room** `2.6.1` — persistance locale SQLite
- **Retrofit + Gson** `2.9.0` — appels API REST
- **Coil** `2.5.0` — chargement images depuis URL (équivalent Glide pour Compose)
- **osmdroid** `6.1.11` — cartes OpenStreetMap
- **play-services-location** `21.3.0` — GPS fusionné
- **Firebase BOM** `32.7.0` *(optionnel, W7)*

## Structure des fichiers

```
app/src/main/java/com/example/myapplication/
├── MainActivity.kt          ← Page1 (accueil météo), Page2 (DB), Page3 (settings)
├── OpenStreetMapsActivity.kt ← Carte avec marqueurs GPS + Room DB
├── FirebaseActivity.kt      ← Auth + Realtime Database (W7)
├── network/
│   ├── WeatherApiService.kt ← Interface Retrofit
│   └── WeatherResponse.kt   ← Modèles JSON météo
└── room/
    ├── AppDatabase.kt       ← Singleton Room
    ├── CoordinatesEntity.kt ← Table "coordinates"
    └── ICoordinatesDao.kt   ← DAO (insert, getAll, delete, update)
```
