## Funktionale Anforderungen

### Datenmodell & Hierarchie

**F1** Das System muss eine hierarchische Struktur aus Räumen, Regalen, Kisten und Gegenständen abbilden können.

**F2** Das System muss beliebig tiefe Verschachtelungen von Kisten ermöglichen (Kiste in Kiste in Kiste...).

**F3** Das System muss erlauben, dass Gegenstände direkt in Räumen platziert werden können (ohne Regal/Kiste).

**F4** Das System muss erlauben, dass Gegenstände direkt in Regalen platziert werden können (ohne Kiste).

**F5** Das System muss für jedes Element (Raum, Regal, Kiste, Gegenstand) eine eindeutige Identifikation ermöglichen.

### Positionsangaben

**F6** Das System muss die Möglichkeit bieten, für Gegenstände Positionsangaben innerhalb ihres Containers zu erfassen (z.B. "hintere linke Ecke", "oberste Regalebene").

**F7** Das System muss für Kisten Positionsangaben innerhalb von Regalen oder Räumen speichern können.

**F8** Das System muss für Regale Positionsangaben innerhalb von Räumen speichern können.

### Suchfunktionalität

**F9** Das System muss eine Suche nach Gegenständen anhand ihres Namens ermöglichen.

**F10** Das System muss bei einem gefundenen Gegenstand den vollständigen Pfad zum Standort anzeigen (z.B. "Raum → Regal → Kiste → Gegenstand").

**F11** Das System muss eine Filterung/Suche nach Standorten (Räume, Regale, Kisten) ermöglichen.

**F12** Die Suchfunktion muss auch bei Offline-Betrieb verfügbar sein.

### Verwaltung von Standorten

**F13** Das System muss das Anlegen neuer Räume ermöglichen.

**F14** Das System muss das Anlegen neuer Regale in Räumen ermöglichen.

**F15** Das System muss das Anlegen neuer Kisten ermöglichen.

**F16** Das System muss das Anlegen neuer Gegenstände ermöglichen.

### Umräumen & Verschieben

**F17** Das System muss das Verschieben einzelner Gegenstände zwischen Containern (Kisten/Regale/Räume) ermöglichen.

**F18** Das System muss das Verschieben ganzer Kisten zwischen Standorten ermöglichen.

**F19** Das System muss beim Verschieben von Kisten alle enthaltenen Gegenstände automatisch mit verschieben.

**F20** Verschiebe-Operationen müssen auch offline durchführbar sein.

### Temporäre Standorte

**F21** Das System muss die Möglichkeit bieten, temporäre Standorte zu definieren (z.B. "Ferienfreizeit 2025").

**F22** Das System muss das Verschieben von Kisten zu temporären Standorten ermöglichen.

**F23** Das System muss beim Zurückräumen von temporären Standorten den ursprünglichen Standort anzeigen/vorschlagen können.

**F24** Das System muss kennzeichnen können, welche Kisten sich aktuell an temporären Standorten befinden.

### Bearbeitung & Löschung

**F25** Das System muss das Bearbeiten von Namen und Beschreibungen aller Elemente ermöglichen.

**F26** Das System muss das Löschen von Elementen ermöglichen, sofern diese leer sind.

**F27** Das System muss beim Versuch, nicht-leere Container zu löschen, eine Warnung ausgeben.
### Mengenverwaltung & Mehrfachstandorte

**F28** Das System muss die Möglichkeit bieten, mehrere Exemplare desselben Gegenstands als einen logischen Gegenstand zu verwalten.

**F29** Das System muss die Gesamtanzahl vorhandener Exemplare eines Gegenstands anzeigen können.

**F30** Das System muss bei Gegenständen mit mehreren Exemplaren alle Standorte gruppiert anzeigen können.

**F31** Das System muss bei der Suche Gegenstände mit mehreren Standorten als ein Suchergebnis mit allen Standorten darstellen.

**F32** Das System muss das Verschieben einzelner Exemplare zwischen Standorten ermöglichen, während andere Exemplare am ursprünglichen Ort bleiben.

**F33** Das System muss die Anzahl der Exemplare pro Standort verwalten können (z.B. "5 Walkie-Talkies in Kiste A, 5 in Kiste B").

