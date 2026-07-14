const { Server } = require('socket.io');
const db = require('../storage');
const { verify } = require('../session');
const store = require('./chat.repository');

/**
 * Real-time layer. One room per user id ("u:<id>") so we can push an event to a
 * person no matter which conversation screen they happen to have open.
 *
 * Client -> server:  message:send, message:read, typing
 * Server -> client:  message:new, message:status, typing, presence
 */
function attachSockets(httpServer) {
  const io = new Server(httpServer, { cors: { origin: '*' } });

  io.use((socket, next) => {
    const token = socket.handshake.auth?.token;
    if (!token) return next(new Error('unauthorized'));
    try {
      socket.user = verify(token);
      next();
    } catch {
      next(new Error('unauthorized'));
    }
  });

  io.on('connection', (socket) => {
    const me = socket.user.id;
    socket.join(`u:${me}`);
    socket.broadcast.emit('presence', { userId: me, online: true });

    socket.on('message:send', (payload, ack) => {
      try {
        const { peerId, body, songId, clientId } = payload || {};
        const conversationId = store.getOrCreateConversation(me, Number(peerId));
        const row = store.insertMessage({ conversationId, senderId: me, body, songId, clientId });
        const message = store.mapMessage(row);

        if (songId) {
          const s = db.prepare('SELECT * FROM songs WHERE id = ?').get(songId);
          if (s) {
            message.song = {
              id: s.id, title: s.title, artistName: s.artist_name,
              coverImageUrl: s.cover_image_url, audioUrl: s.audio_url, durationMs: s.duration_ms,
            };
          }
        }

        // Sender gets the server id back so the optimistic row can be reconciled.
        ack?.({ ok: true, message });

        const peerRoom = io.sockets.adapter.rooms.get(`u:${peerId}`);
        io.to(`u:${peerId}`).emit('message:new', message);

        if (peerRoom && peerRoom.size > 0) {
          store.markDelivered(conversationId, Number(peerId));
          io.to(`u:${me}`).emit('message:status', {
            conversationId, messageId: message.id, status: 'DELIVERED',
          });
        }
      } catch (e) {
        ack?.({ ok: false, error: e.message });
      }
    });

    socket.on('message:read', ({ conversationId }) => {
      store.markRead(conversationId, me);
      const peer = store.peerOf(conversationId, me);
      io.to(`u:${peer}`).emit('message:status', { conversationId, status: 'READ' });
    });

    socket.on('typing', ({ peerId, isTyping }) => {
      io.to(`u:${peerId}`).emit('typing', { userId: me, isTyping: !!isTyping });
    });

    socket.on('disconnect', () => {
      socket.broadcast.emit('presence', { userId: me, online: false });
    });
  });

  return io;
}

module.exports = { attachSockets };
