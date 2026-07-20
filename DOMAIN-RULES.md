# Domain Rules

Aus der Liste der Commands müssen sich ALLE Daten abbilden lassen. Die Commands sind die Source of Truth, jede andere Repräsentation ist nur ein Snapshot und verliert im Falle eines Konflikts.

Wurde ein Command einmal erfolgreich an den Server gesendet, darf er nicht mehr verändert werden.

Die Commands werden in einer doppelt verketteten Liste gespeichert. Jeder Command hat also einen Vorgänger und einen Nachfolger. Ausnahmen bilden nur der "Root" Command und der aktuelle HEAD.
Wurde einmal ein child bzw parent command gesetzt, darf dieser nicht mehr verändert werden.

Der ROOT Command wird Serverseitig einmal angelegt:

```json
{
  "id": "111111111111-1111-1111-1111-111111111111",
  "command_type": "CONTAINER_CREATE",
  "command_version": 1,
  "payload": {
    "id": "111111111111-1111-1111-1111-111111111111",
    "name": "Root Container",
    "created_at": "1970-01-01T00:00:00"
  }
}
```
Dieser Container/Command ist die einzige Ausnahme, wo kein parent Container/Command benötigt wird.
Dieser Container darf weder bearbeitet noch gelöscht werden.

## Erlaubte Commands

Ein Command hat folgende Felder:

| Feld            | Typ                                                                                                       |
|-----------------|-----------------------------------------------------------------------------------------------------------|
| id              | UUID                                                                                                      |
| command_type    | String. Erlaubte Werte werden unten aufgelistet                                                           |
| command_version | int. Default = 1. Wird hochgezählt (je command_type einzeln) wenn sich Verhalten des Command types ändert |
| payload         | ein Objekt in JSON. Felder sind Abhängig vom command_type                                                 |


### ITEM_CREATE

Pflichtfelder:

| Feld       | Typ                                 |
|------------|-------------------------------------|
| id         | UUID string                         |
| name       | string (not blank, min 3. chars)    |
| container  | UUID eines existierenden Containers |
| quantity   | int >= 1                            |
| created_at | Timestamp before now                |

Weitere erlaubte Felder (Optional):

| Feld        | Typ                               |
|-------------|-----------------------------------|
| description | String (Multiline allowed)        |
| position    | String                            |
| category    | UUID einer existierenden Category |

### ITEM_UPDATE

Nur enthaltene Felder werden geändert

Pflichtfelder:

| Feld       | Typ                                 |
|------------|-------------------------------------|
| id         | UUID string                         |

Weitere erlaubte Felder (Optional):

| Feld          | Typ                                                            |
|---------------|----------------------------------------------------------------|
| name          | string (not blank, min 3. chars)                               |
| container     | UUID eines existierenden Containers                            |
| category      | UUID einer existierenden Category                              |
| quantity      | int >= 1                                                       |
| description   | String (Multiline allowed)                                     |
| position      | String                                                         |
| primary_image | UUID eines Item_Images, dessen item feld auf dieses item zeigt |

### ITEM_DELETE

Nur enthaltene Felder werden geändert

Pflichtfelder:

| Feld       | Typ                                 |
|------------|-------------------------------------|
| id         | UUID string                         |

### CONTAINER_CREATE

Pflichtfelder:

| Feld       | Typ                                                                     |
|------------|-------------------------------------------------------------------------|
| id         | UUID string                                                             |
| name       | string (not blank, min 3. chars)                                        |
| parent     | UUID eines existierenden Containers. Keine Self oder Circular Reference |
| created_at | Timestamp before now                                                    |
TODO: Braucht es hier ein type Feld um zwischen RAUM, REGAL und KISTE zu unterscheiden??

Weitere erlaubte Felder (Optional):

| Feld        | Typ                               |
|-------------|-----------------------------------|
| description | String (Multiline allowed)        |
| position    | String                            |
| category    | UUID einer existierenden Category |

### CONTAINER_UPDATE

Pflichtfelder:

| Feld       | Typ                                                                     |
|------------|-------------------------------------------------------------------------|
| id         | UUID string                                                             |

Weitere erlaubte Felder (Optional):

| Feld        | Typ                                                                     |
|-------------|-------------------------------------------------------------------------|
| name        | string (not blank, min 3. chars)                                        |
| parent      | UUID eines existierenden Containers. Keine Self oder Circular Reference |
| category    | UUID einer existierenden Category                                       |
| description | String (Multiline allowed)                                              |
| position    | String                                                                  |

### CONTAINER_DELETE

Pflichtfelder:

| Feld       | Typ                                                                     |
|------------|-------------------------------------------------------------------------|
| id         | UUID string                                                             |

### CATEGORY_CREATE

Pflichtfelder:

| Feld       | Typ                              |
|------------|----------------------------------|
| id         | UUID string                      |
| name       | string (not blank, min 3. chars) |
| shortcode  | string max. 4 chars              |
| created_at | Timestamp before now             |

Weitere erlaubte Felder (Optional):

| Feld        | Typ                                                  |
|-------------|------------------------------------------------------|
| description | String (Multiline allowed)                           |
| hue         | int. Hue on the color circle for displaying purposes |

### CATEGORY_UPDATE

Pflichtfelder:

| Feld       | Typ                              |
|------------|----------------------------------|
| id         | UUID string                      |

Weitere erlaubte Felder (Optional):

| Feld        | Typ                                                  |
|-------------|------------------------------------------------------|
| name        | string (not blank, min 3. chars)                     |
| shortcode   | string max. 4 chars                                  |
| description | String (Multiline allowed)                           |
| hue         | int. Hue on the color circle for displaying purposes |

### CATEGORY_DELETE

Pflichtfelder:

| Feld       | Typ                              |
|------------|----------------------------------|
| id         | UUID string                      |

### ITEM_IMAGE_CREATE

Pflichtfelder:

| Feld       | Typ                        |
|------------|----------------------------|
| id         | UUID string                |
| item       | UUID of and exisiting item |
| created_at | Timestamp before now       |

### ITEM_IMAGE_DELETE

Pflichtfelder:

| Feld       | Typ                              |
|------------|----------------------------------|
| id         | UUID string                      |

### CONTAINER_IMAGE_CREATE

Pflichtfelder:

| Feld       | Typ                             |
|------------|---------------------------------|
| id         | UUID string                     |
| container  | UUID of and exisiting container |
| created_at | Timestamp before now            |
### CONTAINER_IMAGE_DELETE

Pflichtfelder:

| Feld       | Typ                              |
|------------|----------------------------------|
| id         | UUID string                      |

## Anwenden von Commands

Die geforderten Constraints auf den Feldern müssen nach jedem einzelnen Command erfüllt sein.

Daraus ergeben sich ein paar Implikationen für die Reihenfolge von Commands:
1. Ein Item/Container muss erstellt werden, bevor ein zugehöriges Bild erstellt werden kann
2. Um einen Container löschen zu können, müssen zuerst alle Container und Items gelöscht werden, die diesen Container referenzieren
3. Um eine Category löschen zu können, müssen zuerst alle Container und Items gelöscht werden, die diese Category referenzieren
4. Wenn ein item/container in primary_image ein image referenziert, muss diese Referenz geändert werden, bevor das Image gelöscht werden kann
5. Ein Container muss zuerst erstellt werden, bevor es Child Items oder Container geben kann

Wird ein Snapshot aus den Commands berechnet, muss immer beim root Container begonnen werden. Danach wird immer der Child Command als nächstes Angewendet.
Inkrementelle Snapshots sind auch erlaubt. Dazu muss immer der HEAD des aktuellen Snapshots gespeichert werden. Von diesem Command aus werden dann alle Child Commands angewendet

## Synchronisation

Client und Server kommunizieren nur über diese Schnittstellen:

GET fetchCommands?since=<Command UUID>
<linked list of commands>
POST applyCommands?head=<Command UUID>
<linked list of commands>

