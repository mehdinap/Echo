const router = require('express').Router();
const db = require('../storage');
const { requireAuth } = require('../session');
const { publicUser } = require('./auth.module');
const { readPage, pageResult } = require('../common/paging');

router.get('/users', requireAuth, (req, res) => {
  const q = `%${(req.query.q || '').trim()}%`;
  const { page, size, offset } = readPage(req.query, 20, 50);
  const rows = db.prepare(
    `SELECT u.*, EXISTS(SELECT 1 FROM follows f WHERE f.follower_id = ? AND f.followee_id = u.id) AS following
     FROM users u WHERE u.id != ? AND (u.username LIKE ? OR u.display_name LIKE ?)
     LIMIT ? OFFSET ?`
  ).all(req.user.id, req.user.id, q, q, size, offset);
  res.json({
    ...pageResult(
      rows.map((u) => ({ ...publicUser(u, req), isFollowing: !!u.following })),
      page,
      size,
      rows.length
    ),
  });
});

router.get('/users/:id', requireAuth, (req, res) => {
  const u = db.prepare('SELECT * FROM users WHERE id = ?').get(req.params.id);
  if (!u) return res.status(404).json({ error: 'not_found' });
  const following = db.prepare('SELECT 1 FROM follows WHERE follower_id = ? AND followee_id = ?')
    .get(req.user.id, u.id);
  const playlists = db.prepare(
    `SELECT id, title, cover_url FROM playlists WHERE owner_id = ? AND is_public = 1`
  ).all(u.id);
  res.json({
    ...publicUser(u, req),
    isFollowing: !!following,
    followers: db.prepare('SELECT COUNT(*) c FROM follows WHERE followee_id = ?').get(u.id).c,
    publicPlaylists: playlists.map((p) => ({ id: p.id, title: p.title, coverUrl: p.cover_url })),
  });
});

router.put('/users/:id/follow', requireAuth, (req, res) => {
  if (Number(req.params.id) === req.user.id) return res.status(400).json({ error: 'cannot_follow_self' });
  db.prepare('INSERT OR IGNORE INTO follows (follower_id, followee_id) VALUES (?,?)')
    .run(req.user.id, req.params.id);
  res.json({ following: true });
});

router.delete('/users/:id/follow', requireAuth, (req, res) => {
  db.prepare('DELETE FROM follows WHERE follower_id = ? AND followee_id = ?')
    .run(req.user.id, req.params.id);
  res.json({ following: false });
});

router.get('/me/following', requireAuth, (req, res) => {
  const rows = db.prepare(
    `SELECT u.* FROM follows f JOIN users u ON u.id = f.followee_id WHERE f.follower_id = ?`
  ).all(req.user.id);
  res.json(rows.map((u) => publicUser(u, req)));
});

// ---- artists -------------------------------------------------------------
router.get('/artists/top', (_req, res) => {
  const rows = db.prepare('SELECT * FROM artists ORDER BY followers DESC LIMIT 20').all();
  res.json(rows.map((a) => ({ id: a.id, name: a.name, imageUrl: a.image_url, followers: a.followers })));
});

module.exports = router;
