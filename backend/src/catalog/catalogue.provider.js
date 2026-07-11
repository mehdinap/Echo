/**
 * Remote catalogue provider used by the development data importer.
 *
 * Track metadata and preview URLs are fetched from Apple's public search API
 * when `npm run seed` is executed.
 */

const seedAccounts = [
  { username: 'mehdi', displayName: 'مهدی نجیب پور', isPremium: true },
  { username: 'sara', displayName: 'سارا احمدی', isPremium: false },
  { username: 'sam', displayName: 'سید امیرحسین میراجمدی', isPremium: false },
  { username: 'roya', displayName: 'رویا کریمی', isPremium: true },
  { username: 'kian', displayName: 'کیان رضایی', isPremium: false },
];

const queries = [
  { term: 'pop music', local: false },
  { term: 'rock music', local: false },
  { term: 'electronic music', local: false },
  { term: 'Persian music', local: true },
  { term: 'Iranian music', local: true },
  { term: 'jazz music', local: false },
];

const playlists = {
  world: ['Live Pop Picks', 'Rock Essentials', 'Electronic Nights', 'Jazz Corner'],
  local: ['Persian Favorites', 'Iranian Evenings', 'Local Discoveries'],
  mine: ['My Live Favorites', 'For Running'],
};

async function search(query) {
  const url = new URL('https://itunes.apple.com/search');
  url.search = new URLSearchParams({
    term: query.term,
    country: 'US',
    media: 'music',
    entity: 'song',
    limit: '50',
  });
  const response = await fetch(url);
  if (!response.ok) throw new Error(`iTunes Search API returned ${response.status}`);
  return response.json();
}

function artwork(url) {
  return url.replace('100x100', '600x600');
}

async function loadCatalogue() {
  const results = (await Promise.all(queries.map(async (query) => {
    const response = await search(query);
    return response.results
      .filter((song) => song.trackId && song.trackName && song.artistName && song.previewUrl && song.artworkUrl100)
      .map((song) => ({ ...song, isLocal: query.local }));
  }))).flat();

  const unique = [...new Map(results.map((song) => [song.trackId, song])).values()].slice(0, 100);
  if (unique.length < 100) {
    throw new Error(`Only ${unique.length} playable songs were returned by iTunes; need 100.`);
  }

  const artistNames = [...new Set(unique.map((song) => song.artistName))];
  const songs = unique.map((song, index) => ({
    title: song.trackName,
    artistName: song.artistName,
    artistKey: song.artistName,
    artistImage: artwork(song.artworkUrl100),
    cover: artwork(song.artworkUrl100),
    audio: song.previewUrl,
    duration: song.trackTimeMillis || 180000,
    genre: song.primaryGenreName || 'Other',
    isLocal: song.isLocal ? 1 : 0,
    plays: 5000 - index * 17,
    released: Date.parse(song.releaseDate) || Date.now() - index * 86400000,
  }));

  return { seedAccounts, artists: artistNames, songs, playlists };
}

module.exports = { loadCatalogue };