POST uploadImage/{Image UUID}
<Multipart Image Data>
GET images/{Image UUID}
<Multipart Image Data>

### Images

Images sind die einzigen Daten, die sich nicht vollständig aus den Commands wiederherstellen lassen. Über die beiden Image Schnittstellen lassen sich Images hoch- und herunterladen.
Der Server ist dabei ein Proxy für S3.
Clients sollten die Bilder lokal cachen um die Bilder auch offline anzeigen zu können.
Es gibt bewusst keine Möglichkeit ein Bild zu löschen um theoretisch zu jedem Zustand in der Command History zurückkehren zu können.

Der Server muss png und jpeg Bilder unterstützen. Der Upload endpoint sollte die Bilder auf max. 500KB komprimieren und maximal 5 MB große Bilder annehmen.
Clients sollten die Bilder schon vor dem Hochladen komprimieren.

### Commands

#### fetchCommands

Der Server muss verifizieren, dass since existiert und gibt sonst 404 zurück.
Ist since nicht gesetzt oder = null, dann werden ALLE Commands zurückgegeben.

Wenn since = head, dann wird eine leere Liste zurückgegeben.

Die Liste sollte mit dem aktuellen head enden und mit dem Element direkt nach since starten (since ist nicht enthalten)

#### applyCommands

head ist erforderlich. Wenn nicht gesetzt, null oder dandling reference wird 400 zurückgegeben.

Wenn head nicht dem tatsächlichen Head entspricht, wird 409 zurückgegeben → Der Client muss vorher änderungen herunterlanden.

Der Server wendet entweder alle Commands an oder lehnt alle ab.
Der Server muss sicherstellen, dass 
- nur gültige Command Types übermittelt wurden mit gültigen Command Versions.
- nur erlaubte Felder im Payload enthalten sind (je command_type bzw command_version)
- die constraints auf den Feldern zum Zeitpunk des Anwendens erfüllt sind → Dafür braucht der Server einen Snapshot der Daten
- die Commands von root → Head Anwendet werden
- der alte head mit dem root der Anfrage verbunden wird (child bzw parent setzen)

### Conflicts

Da der Server nur sync erlaubt, wenn Client und Server vom gleichen HEAD ausgehen, ist es die Verantwortung des Clients mit Konflikten umzugehen.
Bevor ein Client seine Änderungen hochladen darf, muss er alle Änderungen vom Server herunterladen und VOR seinen eigenen Änderungen anwenden.
Somit kann der Client entscheiden, ob er den User über Konflikte informiert und die Entscheidung über den "Gewinner" treffen lässt, oder einfach seine eigenen Änderungen Gewinnen lässt (Last-Write-Wins).

#### Client Implementation

Der Client speichert zwei Snapshots:
1. Synced State → Auf genau diesem Stand war der Server als das letzte Mal Änderungen heruntergeladen worden sind.
2. Pending State → Hier wurden alle Änderungen, die der Client nach dem letzten Sync gemacht hat schon angewendet.

Wenn der Client einen Konflikt beheben muss, kann der Client so den neuen Pending State basierend auf dem Synced State und den neuen Commands berechnen. Danach werden die eigenen Commands auf den Pending State angewendet. War der neue Upload dann erfolgreich, wird der Synced State = Pending State gesetzt

In beiden States wird außerdem die Command Linked List bis zum entsprechenden State gespeichert.

#### Conflict Definition

Zwei Änderungen werden dann als Konflikt betrachtet, wenn
1. Zwei Commands ein UPDATE auf das gleiche Feld der gleichen Entity durchführen wollen
2. Ein Command eine Entity updaten oder referenzieren will, die ein früherer Command gelöscht hat

Fall 1 kann durch Last-Write-Wins gelöst werden => Für diese Implementation verwenden

Fall 2 kann nicht durch Last-Write-Wins gelöst werden. Es gibt drei Optionen:
a) Die Entity wiederherstellen
b) Die eigenen Änderungen verwerfen
c) Die eigenen Optionen bearbeiten