### Erweiterte Suchfunktionalität

**F34** Das System muss bei der Suche ähnliche Begriffe erkennen und entsprechende Ergebnisse liefern (z.B. "Handy" findet "Smartphone").

**F35** Das System muss Synonyme und alternative Bezeichnungen für Gegenstände verwalten können.

**F36** Die Suche muss Tippfehler tolerieren und trotzdem passende Ergebnisse liefern (Fuzzy-Search).

### Tag-System

**F37** Das System muss Gegenständen automatisch Tags zuordnen können (z.B. "Technik", "Basteln", "Outdoor-Spielzeug").

**F38** Das System muss die Möglichkeit bieten, Tags manuell hinzuzufügen oder zu entfernen.

**F39** Das System muss eine Suche/Filterung nach Tags ermöglichen.

**F40** Das System muss mehrere Tags pro Gegenstand unterstützen.

**F41** Das System muss eine Übersicht aller verfügbaren Tags anzeigen können.

**F42** Das System muss die Anzahl der Gegenstände pro Tag anzeigen können.

### Verleihverwaltung (zukünftig)

**F43** _(Zukunft)_ Das System muss die Planung zukünftiger Verleihvorgänge ermöglichen.

**F44** _(Zukunft)_ Das System muss erfassen können, welcher Gegenstand aktuell an wen verliehen ist.

**F45** _(Zukunft)_ Das System muss erfassen können, von wann bis wann ein Gegenstand verliehen ist.

**F46** _(Zukunft)_ Das System muss anzeigen können, welche Gegenstände aktuell verliehen sind.

**F47** _(Zukunft)_ Das System muss bei der Planung von Verleihvorgängen Konflikte erkennen (Gegenstand bereits verliehen).

**F48** _(Zukunft)_ Das System muss eine Übersicht über vergangene Verleihvorgänge bieten.

**F49** _(Zukunft)_ Das System muss bei verliehenen Gegenständen mit mehreren Exemplaren die Anzahl verfügbarer Exemplare berücksichtigen.

**F50** _(Zukunft)_ Das System muss Erinnerungen für Rückgabetermine ermöglichen.

### Barcode/QR-Code-Scanning

**F51** Das System muss das Scannen von Barcodes und QR-Codes über die Kamera des Geräts ermöglichen.

**F52** Das System muss beim Anlegen von Gegenständen die Möglichkeit bieten, einen Barcode/QR-Code zu erfassen und zu speichern.

**F53** Das System muss beim Scannen eines Barcodes/QR-Codes den zugehörigen Gegenstand direkt anzeigen.

**F54** Das System muss das Generieren von QR-Codes für Gegenstände, Kisten, Regale und Räume ermöglichen.

**F55** Das System muss generierte QR-Codes zum Ausdrucken bereitstellen (als PDF oder Bild).

**F56** Das System muss beim Scannen eines Container-QR-Codes (Kiste/Regal) alle enthaltenen Gegenstände anzeigen.

**F57** Das System muss das schnelle Umräumen per QR-Code ermöglichen (Gegenstand scannen → Zielort scannen → fertig).

**F58** Das System muss auch bei Offline-Betrieb QR-Code-Scanning ermöglichen.

**F59** Das System muss die Möglichkeit bieten, mehrere Gegenstände nacheinander zu scannen (Batch-Scanning).

**F60** Das System muss beim Scannen unbekannter Barcodes/QR-Codes die Möglichkeit bieten, einen neuen Gegenstand anzulegen.

### Fotos von Gegenständen

**F61** Das System muss das Hinzufügen von Fotos zu Gegenständen ermöglichen.

**F62** Das System muss das Aufnehmen von Fotos direkt über die Kamera ermöglichen.

**F63** Das System muss das Hochladen vorhandener Fotos aus der Galerie ermöglichen.

**F64** Das System muss mehrere Fotos pro Gegenstand unterstützen (mindestens 5 Fotos).

**F65** Das System muss Fotos in der Gegenstandsansicht prominent anzeigen.

**F66** Das System muss bei der Suche optional Vorschaubilder der gefundenen Gegenstände anzeigen.

**F67** Das System muss das Löschen einzelner Fotos ermöglichen.

