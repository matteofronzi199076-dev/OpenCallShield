# OpenCallShield 1.4.3 in italiano

Questa edizione mantiene le funzioni del sorgente originale e usa il pacchetto `com.matteofronzi.opencallshield.it.debug`, distinto da quello della versione spagnola.

## Scaricare e installare

1. Apri la compilazione **APK italiano 1.4.3** nella scheda **Actions** di questo repository.
2. Dopo una compilazione riuscita, scarica l’artefatto **OpenCallShield-1.4.3-italiano-debug**.
3. Estrai lo ZIP e apri `OpenCallShield-1.4.3-italiano-debug.apk` sull’OPPO Reno14 F 5G.
4. Se Android lo richiede, consenti l’installazione da questa origine per il browser o il gestore file utilizzato.
5. Avvia **OpenCallShield IT**, concedi l’accesso ai contatti per permettere le chiamate dei contatti conosciuti e premi **Attiva come app per filtrare le chiamate**.
6. Seleziona **OpenCallShield IT** nella finestra di Android, quindi configura le regole di blocco e premi **Sincronizza ora**.

Entrambe le versioni possono restare installate. Android permette un solo filtro chiamate attivo alla volta. Le preferenze, la lista locale e la cronologia sono separate tra le due applicazioni; la sincronizzazione continua a utilizzare il database collaborativo originale.

L’APK è una build debug firmata. Il workflow verifica compilazione Gradle, lint, firma, versione e pacchetto. Il progetto usa `compileSdk` e `targetSdk` 36 per Android 16 e `minSdk` 29. La compilazione non sostituisce la prova delle chiamate sul dispositivo OPPO.