Option a) ist die technisch aufwändigste Option.
Option b) Ist technisch am einfachsten, kann aber für den User unerwartetes oder frustrierendes Verhalten sein.
Option c) Ist technisch auch aufwändig (UI), bietet aber die beste UX

==> Den User zwischen Option b) und c) wählen lassen und LOKAL die entsprechenden Commands bearbeiten (NUR ERLAUBT BEVOR SYNC)

# Test Cases

Wenn nicht anders angegeben, verwende immer alle erlaubten Felder mit gültigen Werte für die Tests.
Ohne Angabe wird als container bzw parent immer der root container vom system verwendet.

## Item Tests
Bei allen Tests muss sowohl die Operation selbst erfolgreich sein, als auch der Berechnete Snapshot mit der Erwartung übereinstimmen
1. ITEM_CREATE → succeeds
2. ITEM_CREATE → ITEM_DELETE → succeeds
3. ITEM_CREATE → ITEM_UPDATE → succeeds
4. ITEM_CREATE → ITEM_UPDATE → ITEM_DELETE → succeeds
5. ITEM_CREATE → ITEM_UPDATE (name) → ITEM_UPDATE (name) → succeeds
6. ITEM_CREATE → ITEM_UPDATE (name) → ITEM_UPDATE (description) → succeeds
7. ITEM_CREATE ohne ID → fails
8. ITEM_CREATE ohne name → fails
9. ITEM_CREATE mit blank name → fails
10. ITEM_CREATE mit name mit 2 chars → fails
11. ITEM_CREATE ohne container → fails
12. ITEM_CREATE mit nicht existierenden container, aber valid UUID → fails
13. ITEM_CREATE mit id, aber kein valid UUID → fails
14. ITEM_CREATE mit container, aber kein valid UUID → fails
15. ITEM_CREATE mit quantity = 0 → fails
16. ITEM_CREATE mit created_at = 3000-01-01T00:00:00 → fails
17. ITEM_CREATE mit created_at = 2000-01-01T00:00:00 → succeeds
18. ITEM_UPDATE ohne ID → fails
19. ITEM_UPDATE ohne name → succeeds
20. ITEM_UPDATE mit blank name → fails
21. ITEM_UPDATE mit name mit 2 chars → fails
22. ITEM_UPDATE ohne container → fails
23. ITEM_UPDATE mit nicht existierenden container, aber valid UUID → fails
24. ITEM_UPDATE mit id, aber kein valid UUID → fails
25. ITEM_UPDATE mit container, aber kein valid UUID → fails
26. ITEM_UPDATE mit quantity = 0 → fails
27. ITEM_UPDATE mit created_at = 3000-01-01T00:00:00 → fails
28. ITEM_UPDATE mit created_at = 2000-01-01T00:00:00 → fails
29. ITEM_UPDATE mit created_at = '' → fails
30. ITEM_DELETE ohne id → fails
31. ITEM_DELETE mit ungültiger id, aber valid UUID → fails
32. ITEM_CREATE mit nur pflichtfeldern → succeeds