**F68** Das System muss auch für Kisten Fotos ermöglichen (z.B. Foto des Kisten-Labels).

**F69** Das System sollte die Möglichkeit bieten, ein Hauptfoto pro Gegenstand zu definieren.

**F70** Fotos müssen auch offline verfügbar sein (lokale Speicherung).

### History/Audit-Log

**F71** Das System muss alle Änderungen an Gegenständen, Kisten, Regalen und Räumen protokollieren.

**F72** Das System muss für jeden Logeintrag erfassen: Zeitpunkt, Art der Änderung, betroffenes Objekt, vorheriger und neuer Wert.

**F73** Das System muss eine chronologische Übersicht aller Änderungen anzeigen können.

**F74** Das System muss die Möglichkeit bieten, die History nach Objekten zu filtern (z.B. "alle Änderungen an Kiste XY").

**F75** Das System muss die Möglichkeit bieten, die History nach Aktionstypen zu filtern (z.B. nur "Verschieben", nur "Umbenennen").

**F76** Das System muss die Möglichkeit bieten, die History nach Zeiträumen zu filtern.

**F77** Das System muss bei Verschiebe-Vorgängen den vollständigen Pfad von Quelle und Ziel protokollieren.

**F78** Das System muss bei Gegenständen mit mehreren Exemplaren Mengenänderungen protokollieren.

**F79** Das System sollte die Möglichkeit bieten, einzelne Änderungen rückgängig zu machen (Undo-Funktion).

**F80** _(Zukunft mit Berechtigungssystem)_ Das System sollte bei Mehrbenutzer-Betrieb erfassen, welcher Benutzer welche Änderung vorgenommen hat.

### Export-Funktionen

**F81** Das System muss den Export einer vollständigen Inventarliste ermöglichen.

**F82** Das System muss den Export in verschiedenen Formaten unterstützen (mindestens CSV und PDF).

**F83** Das System muss den Export einzelner Räume oder Kisten ermöglichen.

**F84** Das System muss beim Export die Möglichkeit bieten, die hierarchische Struktur abzubilden.

**F85** Das System muss beim Export die Möglichkeit bieten, Fotos einzubeziehen (optional).

**F86** Das System muss Packzettel für temporäre Standorte generieren können (z.B. "alle Kisten für Ferienfreizeit 2025").

**F87** Das System muss beim Export die Möglichkeit bieten, nur bestimmte Informationen auszuwählen (z.B. nur Namen, oder inkl. Mengen, Tags, etc.).

**F88** Das System muss Etiketten für Kisten generieren können (mit QR-Code, Name, Inhaltsliste).

**F89** Das System sollte die Möglichkeit bieten, Export-Vorlagen zu speichern (z.B. "Standard-Inventarliste", "Packzettel-Format").

**F90** Exporte müssen auch offline erstellt werden können (Speicherung lokal, später Upload bei Online-Verbindung).

**F91** Das System sollte Excel-Export (.xlsx) unterstützen mit separaten Sheets für Räume, Regale, Kisten, Gegenstände.

**F92** Das System sollte die Möglichkeit bieten, QR-Code-Bögen für mehrere Objekte auf einmal zu exportieren (z.B. alle Kisten eines Raums).
### Multi-Device & Synchronisation

**F93** Das System muss von mehreren Geräten gleichzeitig nutzbar sein.

**F94** Das System muss über einen Webbrowser auf Desktop-Computern zugänglich sein.

**F95** Das System muss über einen Webbrowser auf mobilen Geräten zugänglich sein.

**F96** Das System muss Änderungen zwischen allen verbundenen Geräten synchronisieren.

**F97** Das System muss anzeigen, wenn ein anderes Gerät gerade Änderungen vornimmt (Echtzeit-Updates).

**F98** Das System muss bei gleichzeitigen Änderungen auf verschiedenen Geräten Konflikte erkennen.

**F99** Das System muss bei Synchronisationskonflikten eine Konfliktlösung anbieten (z.B. "letzte Änderung gewinnt" oder manuelle Auswahl).

**F100** Das System muss auch nach längerer Offline-Nutzung mehrerer Geräte eine konsistente Synchronisation ermöglichen.

