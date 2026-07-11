const bcrypt = require('bcryptjs');
const db = require('../storage');
const { loadCatalogue } = require('./catalogue.provider');

const now = Date.now();

function clearCatalog() {
  const tables = ['messages', 'conversations', 'artist_follows', 'follows', 'recently_played',
    'likes', 'playlist_songs', 'playlists', 'songs', 'artists', 'users'];
  tables.forEach((table) => db.prepare(`DELETE FROM ${table}`).run());
  db.prepare('DELETE FROM sqlite_sequence').run();
}

function insertUsers(catalogue) {
  const insert = db.prepare(`
    INSERT INTO users (username, display_name, password_hash, avatar_url, is_premium, created_at)
    VALUES (?, ?, ?, ?, ?, ?)
  `);
  const passwordHash = bcrypt.hashSync('2468', 8);
  catalogue.seedAccounts.forEach((user) => insert.run(
    user.username,
    user.displayName,
    passwordHash,
    null,
    user.isPremium ? 1 : 0,
    now
  ));
}

function insertArtists(catalogue) {
  const insert = db.prepare('INSERT INTO artists (name, image_url, followers) VALUES (?, ?, ?)');
  const artistIds = {};
  catalogue.artists.forEach((name, index) => {
    const image = catalogue.songs.find((song) => song.artistName === name)?.artistImage || null;
    artistIds[name] = insert.run(name, image, 1000 + index * 137).lastInsertRowid;
  });
  return artistIds;
}

function insertSongs(catalogue, artistIds) {
  const insert = db.prepare(`
    INSERT INTO songs (title, artist_id, artist_name, cover_image_url, audio_url,
                       duration_ms, genre, is_local, plays, released_at)
    VALUES (@title, @artistId, @artistName, @cover, @audio, @duration,
            @genre, @isLocal, @plays, @released)
  `);
  const songs = catalogue.songs.map((song) => ({
    title: song.title,
    artistId: artistIds[song.artistKey],
    artistName: song.artistName,
    cover: song.cover,
    audio: song.audio,
    duration: song.duration,
    genre: song.genre,
    isLocal: song.isLocal,
    plays: song.plays,
    released: song.released,
  }));
  db.transaction((rows) => rows.forEach((song) => insert.run(song)))(songs);
  return songs.length;
}

function insertPlaylists(catalogue) {
  const insertPlaylist = db.prepare(`
    INSERT INTO playlists (title, cover_url, owner_id, is_public, kind, created_at)
    VALUES (?, ?, ?, ?, ?, ?)
  `);
  const insertSong = db.prepare(
    'INSERT OR IGNORE INTO playlist_songs (playlist_id, song_id, position) VALUES (?, ?, ?)'
  );
  const worldSongIds = db.prepare('SELECT id FROM songs WHERE is_local = 0').all().map(({ id }) => id);
  const localSongIds = db.prepare('SELECT id FROM songs WHERE is_local = 1').all().map(({ id }) => id);
  const addPlaylist = (title, kind, ownerId, songIds) => {
    const cover = songIds.length
      ? db.prepare('SELECT cover_image_url FROM songs WHERE id = ?').get(songIds[0]).cover_image_url
      : null;
    const id = insertPlaylist.run(title, cover, ownerId, 1, kind, now).lastInsertRowid;
    songIds.forEach((songId, position) => insertSong.run(id, songId, position));
  };

  catalogue.playlists.world.forEach((title, index) => addPlaylist(
    title,
    'world',
    null,
    worldSongIds.slice(index * 4, index * 4 + 8)
  ));
  catalogue.playlists.local.forEach((title, index) => addPlaylist(
    title,
    'local',
    null,
    localSongIds.slice(index * 4, index * 4 + 8)
  ));
  catalogue.playlists.mine.forEach((title, index) => addPlaylist(
    title,
    'user',
    1,
    [...worldSongIds, ...localSongIds].slice(index * 6, index * 6 + 6)
  ));
}

