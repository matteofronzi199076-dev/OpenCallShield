# OpenCallShield 1.4.3 IT Estero

APK debug firmato, in italiano, con target Android 16 (API 36), installabile accanto alle precedenti edizioni. Pacchetto: `com.matteofronzi.opencallshield.it.esteri.debug`. I dati delle altre edizioni rimangono nei rispettivi pacchetti e non vengono trasferiti automaticamente.

## Installazione

1. Nella scheda Actions apri una compilazione riuscita di **APK italiano 1.4.3 con blocco estero automatico**.
2. Scarica l'artefatto **OpenCallShield-1.4.3-italiano-estero-debug**, estrai lo ZIP e installa il file APK contenuto.
3. Apri **OpenCallShield IT Estero**, concedi l'accesso ai contatti e premi **Attiva come app per filtrare le chiamate**. Scegli questa edizione nella finestra di Android. Android permette un solo filtro chiamate attivo alla volta.

Il blocco estero è già attivo: rifiuta le chiamate con prefisso internazionale diverso da +39, riconoscendo sia + sia 00. Non è necessario selezionare i Paesi. I numeri italiani con +39/0039 e i numeri nazionali restano consentiti salvo le altre regole di blocco. Senza prefisso internazionale non si può dedurre il Paese. Il numero presentato dalla rete può essere falsificato: il filtro non verifica la posizione geografica del chiamante.

I contatti salvati hanno sempre la precedenza quando il permesso Contatti è concesso. Restano disponibili la lista nera, il blocco dei numeri sconosciuti, i prefissi personalizzati, il blocco per Paese, la cronologia, la modalità silenziosa e la sincronizzazione. La modalità silenziosa è disattivata di default, quindi il filtro rifiuta le chiamate bloccate.

## Database gratuito

La nuova installazione usa direttamente:

https://raw.githubusercontent.com/matteofronzi199076-dev/OpenCallShield/main/data/italia/spam_numbers.json

Il lavoro periodico viene registrato al primo avvio e sincronizza quando la rete è disponibile; poi ogni 24 ore, secondo la pianificazione di Android. **Sincronizza ora** permette di confermare subito il download. Su OPPO, se gli aggiornamenti vengono ritardati, consenti il lavoro in background nelle impostazioni della batteria dell'app.

Al momento della preparazione sono presenti 813 record originali e 3 numeri italiani, ciascuno in tre forme (+39, nazionale, 0039), per 822 record totali. La copertura italiana è limitata. Fonti: OpenCallShield di jhonsu01 (MIT), Blocklist telefonica Italia della community Kallm / thesqual87 (dati CC BY-SA 4.0). Conversione CSV in JSON, unione e alias dei numeri sono le modifiche apportate. Attribuzione, licenza e provenienza: [documentazione del database](https://github.com/matteofronzi199076-dev/OpenCallShield/blob/main/data/italia/README.md), [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).

Non viene importato il database proprietario Truecaller. La sincronizzazione originale aggiunge/aggiorna record senza cancellare quelli locali: una rimozione dalla fonte non cancella automaticamente tutte le copie già importate sul telefono.

## Verifiche

Il workflow esegue i test delle regole, assembleDebug e lintDebug con Gradle. Verifica inoltre firma APK, pacchetto, versione, lingua dell'etichetta e SDK 36. I test comprendono contatti prioritari, prefissi esteri +/00, numeri italiani fissi e mobili, lista SPAM, prefissi manuali, numeri nascosti e disattivazione del blocco estero. Le prove software non sostituiscono una chiamata reale sull'OPPO.
