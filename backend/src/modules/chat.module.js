const router = require('express').Router();
const db = require('../storage');
const { requireAuth } = require('../session');
const { publicUser } = require('./auth.module');
const { mapSong } = require('./songs.module');
const { getOrCreateConversation, mapMessage } = require('../realtime/chat.repository');
const { readPage, pageResult } = require('../common/paging');

router.get('/conversations', requireAuth, (req, res) => {
  const rows = db.prepare(
    `SELECT c.id,
            CASE WHEN c.user_a = ? THEN c.user_b ELSE c.user_a END AS peer_id
     FROM conversations c WHERE c.user_a = ? OR c.user_b = ?`
  ).all(req.user.id, req.user.id, req.user.id);

  const result = rows.map((c) => {
    const peer = db.prepare('SELECT * FROM users WHERE id = ?').get(c.peer_id);
    const last = db.prepare(
      'SELECT * FROM messages WHERE conversation_id = ? ORDER BY id DESC LIMIT 1'
    ).get(c.id);
    const unread = db.prepare(
      `SELECT COUNT(*) c FROM messages
       WHERE conversation_id = ? AND sender_id != ? AND status != 'READ'`
    ).get(c.id, req.user.id).c;
    return {
      id: c.id,
      peer: publicUser(peer, req),
      lastMessage: last ? mapMessage(last) : null,
      unreadCount: unread,
    };
  });
  result.sort((a, b) => (b.lastMessage?.createdAt ?? 0) - (a.lastMessage?.createdAt ?? 0));
  res.json(result);
});

/** Opens (or creates) a 1:1 conversation with `peerId`. */
router.post('/conversations/:peerId', requireAuth, (req, res) => {
  const id = getOrCreateConversation(req.user.id, Number(req.params.peerId));
  res.json({ id });
});

/** Paged message history, newest first -- feeds Paging 3 on the client. */
router.get('/conversations/:id/messages', requireAuth, (req, res) => {
  const { page, size, offset } = readPage(req.query, 30, 100);
  const rows = db.prepare(
    'SELECT * FROM messages WHERE conversation_id = ? ORDER BY id DESC LIMIT ? OFFSET ?'
  ).all(req.params.id, size, offset);

  const items = rows.map((m) => {
    const msg = mapMessage(m);
    if (m.song_id) {
      const s = db.prepare('SELECT * FROM songs WHERE id = ?').get(m.song_id);
      msg.song = s ? mapSong(s) : null;
    }
    return msg;
  });
  res.json(pageResult(items, page, size, rows.length));
});

module.exports = router;