## Container Tests
Bei allen Tests muss sowohl die Operation selbst erfolgreich sein, als auch der Berechnete Snapshot mit der Erwartung übereinstimmen
1. CONTAINER_CREATE → succeeds
2. CONTAINER_CREATE → CONTAINER_DELETE → succeeds
3. CONTAINER_CREATE → CONTAINER_UPDATE → succeeds
4. CONTAINER_CREATE → CONTAINER_UPDATE → CONTAINER_DELETE → succeeds
5. CONTAINER_CREATE → CONTAINER_UPDATE (name) → CONTAINER_UPDATE (name) → succeeds
6. CONTAINER_CREATE → CONTAINER_UPDATE (name) → CONTAINER_UPDATE (description) → succeeds
7. CONTAINER_CREATE ohne ID → fails
8. CONTAINER_CREATE ohne name → fails
9. CONTAINER_CREATE mit blank name → fails
10. CONTAINER_CREATE mit name mit 2 chars → fails
11. CONTAINER_CREATE ohne parent → fails
12. CONTAINER_CREATE mit nicht existierenden parent, aber valid UUID → fails
13. CONTAINER_CREATE mit id, aber kein valid UUID → fails
14. CONTAINER_CREATE mit parent, aber kein valid UUID → fails
15. CONTAINER_CREATE mit created_at = 3000-01-01T00:00:00 → fails
16. CONTAINER_CREATE mit created_at = 2000-01-01T00:00:00 → succeeds
17. CONTAINER_UPDATE ohne ID → fails
18. CONTAINER_UPDATE ohne name → succeeds
19. CONTAINER_UPDATE mit blank name → fails
20. CONTAINER_UPDATE mit name mit 2 chars → fails
21. CONTAINER_UPDATE ohne parent → fails
22. CONTAINER_UPDATE mit nicht existierenden parent, aber valid UUID → fails
23. CONTAINER_UPDATE mit id, aber kein valid UUID → fails
24. CONTAINER_UPDATE mit parent, aber kein valid UUID → fails
25. CONTAINER_UPDATE mit created_at = 3000-01-01T00:00:00 → fails
26. CONTAINER_UPDATE mit created_at = 2000-01-01T00:00:00 → fails
27. CONTAINER_UPDATE mit created_at = '' → fails
28. CONTAINER_DELETE ohne id → fails
29. CONTAINER_DELETE mit ungültiger id, aber valid UUID → fails
30. CONTAINER_CREATE mit nur pflichtfeldern → succeeds

## Category Tests
Bei allen Tests muss sowohl die Operation selbst erfolgreich sein, als auch der Berechnete Snapshot mit der Erwartung übereinstimmen
1. CATEGORY_CREATE → succeeds
2. CATEGORY_CREATE → CATEGORY_DELETE → succeeds
3. CATEGORY_CREATE → CATEGORY_UPDATE → succeeds
4. CATEGORY_CREATE → CATEGORY_UPDATE → CATEGORY_DELETE → succeeds
5. CATEGORY_CREATE → CATEGORY_UPDATE (name) → CATEGORY_UPDATE (name) → succeeds
6. CATEGORY_CREATE → CATEGORY_UPDATE (name) → CATEGORY_UPDATE (description) → succeeds
7. CATEGORY_CREATE ohne ID → fails
8. CATEGORY_CREATE ohne name → fails
9. CATEGORY_CREATE mit blank name → fails
10. CATEGORY_CREATE mit name mit 2 chars → fails
11. CATEGORY_CREATE ohne shortcode → fails
12. CATEGORY_CREATE mit blank shortcode → fails
13. CATEGORY_CREATE mit shortcode mit 2 chars → succeeds
14. CATEGORY_CREATE mit shortcode mit 5 chars → fails
15. CATEGORY_CREATE mit id, aber kein valid UUID → fails
16. CATEGORY_CREATE mit created_at = 3000-01-01T00:00:00 → fails
17. CATEGORY_CREATE mit created_at = 2000-01-01T00:00:00 → succeeds
18. CATEGORY_UPDATE ohne ID → fails
19. CATEGORY_UPDATE ohne name → succeeds
20. CATEGORY_UPDATE mit blank name → fails
21. CATEGORY_UPDATE mit name mit 2 chars → fails
22. CATEGORY_UPDATE ohne shortcode → fails
23. CATEGORY_UPDATE mit blank shortcode → fails
24. CATEGORY_UPDATE mit shortcode mit 2 chars → succeeds
25. CATEGORY_UPDATE mit shortcode mit 5 chars → fails
26. CATEGORY_UPDATE mit id, aber kein valid UUID → fails
27. CATEGORY_UPDATE mit created_at = 3000-01-01T00:00:00 → fails
28. CATEGORY_UPDATE mit created_at = 2000-01-01T00:00:00 → fails
29. CATEGORY_UPDATE mit created_at = '' → fails
30. CATEGORY_DELETE ohne id → fails
31. CATEGORY_DELETE mit ungültiger id, aber valid UUID → fails
32. CATEGORY_CREATE mit nur pflichtfeldern → succeeds

