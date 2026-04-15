exports.calculateCredits = (tokensUsed) => {
  return Math.ceil(tokensUsed / 500);
};
