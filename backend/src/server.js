const express = require('express');
const http = require('http');
const path = require('path');
const cors = require('cors');
const morgan = require('morgan');
const config = require('./runtime.config');
const { attachSockets } = require('./realtime/gateway');

const app = express();
app.use(cors());
app.use(express.json({ limit: '1mb' }));
app.use(morgan('dev'));
app.use('/media', express.static(path.join(__dirname, '../media')));

app.get('/health', (_req, res) => res.json({ ok: true, ts: Date.now() }));

app.use('/api/auth', require('./modules/auth.module'));
app.use('/api', require('./modules/songs.module'));
app.use('/api', require('./modules/playlists.module'));
app.use('/api', require('./modules/social.module'));
app.use('/api', require('./modules/chat.module'));

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'server_error' });
});

const server = http.createServer(app);
attachSockets(server);

server.listen(config.port, () =>
  console.log(`LumaBeat API listening on http://0.0.0.0:${config.port}`)
);
