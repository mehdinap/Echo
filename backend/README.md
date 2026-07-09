# LumaBeat API

Node.js API and realtime gateway for the LumaBeat music and social client.

## Commands

```bash
npm install
npm run seed
npm start
```

The service listens on port `4000` by default. Set `PORT`, `JWT_SECRET`, and
`DB_FILE` in `.env` for local configuration.

## Source layout

- `src/modules/` contains HTTP route modules.
- `src/catalog/` contains remote catalogue loading and database imports.
- `src/realtime/` contains the Socket.IO gateway and chat persistence.
- `src/common/` contains shared request helpers.
- `src/storage.js` owns the SQLite connection and schema.
- `src/session.js` owns JWT signing and authentication middleware.
