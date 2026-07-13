const router = require('express').Router();
const db = require('../storage');
const { requireAuth } = require('../session');
const { mapSong } = require('./songs.module');

const mapPlaylist = (p) => ({
  id: p.id,
  title: p.title,
  coverUrl: p.cover_url,
  ownerId: p.owner_id,
  ownerName: p.owner_name ?? null,
  isPublic: !!p.is_public,
  kind: p.kind,
  songCount: p.song_count ?? 0,
});

const WITH_COUNT = `
  SELECT p.*, u.display_name AS owner_name,
         (SELECT COUNT(*) FROM playlist_songs ps WHERE ps.playlist_id = p.id) AS song_count
  FROM playlists p LEFT JOIN users u ON u.id = p.owner_id
`;

router.get('/playlists', (req, res) => {
  const kind = req.query.kind;
  const rows = kind
    ? db.prepare(`${WITH_COUNT} WHERE p.kind = ? AND p.is_public = 1`).all(kind)
    : db.prepare(`${WITH_COUNT} WHERE p.is_public = 1`).all();
  res.json(rows.map(mapPlaylist));
});

router.get('/me/playlists', requireAuth, (req, res) => {
  const rows = db.prepare(`${WITH_COUNT} WHERE p.owner_id = ?`).all(req.user.id);
  res.json(rows.map(mapPlaylist));
});

router.get('/playlists/:id', (req, res) => {
  const p = db.prepare(`${WITH_COUNT} WHERE p.id = ?`).get(req.params.id);
  if (!p) return res.status(404).json({ error: 'not_found' });
  const songs = db.prepare(
    `SELECT s.* FROM playlist_songs ps JOIN songs s ON s.id = ps.song_id
     WHERE ps.playlist_id = ? ORDER BY ps.position`
  ).all(req.params.id);
  res.json({ ...mapPlaylist(p), songs: songs.map(mapSong) });
});

router.post('/playlists', requireAuth, (req, res) => {
  const { title, coverUrl, isPublic = true } = req.body || {};
  if (!title) return res.status(400).json({ error: 'title_required' });
  const info = db.prepare(
    `INSERT INTO playlists (title, cover_url, owner_id, is_public, kind, created_at)
     VALUES (?,?,?,?,'user',?)`
  ).run(title, coverUrl ?? null, req.user.id, isPublic ? 1 : 0, Date.now());
  res.status(201).json(mapPlaylist(db.prepare(`${WITH_COUNT} WHERE p.id = ?`).get(info.lastInsertRowid)));
});

router.put('/playlists/:id/songs/:songId', requireAuth, (req, res) => {
  const pl = db.prepare('SELECT * FROM playlists WHERE id = ?').get(req.params.id);
  if (!pl || pl.owner_id !== req.user.id) return res.status(403).json({ error: 'forbidden' });
  const pos = db.prepare('SELECT COUNT(*) c FROM playlist_songs WHERE playlist_id = ?').get(pl.id).c;
  db.prepare('INSERT OR IGNORE INTO playlist_songs (playlist_id, song_id, position) VALUES (?,?,?)')
    .run(pl.id, req.params.songId, pos);
  res.status(204).end();
});

router.delete('/playlists/:id/songs/:songId', requireAuth, (req, res) => {
  const pl = db.prepare('SELECT * FROM playlists WHERE id = ?').get(req.params.id);
  if (!pl || pl.owner_id !== req.user.id) return res.status(403).json({ error: 'forbidden' });
  db.prepare('DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?')
    .run(pl.id, req.params.songId);
  res.status(204).end();
});

module.exports = router;
