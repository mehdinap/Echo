const db = require('../storage');

const mapMessage = (m) => ({
  id: m.id,
  clientId: m.client_id,
  conversationId: m.conversation_id,
  senderId: m.sender_id,
  body: m.body,
  songId: m.song_id,
  status: m.status,
  createdAt: m.created_at,
});

/** Conversations are keyed on the ordered pair so (a,b) and (b,a) are the same row. */
function getOrCreateConversation(u1, u2) {
  const [a, b] = u1 < u2 ? [u1, u2] : [u2, u1];
  const existing = db.prepare('SELECT id FROM conversations WHERE user_a = ? AND user_b = ?').get(a, b);
  if (existing) return existing.id;
  return db.prepare('INSERT INTO conversations (user_a, user_b, created_at) VALUES (?,?,?)')
           .run(a, b, Date.now()).lastInsertRowid;
}

function peerOf(conversationId, userId) {
  const c = db.prepare('SELECT * FROM conversations WHERE id = ?').get(conversationId);
  if (!c) return null;
  return c.user_a === userId ? c.user_b : c.user_a;
}

function insertMessage({ conversationId, senderId, body, songId, clientId }) {
  const info = db.prepare(
    `INSERT INTO messages (client_id, conversation_id, sender_id, body, song_id, status, created_at)
     VALUES (?,?,?,?,?, 'SENT', ?)`
  ).run(clientId ?? null, conversationId, senderId, body ?? null, songId ?? null, Date.now());
  return db.prepare('SELECT * FROM messages WHERE id = ?').get(info.lastInsertRowid);
}

function markDelivered(conversationId, receiverId) {
  db.prepare(
    `UPDATE messages SET status = 'DELIVERED'
     WHERE conversation_id = ? AND sender_id != ? AND status = 'SENT'`
  ).run(conversationId, receiverId);
}

function markRead(conversationId, receiverId) {
  db.prepare(
    `UPDATE messages SET status = 'READ'
     WHERE conversation_id = ? AND sender_id != ? AND status != 'READ'`
  ).run(conversationId, receiverId);
}

module.exports = { mapMessage, getOrCreateConversation, peerOf, insertMessage, markDelivered, markRead };