### Benutzerverwaltung

**F101** Das System muss ein Login-System bereitstellen, um von verschiedenen Geräten auf dieselben Daten zuzugreifen.

**F102** Das System muss die Möglichkeit bieten, auf mehreren Geräten gleichzeitig eingeloggt zu sein.

**F103** Das System sollte die Möglichkeit bieten, aktive Sessions/Geräte anzuzeigen.

**F104** Das System sollte die Möglichkeit bieten, einzelne Sessions remote abzumelden (z.B. bei verlorenem Gerät).

### Datenhosting & Zugriff

**F105** Das System muss alle Daten zentral speichern und über das Web bereitstellen.

**F106** Das System muss auch bei schlechter Internetverbindung nutzbar bleiben (Progressive Web App Prinzip).

**F107** Das System muss automatisch zwischen Online- und Offline-Modus wechseln.

**F108** Das System muss beim Wechsel von Offline zu Online automatisch synchronisieren.

---

## Nicht-funktionale Anforderungen

### Performance

**NF1** Die Suche nach Gegenständen muss innerhalb von maximal 2 Sekunden ein Ergebnis liefern.

**NF2** Das Verschieben von Kisten muss innerhalb von maximal 1 Sekunde bestätigt werden.

**NF3** Das System muss mindestens 10.000 Gegenstände performant verwalten können.

**NF24** Bei Gegenständen mit mehreren Standorten soll die Anzeige übersichtlich und auf einen Blick erfassbar sein.

**NF25** Die Tag-Übersicht sollte visuell ansprechend gestaltet sein (z.B. mit Farben oder Icons).

**NF26** _(Zukunft)_ Die Verleihverwaltung sollte kalenderbasiert visualisiert werden können.

**NF32** QR-Code-Scanning muss innerhalb von maximal 1 Sekunde einen Gegenstand identifizieren.

**NF33** Fotos müssen automatisch komprimiert werden, um Speicherplatz zu sparen (ohne sichtbaren Qualitätsverlust).

**NF34** Die History-Ansicht muss auch bei >10.000 Logeinträgen performant sein (Pagination/Lazy Loading).

**NF35** PDF-Exporte für große Inventarlisten (>1000 Gegenstände) müssen innerhalb von maximal 10 Sekunden generiert werden.

**NF36** Foto-Upload und -Komprimierung sollten im Hintergrund erfolgen und die UI nicht blockieren.

### Offline-Fähigkeit

**NF4** Das System muss vollständig offline nutzbar sein (alle Funktionen).

**NF5** Offline vorgenommene Änderungen müssen lokal gespeichert werden.

**NF6** Bei Wiederherstellung der Internetverbindung müssen Offline-Änderungen automatisch synchronisiert werden.

**NF7** Bei Synchronisationskonflikten muss das System eine Konfliktlösung anbieten.

**NF8** Das System muss anzeigen, ob es online oder offline arbeitet.

### Usability

**NF9** Die Benutzeroberfläche muss auf mobilen Geräten (Smartphones) gut bedienbar sein.

**NF10** Das Verschieben von Kisten muss mit maximal 3 Klicks/Taps möglich sein.

**NF11** Das System muss eine intuitive Navigation durch die Hierarchie ermöglichen.

**NF12** Das System sollte Touch-freundliche Eingabeelemente verwenden (große Buttons, Swipe-Gesten).

**NF41** Die Kamera-Schnittstelle für Barcode-Scanning und Foto-Aufnahme muss intuitiv bedienbar sein.

**NF42** Das System sollte visuelles und haptisches Feedback beim erfolgreichen Scannen geben.

**NF43** Die History sollte in einer Timeline-Ansicht darstellbar sein.

**NF44** Export-Formate sollten direkt teilbar sein (z.B. per E-Mail, Cloud-Dienste).

**NF45** Generierte QR-Code-Etiketten sollten direkt druckbar sein (Standard-Etikettengrößen).

**NF46** Bei Foto-Uploads sollte ein Fortschrittsbalken angezeigt werden.

### Zuverlässigkeit

**NF13** Das System darf bei Offline-Betrieb keine Daten verlieren.

**NF14** Das System muss eine Datensicherung ermöglichen.

