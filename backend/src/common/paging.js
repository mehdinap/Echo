function readPage(query, defaultSize, maxSize) {
  const requestedPage = Number(query.page);
  const requestedSize = Number(query.size);

  const page = Number.isInteger(requestedPage) && requestedPage >= 0
    ? requestedPage
    : 0;
  const size = Number.isInteger(requestedSize) && requestedSize > 0
    ? Math.min(requestedSize, maxSize)
    : defaultSize;

  return { page, size, offset: page * size };
}

function pageResult(items, page, size, totalItems) {
  return {
    items,
    page,
    hasNext: totalItems === size,
  };
}

module.exports = { readPage, pageResult };