## Container Image Tests
Bei allen Tests muss sowohl die Operation selbst erfolgreich sein, als auch der Berechnete Snapshot mit der Erwartung übereinstimmen
1. CONTAINER_IMAGE_CREATE → succeeds
2. CONTAINER_IMAGE_CREATE → CONTAINER_IMAGE_DELETE → succeeds
3. CONTAINER_IMAGE_CREATE ohne ID → fails
4. CONTAINER_IMAGE_CREATE ohne container → fails
5. CONTAINER_IMAGE_CREATE mit nicht existierenden container, aber valid UUID → fails
6. CONTAINER_IMAGE_CREATE mit id, aber kein valid UUID → fails
7. CONTAINER_IMAGE_CREATE mit container, aber kein valid UUID → fails
8. CONTAINER_IMAGE_CREATE mit created_at = 3000-01-01T00:00:00 → fails
9. CONTAINER_IMAGE_CREATE mit created_at = 2000-01-01T00:00:00 → succeeds
10. CONTAINER_IMAGE_DELETE ohne id → fails
11. CONTAINER_IMAGE_DELETE mit ungültiger id, aber valid UUID → fails

## Item Image Tests
Bei allen Tests muss sowohl die Operation selbst erfolgreich sein, als auch der Berechnete Snapshot mit der Erwartung übereinstimmen
1. ITEM_IMAGE_CREATE → succeeds
2. ITEM_IMAGE_CREATE → ITEM_IMAGE_DELETE → succeeds
3. ITEM_IMAGE_CREATE ohne ID → fails
4. ITEM_IMAGE_CREATE ohne item → fails
5. ITEM_IMAGE_CREATE mit nicht existierenden item, aber valid UUID → fails
6. ITEM_IMAGE_CREATE mit id, aber kein valid UUID → fails
7. ITEM_IMAGE_CREATE mit item, aber kein valid UUID → fails
8. ITEM_IMAGE_CREATE mit created_at = 3000-01-01T00:00:00 → fails
9. ITEM_IMAGE_CREATE mit created_at = 2000-01-01T00:00:00 → succeeds
10. ITEM_IMAGE_DELETE ohne id → fails
11. ITEM_IMAGE_DELETE mit ungültiger id, aber valid UUID → fails

