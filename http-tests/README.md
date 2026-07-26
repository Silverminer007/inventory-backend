# http-tests — Bruno collection

Manual/scriptable API scenarios for the command-sourced inventory backend, driving
the real `/api/v2/sync/*` and image endpoints.

## Open it

Install [Bruno](https://www.usebruno.com/) and open this `http-tests/` folder as a
collection, or run headless with the CLI:

```bash
npm install -g @usebruno/cli
cd http-tests
bru run --env Local
```

Select the **Local** environment (`environments/Local.bru`, `baseUrl = http://localhost:8080`).
Start the backend first: `./mvnw quarkus:dev`.

## How the chain works (important)

This backend is **command-sourced**: there is one global, append-only, doubly-linked
command chain. To mutate anything you `POST applyCommands?head=<current tip>` — the
request is rejected with **409** if `head` isn't the real tip.

So the workflow is:

1. Run **`Sync/Get Head`** once. It fetches the chain and stores the current tip into
   the runtime var `head`.
2. Run any `applyCommands` scenario. Each one:
   - generates fresh UUIDs in a `pre-request` script (entities must be unique), and
   - on `200`, advances `head` to the new tip in a `post-response` script,

   so you can run several apply scenarios back-to-back without re-fetching the head.

If you ever get a **409**, just re-run `Sync/Get Head` to resync.

Runtime vars set along the way and reused by later requests: `head`, `itemId`,
`containerId`, `categoryId`, `imageId`.

## Layout

Because the whole collection shares one global, append-only chain, a full
`bru run` is one linear scenario and folder order matters. The Bruno CLI walks
folders alphabetically, so folders are numbered to force a meaningful order:

- **01-Sync/** — `Initial empty database` (precondition guard: the chain holds only
  the seeded ROOT command — must run first, against a fresh DB), `Get Head`, and
  `fetch`/`apply` edge cases (400/404). None of these mutate, so the DB is still
  empty when they finish.
- **02-Container/**, **03-Category/** — happy-path creates that move the head past root.
- **04-Item/** — create → update → delete, plus validation failures. This folder
  deletes its own item, which is why Images creates a fresh one.
- **05-Images/** — self-contained: `ITEM_CREATE` → `ITEM_IMAGE_CREATE` → `uploadImage`
  (multipart, bundled `sample.png`) → `downloadImage`, run 1 → 2 → 3 → 4.
- **06-Mixed/** — a multi-command chain applied atomically, and a command-envelope
  validation failure.
- **07-Sync-Conflicts/** — the two 409 conflict cases (stale head, parent-vs-head
  mismatch). These need the chain to already be past root, so they run last.

Scenario names reference the numbered "Test Cases" sections in `../DOMAIN-RULES.md`.

## Adding scenarios

Copy the closest existing `.bru` file. The reusable bits are:
- `params:query` `head: {{head}}` + `url ...?head={{head}}`,
- the `pre-request` UUID generator (payloads reject reused ids),
- the `post-response` `bru.setVar('head', res.getBody())` on success.

Payload field names are **snake_case** exactly as in `DOMAIN-RULES.md`, and any field
outside a command_type's allowed set rejects the whole command.