**NF15** Das System muss Transaktionssicherheit bei Verschiebe-Operationen gewährleisten (alles oder nichts).

**NF47** Foto-Uploads müssen bei Netzwerkunterbrechung automatisch fortgesetzt werden.

**NF48** Die History darf auch bei Systemfehlern keine Einträge verlieren.

**NF49** QR-Codes müssen auch bei leichter Beschädigung oder schlechten Lichtverhältnissen lesbar sein.

**NF50** Export-Vorgänge müssen bei Abbruch oder Fehler den vorherigen Zustand wiederherstellen.

### Plattform

**NF16** Das System sollte als Progressive Web App (PWA) oder native App verfügbar sein.

**NF17** Das System sollte auf Android und iOS lauffähig sein.

**NF18** Das System sollte auch im Browser nutzbar sein.

### Wartbarkeit

**NF19** Der Code sollte modular aufgebaut sein.

**NF20** Das System sollte erweiterbar sein für zukünftige Features (z.B. Barcode-Scanning).
### Intelligenz & Automatisierung

**NF27** Die automatische Tag-Zuordnung sollte eine Genauigkeit von mindestens 80% erreichen.

**NF28** Das System sollte aus Korrekturen bei Tag-Zuordnungen lernen können (Machine Learning optional).

**NF29** Die Synonym-Erkennung sollte kontextbezogen arbeiten.

### Datenintegrität (Ergänzungen)

**NF30** Bei Gegenständen mit Mengenangaben muss die Summe aller Teilmengen immer der Gesamtmenge entsprechen.

**NF31** _(Zukunft)_ Bei Verleihvorgängen muss sichergestellt sein, dass nicht mehr Exemplare verliehen werden als vorhanden sind.
### Speicher & Datenmenge

**NF37** Fotos sollten automatisch auf eine maximale Auflösung von 1920x1080 Pixel verkleinert werden.

**NF38** Das System sollte pro Foto maximal 500 KB Speicherplatz benötigen (nach Komprimierung).

**NF39** Die History sollte eine konfigurierbare maximale Länge haben (z.B. letzten 12 Monate, oder max. 50.000 Einträge).

**NF40** Das System sollte beim Erreichen von Speicherlimits den Benutzer informieren.
### Datenschutz & Sicherheit

**NF51** Fotos müssen lokal gesichert werden und dürfen nur mit expliziter Zustimmung in die Cloud synchronisiert werden.

**NF52** Die History darf keine sensiblen Informationen im Klartext speichern.

**NF53** Export-Dateien sollten optional mit einem Passwort geschützt werden können.
### Kompatibilität

**NF54** QR-Code-Scanning muss mit Standard-Smartphone-Kameras funktionieren (keine Spezial-Hardware erforderlich).

**NF55** Generierte QR-Codes müssen mit Standard-QR-Code-Readern (auch außerhalb der App) lesbar sein.

**NF56** Export-Formate müssen mit gängiger Software (Excel, PDF-Reader, etc.) kompatibel sein.

**NF57** Die Foto-Funktion muss auf iOS und Android gleichermaßen funktionieren.

### Performance (Multi-Device)

**NF58** Die Synchronisation zwischen Geräten muss innerhalb von maximal 5 Sekunden erfolgen.

**NF59** Das System muss auch bei langsamer Internetverbindung (3G/Edge) nutzbar sein.

**NF60** Änderungen auf einem Gerät müssen auf anderen Geräten innerhalb von maximal 10 Sekunden sichtbar sein.

**NF61** Die initiale Synchronisation beim ersten Login auf einem neuen Gerät sollte auch für große Datenbestände (>5000 Gegenstände) unter 30 Sekunden dauern.

**NF62** Das System sollte Delta-Synchronisation nutzen (nur Änderungen übertragen, nicht kompletter Datenbestand).

### Verfügbarkeit & Zuverlässigkeit

**NF63** Das Backend muss eine Verfügbarkeit von mindestens 99% gewährleisten.

**NF64** Das System muss auch bei Ausfall des Servers offline weiterarbeiten können.

**NF65** Bei Server-Ausfall dürfen keine Daten verloren gehen (lokale Persistierung).