## Mixed Tests
1. CATEGORY_CREATE → ITEM_CREATE (category) → CATEGORY_DELETE → fails
2. CATEGORY_CREATE → ITEM_CREATE (category) → CATEGORY_UPDATE → succeeds
3. CATEGORY_CREATE → ITEM_CREATE → ITEM_UPDATE (category) → CATEGORY_DELETE → fails
4. CATEGORY_CREATE → ITEM_CREATE (category) → succeeds
5. CATEGORY_CREATE → ITEM_CREATE → ITEM_UPDATE (category) → succeeds
6. CATEGORY_CREATE → CONTAINER_CREATE (category) → CATEGORY_DELETE → fails
7. CATEGORY_CREATE → CONTAINER_CREATE (category) → CATEGORY_UPDATE → succeeds
8. CATEGORY_CREATE → CONTAINER_CREATE → CONTAINER_UPDATE (category) → CATEGORY_DELETE → fails
9. CATEGORY_CREATE → CONTAINER_CREATE (category) → succeeds
10. CATEGORY_CREATE → CONTAINER_CREATE → CONTAINER_UPDATE (category) → succeeds
11. CONTAINER_CREATE → ITEM_CREATE (container) → CONTAINER_DELETE → fails
12. CONTAINER_CREATE → ITEM_CREATE → ITEM_UPDATE (container) → CONTAINER_DELETE → fails
13. CONTAINER_CREATE → ITEM_CREATE (container) → succeeds
14. CONTAINER_CREATE → ITEM_CREATE → ITEM_UPDATE (container) → succeeds
15. CONTAINER_CREATE → CONTAINER_CREATE (parent) → CONTAINER_DELETE (parent) → fails
16. CONTAINER_CREATE → CONTAINER_CREATE → CONTAINER_UPDATE (parent) → CONTAINER_DELETE (parent) → fails
17. CONTAINER_CREATE → CONTAINER_CREATE (parent) → succeeds
18. CONTAINER_CREATE → CONTAINER_CREATE → CONTAINER_UPDATE (parent) → succeeds
19. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → ITEM_DELETE → fails
20. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → ITEM_IMAGE_DELETE → succeeds
21. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → ITEM_UPDATE (primary_image) → ITEM_DELETE → fails
22. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → ITEM_UPDATE (primary_image) → ITEM_IMAGE_DELETE → fails
23. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → ITEM_UPDATE (primary_image) → ITEM_UPDATE (primary_image = null) → ITEM_IMAGE_DELETE → succeeds
24. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → ITEM_UPDATE (primary_image) → ITEM_UPDATE (primary_image = null) → ITEM_IMAGE_DELETE → ITEM_DELETE → succeeds
25. ITEM_CREATE → ITEM_IMAGE_CREATE (item) → succeeds
26. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → CONTAINER_DELETE → fails
27. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → CONTAINER_IMAGE_DELETE → succeeds
28. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → CONTAINER_UPDATE (primary_image) → CONTAINER_DELETE → fails
29. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → CONTAINER_UPDATE (primary_image) → CONTAINER_IMAGE_DELETE → fails
30. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → CONTAINER_UPDATE (primary_image) → CONTAINER_UPDATE (primary_image = null) → CONTAINER_IMAGE_DELETE → succeeds
31. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → CONTAINER_UPDATE (primary_image) → CONTAINER_UPDATE (primary_image = null) → CONTAINER_IMAGE_DELETE → CONTAINER_DELETE → succeeds
32. CONTAINER_CREATE → CONTAINER_IMAGE_CREATE (container) → succeeds

## Command Tests
1. Alle command_type mit command_version=1 -> succeeds
2. Alle command_type mit command_version=0 -> fails
3. Alle command_type mit command_version=2 -> fails
4. Alle command_type ohne command_version -> fails
5. command_type = CONTAINER_IMAGE_DELETE -> fails
6. command_type = ITEM_IMAGE_DELETE -> fails
7. ohne id -> fails
8. mit id, aber keine gültige UUID -> fails

## Sync
1. applyCommands?head=null -> 400
2. applyCommands?head=21313123 -> 400
3. applyCommands -> 400
4. applyCommands?head=111111111111-1111-1111-1111-111111 <ohne payload> -> applyCommands?head=111111111111-1111-1111-1111-111111 -> 200
5. applyCommands?head=111111111111-1111-1111-1111-111111 <invalid command payload> -> 400
6. applyCommands?head=111111111111-1111-1111-1111-111111 <invalid command_type> -> 400
7. applyCommands?head=111111111111-1111-1111-1111-111111 <invalid command_version> -> 400
8. applyCommands?head=111111111111-1111-1111-1111-111111 <1 valid command payload, 1 invalid command payload> -> 400
9. applyCommands?head=111111111111-1111-1111-1111-111111 <1 (valid) command im payload> -> applyCommands?head=111111111111-1111-1111-1111-111111 -> 409
10. fetchCommands?since=null -> 400
11. fetchCommands?since=123123 -> 400
12. fetchCommands?since=999999999999-9999-9999-9999-999999999999 -> 404
13. fetchCommands?since=111111111111-1111-1111-1111-111111111111 -> 200
14. fetchCommands -> 200
15. Multi-Step
a. head = fetchCommands.last.id
b. applyCommands?head=head <1 valid command in payload>
c. fetchCommands?since=111111111111-1111-1111-1111-111111111111 -> nach head suchen -> child = new command id?