package com.findex.core.tokenizer;

import com.findex.core.model.Token;
import java.util.*;

public class StandardTokenizer implements Tokenizer {
    public static final Set<String> DEFAULT_STOPWORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
        "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
        "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
        "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
        "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
        "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once",
        "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own", "same", "she",
        "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them",
        "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
        "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves"
    )));

    private final boolean removeStopwords;
    private final Set<String> stopwords;

    public StandardTokenizer() {
        this(false);
    }

    public StandardTokenizer(boolean removeStopwords) {
        this.removeStopwords = removeStopwords;
        this.stopwords = DEFAULT_STOPWORDS;
    }

    public StandardTokenizer(boolean removeStopwords, Set<String> customStopwords) {
        this.removeStopwords = removeStopwords;
        this.stopwords = customStopwords != null ? customStopwords : DEFAULT_STOPWORDS;
    }

    @Override
    public List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        int length = text.length();
        int tokenPosition = 0;
        int i = 0;

        while (i < length) {

            while (i < length && !isTokenChar(text.charAt(i))) {
                i++;
            }

            if (i >= length) break;

            int startOffset = i;
            while (i < length && isTokenChar(text.charAt(i))) {
                i++;
            }
            int endOffset = i;

            String raw = text.substring(startOffset, endOffset).toLowerCase(Locale.ROOT);

            if (removeStopwords && stopwords.contains(raw)) {

                tokenPosition++;
                continue;
            }

            tokens.add(new Token(raw, tokenPosition++, startOffset, endOffset));
        }

        return tokens;
    }

    @Override
    public List<String> normalizeQueryTerms(String query) {
        List<String> terms = new ArrayList<>();
        if (query == null || query.isBlank()) {
            return terms;
        }

        List<Token> tokens = tokenize(query);
        for (Token t : tokens) {
            terms.add(t.getText());
        }
        return terms;
    }

    private boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
