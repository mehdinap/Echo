require('dotenv').config();

module.exports = {
  port: process.env.PORT || 4000,
  jwtSecret: process.env.JWT_SECRET || 'dev_secret_change_me',
  dbFile: process.env.DB_FILE || './LumaBeat.db',
  tokenTtl: '30d',
};
