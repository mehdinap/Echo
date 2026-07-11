const jwt = require('jsonwebtoken');
const config = require('./runtime.config');

function sign(user) {
  return jwt.sign({ id: user.id, username: user.username }, config.jwtSecret, {
    expiresIn: config.tokenTtl,
  });
}

function verify(token) {
  return jwt.verify(token, config.jwtSecret);
}

/** Express middleware: rejects the request unless a valid Bearer token is present. */
function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'missing_token' });
  try {
    req.user = verify(token);
    next();
  } catch {
    res.status(401).json({ error: 'invalid_token' });
  }
}

module.exports = { sign, verify, requireAuth };
