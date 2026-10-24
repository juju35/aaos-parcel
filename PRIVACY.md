# Politique de confidentialité — Parcel pour Android Automotive

*Dernière mise à jour : 24 octobre 2026*

Parcel pour Android Automotive (`me.jstephan.parcel`) est une application Android Automotive OS publiée par juju35.
Elle n'affiche aucune publicité et ne contient aucun outil de mesure d'audience, de suivi ou de rapport de plantage.
Elle n'est pas éditée par Parcel.

## Clé d'API Parcel

L'application a besoin de votre clé d'API Parcel, générée sur web.parcelapp.net, pour lire les colis suivis dans
votre compte Parcel.

- La clé est enregistrée uniquement sur le véhicule, exclue des sauvegardes Android.
- Elle n'est envoyée qu'à l'API de Parcel (api.parcel.app), chiffrée (HTTPS), dans chaque requête.
- La supprimer depuis les réglages de l'application efface aussi la liste des colis enregistrée.

## Données échangées avec Parcel

L'application interroge api.parcel.app au plus une fois par heure en arrière-plan, et à l'ouverture si la dernière
vérification date de plus de 5 minutes. Parcel renvoie vos colis (description, transporteur, numéro de suivi,
statut, événements). L'application télécharge aussi, une fois par jour, la liste publique des transporteurs.
Aucune autre donnée, ni la position du véhicule, n'est envoyée.

Parcel traite ces données selon sa propre politique de confidentialité : https://parcelapp.net

## Données enregistrées sur l'appareil

- Vos réglages (colis affichés, notifications), qui peuvent être inclus dans la sauvegarde Android du profil.
- La dernière liste de colis et la liste des transporteurs, pour l'affichage immédiat et la détection des mises à
  jour, jamais sauvegardées.

Désinstaller l'application, ou effacer ses données dans les paramètres, supprime tout.

## Enfants

L'application s'adresse aux conducteurs et ne vise pas les enfants.

## Contact

Questions ou demandes : https://github.com/juju35/aaos-parcel/issues

---

# Privacy policy — Parcel for Android Automotive (English)

No ads, no analytics, tracking or crash reporting; not made by Parcel. The app needs your Parcel API key to read the
deliveries of your Parcel account: the key stays on the vehicle, is left out of Android backups, and is only sent over
HTTPS to Parcel's API (api.parcel.app), at most once an hour in the background and when the app opens. The app also
downloads Parcel's public carriers list once a day. No other data, and not the vehicle's location, is sent. The app
keeps your settings (which Android may back up) and the last deliveries list on the device; deleting the key,
uninstalling or clearing the app's data removes them. Contact: https://github.com/juju35/aaos-parcel/issues