function insertSocialGraph() {
  const follow = db.prepare('INSERT OR IGNORE INTO follows (follower_id, followee_id) VALUES (?, ?)');
  [[1, 2], [1, 3], [2, 1], [3, 1], [4, 1]].forEach(([from, to]) => follow.run(from, to));
}

function insertEngagement(catalogue, artistIds) {
  const songIds = db.prepare('SELECT id FROM songs ORDER BY id').all().map(({ id }) => id);
  const artistIdList = Object.values(artistIds);

  const like = db.prepare('INSERT OR IGNORE INTO likes (user_id, song_id, liked_at) VALUES (?, ?, ?)');
  const recent = db.prepare(`
    INSERT INTO recently_played (user_id, song_id, played_at) VALUES (?, ?, ?)
    ON CONFLICT(user_id, song_id) DO UPDATE SET played_at = excluded.played_at
  `);
  const artistFollow = db.prepare(
    'INSERT OR IGNORE INTO artist_follows (user_id, artist_id) VALUES (?, ?)'
  );

  // Populate each seeded account with a distinct starting experience.
  catalogue.seedAccounts.forEach((_user, userIndex) => {
    for (let i = 0; i < 6; i += 1) {
      like.run(userIndex + 1, songIds[(userIndex * 7 + i * 3) % songIds.length], now - i * 3600000);
    }
    for (let i = 0; i < 5; i += 1) {
      recent.run(userIndex + 1, songIds[(userIndex * 5 + i) % songIds.length], now - i * 7200000);
    }
    for (let i = 0; i < 4; i += 1) {
      artistFollow.run(userIndex + 1, artistIdList[(userIndex * 3 + i) % artistIdList.length]);
    }
  });

  const conversation = db.prepare(
    'INSERT INTO conversations (user_a, user_b, created_at) VALUES (?, ?, ?)'
  );
  const message = db.prepare(`
    INSERT INTO messages (client_id, conversation_id, sender_id, body, song_id, status, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)
  `);
  const chats = [
    [1, 2, [['Hey Sara! Check out this song.', 1, 'READ'], ['I love it! Adding it to my playlist.', 2, 'READ'], ['The cover art is beautiful too.', null, 'DELIVERED']]],
    [1, 3, [['Sam, are you listening to the new local tracks?', 26, 'READ'], ['Yes, the Tehran playlist is great.', 27, 'READ'], ['Let me know your favorite one.', null, 'SENT']]],
    [2, 4, [['Roya shared a playlist with you.', 8, 'READ'], ['Thanks, I will listen tonight.', null, 'READ'], ['Enjoy!', null, 'SENT']]],
    [3, 5, [['Kian, this instrumental is perfect for studying.', 14, 'READ'], ['Sending it to my Focus playlist now.', null, 'SENT']]],
  ];

  chats.forEach(([userA, userB, messages], chatIndex) => {
    const conversationId = conversation.run(userA, userB, now - (chatIndex + 1) * 86400000).lastInsertRowid;
    messages.forEach(([body, songId, status], messageIndex) => {
      const senderId = messageIndex % 2 === 0 ? userA : userB;
      message.run(
        `seed-chat-${chatIndex + 1}-${messageIndex + 1}`,
        conversationId,
        senderId,
        body,
        songId,
        status,
        now - (messages.length - messageIndex) * 3600000
      );
    });
  });
}

async function importCatalogue() {
  const catalogue = await loadCatalogue();
  clearCatalog();
  insertUsers(catalogue);
  const artistIds = insertArtists(catalogue);
  const songCount = insertSongs(catalogue, artistIds);
  insertPlaylists(catalogue);
  insertSocialGraph();
  insertEngagement(catalogue, artistIds);
  console.log(`Imported ${songCount} songs, ${catalogue.artists.length} artists, and ${catalogue.seedAccounts.length} users.`);
  console.log('Imported likes, listening history, artist follows, conversations, and messages.');
  console.log('Demo access: mehdi / 2468. Other seeded accounts use the same password.');
}

importCatalogue().catch((error) => {
  console.error(`Catalogue import failed: ${error.message}`);
  process.exitCode = 1;
});