**NF66** Das System muss nach Server-Wiederherstellung alle während des Ausfalls vorgenommenen Änderungen synchronisieren.

**NF67** Das System sollte regelmäßige automatische Backups erstellen.

### Skalierbarkeit

**NF68** Das System muss mindestens 10 gleichzeitig aktive Geräte pro Benutzer unterstützen.

**NF69** Das System sollte für bis zu 50 Benutzer (für mögliche zukünftige Team-Nutzung) skalierbar sein.

**NF70** Die Datenbank muss für mindestens 100.000 Gegenstände ausgelegt sein.

**NF71** Das System sollte mit steigender Datenmenge nicht signifikant langsamer werden.

### Sicherheit

**NF72** Die Kommunikation zwischen Client und Server muss verschlüsselt sein (HTTPS/TLS).

**NF73** Passwörter müssen sicher gehasht gespeichert werden (z.B. bcrypt, Argon2).

**NF74** Das System muss vor Cross-Site-Scripting (XSS) Angriffen geschützt sein.

**NF75** Das System muss vor Cross-Site-Request-Forgery (CSRF) Angriffen geschützt sein.

**NF76** Das System muss vor SQL-Injection geschützt sein.

**NF77** Das System sollte Rate-Limiting implementieren, um Brute-Force-Angriffe zu verhindern.

**NF78** Authentifizierungs-Tokens müssen eine begrenzte Gültigkeitsdauer haben.

**NF79** Das System sollte Two-Factor-Authentication (2FA) optional unterstützen.

### Usability (Multi-Device)

**NF80** Das User Interface muss responsive sein und sich an verschiedene Bildschirmgrößen anpassen (Desktop, Tablet, Smartphone).

**NF81** Die wichtigsten Funktionen (Suchen, Verschieben) müssen auf allen Gerätetypen gleich gut bedienbar sein.

**NF82** Das System sollte auf Touch-Geräten und mit Maus/Tastatur gleichermaßen gut bedienbar sein.

**NF83** Der Synchronisationsstatus muss für den Benutzer klar ersichtlich sein (z.B. Icon, Status-Anzeige).

**NF84** Bei Synchronisationskonflikten muss die Auflösung benutzerfreundlich gestaltet sein.

**NF85** Das System sollte auch bei langsamer Verbindung visuelles Feedback geben (z.B. Lade-Indikatoren).

### Datenintegrität (Multi-Device)

**NF86** Bei gleichzeitiger Bearbeitung desselben Objekts auf mehreren Geräten muss die Datenkonsistenz gewährleistet sein.

**NF87** Das System muss Optimistic Locking oder ähnliche Mechanismen implementieren.

**NF88** Beim Synchronisieren dürfen keine Daten überschrieben werden, ohne dass der Benutzer darüber informiert wird.

**NF89** Das System muss Transaktionssicherheit auch über Geräte-Grenzen hinweg gewährleisten.

### Browser-Kompatibilität

**NF90** Das System muss in allen gängigen modernen Browsern funktionieren (Chrome, Firefox, Safari, Edge - jeweils aktuelle Version).

**NF91** Das System sollte auch mit Browser-Versionen funktionieren, die bis zu 2 Jahre alt sind.

**NF92** Alle Kernfunktionen müssen auch ohne JavaScript-Frameworks nutzbar sein (Progressive Enhancement).

### Offline-Storage & Caching

**NF93** Das System muss ausreichend Daten lokal cachen, um auch längere Offline-Phasen (mehrere Tage) zu überbrücken.

**NF94** Fotos müssen intelligent gecacht werden (häufig verwendete Fotos priorisieren).

**NF95** Das System sollte die Möglichkeit bieten, den lokalen Cache manuell zu löschen.

**NF96** Der lokale Speicherverbrauch sollte konfigurierbar sein (z.B. max. 500 MB, 1 GB, etc.).

### Wartbarkeit & Deployment

**NF97** Das System sollte Container-basiert deployt werden können (Docker).

**NF98** Updates sollten ohne Downtime eingespielt werden können (Zero-Downtime-Deployment).

**NF99** Das System sollte automatische Datenbank-Migrationen unterstützen.

**NF100** Es sollte ein Monitoring-System geben, das Server-Status und Performance überwacht.