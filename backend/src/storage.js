const Database = require('better-sqlite3');
const config = require('./runtime.config');

const db = new Database(config.dbFile);
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

db.exec(`
CREATE TABLE IF NOT EXISTS users (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  username      TEXT UNIQUE NOT NULL,
  display_name  TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  avatar_url    TEXT,
  is_premium    INTEGER NOT NULL DEFAULT 0,
  created_at    INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS artists (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT NOT NULL,
  image_url  TEXT,
  followers  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS songs (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  title           TEXT NOT NULL,
  artist_id       INTEGER NOT NULL REFERENCES artists(id),
  artist_name     TEXT NOT NULL,
  cover_image_url TEXT NOT NULL,
  audio_url       TEXT NOT NULL,
  duration_ms     INTEGER NOT NULL DEFAULT 0,
  genre           TEXT,
  is_local        INTEGER NOT NULL DEFAULT 0,   -- 0 = world, 1 = domestic
  plays           INTEGER NOT NULL DEFAULT 0,
  released_at     INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS playlists (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  title       TEXT NOT NULL,
  cover_url   TEXT,
  owner_id    INTEGER REFERENCES users(id) ON DELETE CASCADE,
  is_public   INTEGER NOT NULL DEFAULT 1,
  kind        TEXT NOT NULL DEFAULT 'user',     -- world | local | user
  created_at  INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist_songs (
  playlist_id INTEGER NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
  song_id     INTEGER NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  position    INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (playlist_id, song_id)
);

CREATE TABLE IF NOT EXISTS likes (
  user_id  INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  song_id  INTEGER NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  liked_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, song_id)
);

CREATE TABLE IF NOT EXISTS recently_played (
  user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  song_id   INTEGER NOT NULL REFERENCES songs(id) ON DELETE CASCADE,
  played_at INTEGER NOT NULL,
  PRIMARY KEY (user_id, song_id)
);

CREATE TABLE IF NOT EXISTS follows (
  follower_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  followee_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  PRIMARY KEY (follower_id, followee_id)
);

CREATE TABLE IF NOT EXISTS artist_follows (
  user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  artist_id INTEGER NOT NULL REFERENCES artists(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, artist_id)
);

CREATE TABLE IF NOT EXISTS conversations (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  user_a     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  user_b     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL,
  UNIQUE (user_a, user_b)
);

CREATE TABLE IF NOT EXISTS messages (
  id              INTEGER PRIMARY KEY AUTOINCREMENT,
  client_id       TEXT,                          -- for optimistic UI de-duplication
  conversation_id INTEGER NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body            TEXT,
  song_id         INTEGER REFERENCES songs(id),  -- shared song attachment
  status          TEXT NOT NULL DEFAULT 'SENT',  -- SENT | DELIVERED | READ
  created_at      INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id, id DESC);
CREATE INDEX IF NOT EXISTS idx_songs_title ON songs(title);
`);

module.exports = db;
