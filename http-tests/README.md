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

- **Sync/** — `Initial empty database` (a precondition guard asserting the chain
  holds only the seeded ROOT command — run it first, against a fresh DB), head
  handling, and `fetch`/`apply` edge cases (400/404/409, including a
  parent-vs-head mismatch conflict).
- **Item/**, **Container/**, **Category/** — happy-path create/update/delete plus a
  couple of validation failures.
- **Images/** — `ITEM_IMAGE_CREATE` command → `uploadImage` (multipart, uses the
  bundled `sample.png`) → `downloadImage`. Run in order 1 → 2 → 3, after an
  `Item/ITEM_CREATE`.
- **Mixed/** — a multi-command chain applied atomically in one request, and a
  command-envelope validation failure.

Scenario names reference the numbered "Test Cases" sections in `../DOMAIN-RULES.md`.

## Adding scenarios

Copy the closest existing `.bru` file. The reusable bits are:
- `params:query` `head: {{head}}` + `url ...?head={{head}}`,
- the `pre-request` UUID generator (payloads reject reused ids),
- the `post-response` `bru.setVar('head', res.getBody())` on success.

Payload field names are **snake_case** exactly as in `DOMAIN-RULES.md`, and any field
outside a command_type's allowed set rejects the whole command.