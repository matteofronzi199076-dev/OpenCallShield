# Lista SPAM originale con segnalazioni italiane

URL da utilizzare nell'app:

```
https://raw.githubusercontent.com/matteofronzi199076-dev/OpenCallShield/main/data/italia/spam_numbers.json
```

## Attivazione nell'APK già installato

1. Apri **OpenCallShield IT → Protezione → Impostazioni avanzate**.
2. Nel campo **URL del database collaborativo (JSON)** sostituisci il collegamento con quello qui sopra.
3. Premi **Sincronizza ora**. I numeri già presenti e quelli segnalati da te restano nell'app.

L'app controlla questa stessa fonte anche con il worker giornaliero già presente, quando dispone di connessione. La generazione della lista nel repository è pianificata ogni giorno alle 03:17 UTC; GitHub può ritardare l'esecuzione.

## Copertura e fonti

La lista combina il database originale [OpenCallShield](https://github.com/jhonsu01/OpenCallShield) con [Blocklist telefonica Italia](https://github.com/thesqual87/blocklist-telefonica-italia), mantenuta dalla community Kallm. La fonte italiana attualmente contiene **3 numeri segnalati**: la copertura iniziale è limitata e non è una lista completa dello SPAM nazionale. I riscontri sono quelli dichiarati dal mantenitore della fonte, non una verifica indipendente di ogni chiamante.

Ogni numero italiano esatto compare nel formato `+39`, in quello nazionale e con `0039`, perché la versione dell'app installata confronta i numeri come stringhe. Per i telefoni fissi lo zero iniziale viene mantenuto. Non vengono aggiunti prefissi generici per bloccare l'intera Italia.

Lo script conserva tutte le voci del database originale e non incrementa il conteggio delle segnalazioni a ogni sincronizzazione. Se una fonte manca, è vuota o contiene dati non validi, la generazione fallisce e la lista pubblicata resta disponibile nella versione precedente.

Attribuzioni, versioni delle fonti e conteggi sono in [sources.json](sources.json). Il riuso dei dati derivati è soggetto a [CC BY-SA 4.0](LICENSE); la licenza del codice originale resta MIT.

## Rimozioni e falsi positivi

La sincronizzazione dell'APK attuale è cumulativa: aggiunge numeri, ma non elimina automaticamente quelli rimossi dalla fonte. Se un numero risulta legittimo, rimuovi dalla **Lista SPAM** dell'app tutte le sue forme (`+39`, nazionale e `0039`); finché compare ancora nella fonte, una sincronizzazione può aggiungerlo di nuovo. I contatti riconosciuti vengono sempre consentiti se l'app ha l'autorizzazione ai contatti.

Per contestare un numero italiano nella fonte pubblica, usa la [pagina delle segnalazioni della fonte](https://github.com/thesqual87/blocklist-telefonica-italia/issues). Per interrompere l'importazione italiana puoi ripristinare l'URL originale:

```
https://raw.githubusercontent.com/jhonsu01/OpenCallShield/main/spam_numbers.json
```

Le voci italiane già scaricate devono eventualmente essere eliminate manualmente dalla lista nell'app.

Questo ramo viene usato per verificare il download live delle fonti e i test dell'importatore. L'URL pubblico da usare nell'app resta quello del ramo `main` indicato sopra.
