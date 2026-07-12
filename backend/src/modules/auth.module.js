const router = require('express').Router();
const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');
const db = require('../storage');
const { sign, requireAuth } = require('../session');

const avatarRoot = path.join(__dirname, '../../media/avatars');

const defaultAvatarUrl = (req) =>
  `${req.protocol}://${req.get('host')}/media/avatars/default-avatar.svg`;

const publicUser = (u, req) => ({
  id: u.id,
  username: u.username,
  displayName: u.display_name,
  avatarUrl: !u.avatar_url || u.avatar_url.includes('pravatar.cc')
    ? defaultAvatarUrl(req)
    : u.avatar_url,
  isPremium: !!u.is_premium,
});

router.post('/register', (req, res) => {
  const { username, password, displayName } = req.body || {};
  if (!username || !password) return res.status(400).json({ error: 'username_and_password_required' });

  const exists = db.prepare('SELECT 1 FROM users WHERE username = ?').get(username);
  if (exists) return res.status(409).json({ error: 'username_taken' });

  const info = db.prepare(
    `INSERT INTO users (username, display_name, password_hash, avatar_url, is_premium, created_at)
     VALUES (?,?,?,?,0,?)`
  ).run(username, displayName || username, bcrypt.hashSync(password, 8),
        defaultAvatarUrl(req), Date.now());

  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(info.lastInsertRowid);
  res.status(201).json({ token: sign(user), user: publicUser(user, req) });
});

router.post('/login', (req, res) => {
  const { username, password } = req.body || {};
  const user = db.prepare('SELECT * FROM users WHERE username = ?').get(username || '');
  if (!user || !bcrypt.compareSync(password || '', user.password_hash)) {
    return res.status(401).json({ error: 'bad_credentials' });
  }
  res.json({ token: sign(user), user: publicUser(user, req) });
});

router.get('/me', requireAuth, (req, res) => {
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
  res.json(publicUser(user, req));
});

/** Enables premium access for the current account. */
router.post('/me/premium', requireAuth, (req, res) => {
  db.prepare('UPDATE users SET is_premium = 1 WHERE id = ?').run(req.user.id);
  const user = db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id);
  res.json(publicUser(user, req));
});

router.patch('/me', requireAuth, (req, res) => {
  const { displayName, avatarUrl, avatarData, avatarMimeType } = req.body || {};
  let storedAvatarUrl = avatarUrl ?? null;

  if (avatarData) {
    const encoded = String(avatarData).replace(/^data:[^;]+;base64,/, '');
    if (encoded.length > 4_000_000) {
      return res.status(413).json({ error: 'avatar_too_large' });
    }

    fs.mkdirSync(avatarRoot, { recursive: true });
    const extension = String(avatarMimeType || '').includes('png') ? 'png' : 'jpg';
    // A unique URL makes clients invalidate the previous cached avatar immediately.
    const filename = `user-${req.user.id}-${Date.now()}.${extension}`;
    fs.writeFileSync(path.join(avatarRoot, filename), Buffer.from(encoded, 'base64'));
    storedAvatarUrl = `${req.protocol}://${req.get('host')}/media/avatars/${filename}`;
  }

  db.prepare(`UPDATE users SET display_name = COALESCE(?, display_name),
              avatar_url = COALESCE(?, avatar_url) WHERE id = ?`)
    .run(displayName ?? null, storedAvatarUrl, req.user.id);
  res.json(publicUser(db.prepare('SELECT * FROM users WHERE id = ?').get(req.user.id), req));
});

module.exports = router;
module.exports.publicUser = publicUser;
