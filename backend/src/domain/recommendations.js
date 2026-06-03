function toTagSet(tags) {
  return new Set((tags || []).map((tag) => String(tag).toLowerCase()));
}

function scoreByTags(preferences, candidateTags) {
  const preferred = toTagSet(preferences);
  const candidate = toTagSet(candidateTags);
  let score = 0;

  for (const tag of preferred) {
    if (candidate.has(tag)) {
      score += 10;
    }
  }

  return score;
}

function rankRecommendations({ preferences = [], cats = [], tables = [], menuItems = [] }) {
  const rankedCats = cats
    .map((cat) => ({
      ...cat,
      score: scoreByTags(preferences, cat.personality_tags) + (cat.health_status === "healthy" ? 2 : 0),
    }))
    .sort((a, b) => b.score - a.score || a.id - b.id);

  const rankedTables = tables
    .map((table) => ({
      ...table,
      score:
        scoreByTags(preferences, [table.area, table.cat_zone ? "cat-zone" : "standard"]) +
        (table.cat_zone ? 3 : 0),
    }))
    .sort((a, b) => b.score - a.score || a.seats - b.seats);

  const rankedMenuItems = menuItems
    .map((item) => ({
      ...item,
      score: scoreByTags(preferences, item.tags),
    }))
    .sort((a, b) => b.score - a.score || a.price_cents - b.price_cents);

  return {
    cat: rankedCats[0] || null,
    tables: rankedTables.slice(0, 3),
    menuItems: rankedMenuItems.slice(0, 4),
  };
}

module.exports = {
  rankRecommendations,
  scoreByTags,
};
