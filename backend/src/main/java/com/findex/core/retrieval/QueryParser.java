package com.findex.core.retrieval;

import com.findex.core.index.InvertedIndex;
import com.findex.core.model.Posting;
import com.findex.core.tokenizer.Tokenizer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryParser {
    public sealed interface Node permits TermNode, PhraseNode, AndNode, OrNode, NotNode, MatchAllNode {}

    public record TermNode(String value) implements Node {}
    public record PhraseNode(List<String> terms) implements Node {}
    public record AndNode(Node left, Node right) implements Node {}
    public record OrNode(Node left, Node right) implements Node {}
    public record NotNode(Node child) implements Node {}
    public record MatchAllNode() implements Node {}

    private final Tokenizer tokenizer;
    private static final Pattern PHRASE_REGEX = Pattern.compile("\"([^\"]+)\"");

    public QueryParser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Node parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new MatchAllNode();
        }

        List<String> rawTokens = extractTokensAndPhrases(rawQuery.trim());
        if (rawTokens.isEmpty()) {
            return new MatchAllNode();
        }

        return new Parser(rawTokens).parseOr();
    }

    private List<String> extractTokensAndPhrases(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = PHRASE_REGEX.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {

            if (matcher.start() > lastIndex) {
                String sub = input.substring(lastIndex, matcher.start()).trim();
                if (!sub.isEmpty()) {
                    tokens.addAll(Arrays.asList(sub.split("\\s+")));
                }
            }

            tokens.add("\"" + matcher.group(1).trim() + "\"");
            lastIndex = matcher.end();
        }

        if (lastIndex < input.length()) {
            String sub = input.substring(lastIndex).trim();
            if (!sub.isEmpty()) {
                tokens.addAll(Arrays.asList(sub.split("\\s+")));
            }
        }

        return tokens;
    }

    public Set<String> extractPositiveTerms(Node node) {
        Set<String> terms = new HashSet<>();
        collectTerms(node, terms, false);
        return terms;
    }

    private void collectTerms(Node node, Set<String> terms, boolean isNegated) {
        switch (node) {
            case TermNode t -> {
                if (!isNegated) terms.add(t.value());
            }
            case PhraseNode p -> {
                if (!isNegated) terms.addAll(p.terms());
            }
            case AndNode a -> {
                collectTerms(a.left(), terms, isNegated);
                collectTerms(a.right(), terms, isNegated);
            }
            case OrNode o -> {
                collectTerms(o.left(), terms, isNegated);
                collectTerms(o.right(), terms, isNegated);
            }
            case NotNode n -> collectTerms(n.child(), terms, !isNegated);
            case MatchAllNode m -> {}
        }
    }

    private class Parser {
        private final List<String> tokens;
        private int pos = 0;

        Parser(List<String> tokens) {
            this.tokens = tokens;
        }

        Node parseOr() {
            Node left = parseAnd();
            while (peek("OR") || peek("or") || peek("||")) {
                pos++;
                Node right = parseAnd();
                left = new OrNode(left, right);
            }
            return left;
        }

        Node parseAnd() {
            Node left = parseNot();
            while (pos < tokens.size() && !peek("OR") && !peek("or") && !peek("||")) {
                if (peek("AND") || peek("and") || peek("&&")) {
                    pos++;
                }
                if (pos >= tokens.size()) break;
                Node right = parseNot();
                left = new AndNode(left, right);
            }
            return left;
        }

        Node parseNot() {
            if (peek("NOT") || peek("not") || peek("!")) {
                pos++;
                return new NotNode(parseNot());
            }
            return primary();
        }

        Node primary() {
            if (pos >= tokens.size()) return new MatchAllNode();
            String tok = tokens.get(pos++);

            if ((tok.startsWith("-") || tok.startsWith("!")) && tok.length() > 1) {
                String sub = tok.substring(1);
                return new NotNode(createTermOrPhrase(sub));
            }

            return createTermOrPhrase(tok);
        }

        private Node createTermOrPhrase(String text) {
            if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 2) {
                String inner = text.substring(1, text.length() - 1);
                List<String> words = tokenizer.normalizeQueryTerms(inner);
                if (words.size() == 1) return new TermNode(words.get(0));
                return new PhraseNode(words);
            }

            List<String> normalized = tokenizer.normalizeQueryTerms(text);
            if (normalized.isEmpty()) return new MatchAllNode();
            if (normalized.size() == 1) return new TermNode(normalized.get(0));

            Node chain = new TermNode(normalized.get(0));
            for (int i = 1; i < normalized.size(); i++) {
                chain = new AndNode(chain, new TermNode(normalized.get(i)));
            }
            return chain;
        }

        boolean peek(String kw) {
            return pos < tokens.size() && tokens.get(pos).equalsIgnoreCase(kw);
        }
    }

    public Set<Integer> evaluateCandidates(Node node, InvertedIndex index) {
        return switch (node) {
            case TermNode t -> {
                List<Posting> p = index.getPostings(t.value());
                Set<Integer> docIds = new HashSet<>(p.size());
                for (Posting posting : p) docIds.add(posting.getDocId());
                yield docIds;
            }
            case PhraseNode p -> {
                if (p.terms().isEmpty()) yield Collections.emptySet();
                List<Posting> firstList = index.getPostings(p.terms().get(0));
                Set<Integer> candidates = new HashSet<>();
                for (Posting post : firstList) {
                    if (matchesPhrase(post.getDocId(), p.terms(), index)) {
                        candidates.add(post.getDocId());
                    }
                }
                yield candidates;
            }
            case AndNode a -> {
                Set<Integer> left = evaluateCandidates(a.left(), index);
                if (left.isEmpty()) yield Collections.emptySet();
                Set<Integer> right = evaluateCandidates(a.right(), index);
                Set<Integer> intersection = new HashSet<>(left);
                intersection.retainAll(right);
                yield intersection;
            }
            case OrNode o -> {
                Set<Integer> left = evaluateCandidates(o.left(), index);
                Set<Integer> right = evaluateCandidates(o.right(), index);
                Set<Integer> union = new HashSet<>(left);
                union.addAll(right);
                yield union;
            }
            case NotNode n -> {
                Set<Integer> excluded = evaluateCandidates(n.child(), index);
                Set<Integer> allDocs = new HashSet<>();
                for (var doc : index.getAllDocuments()) {
                    if (!excluded.contains(doc.getId())) {
                        allDocs.add(doc.getId());
                    }
                }
                yield allDocs;
            }
            case MatchAllNode m -> {
                Set<Integer> allDocs = new HashSet<>();
                for (var doc : index.getAllDocuments()) {
                    allDocs.add(doc.getId());
                }
                yield allDocs;
            }
        };
    }

    private boolean matchesPhrase(int docId, List<String> phraseTerms, InvertedIndex index) {
        if (phraseTerms.isEmpty()) return true;
        List<List<Integer>> termPositions = new ArrayList<>();
        for (String term : phraseTerms) {
            Posting post = index.getPosting(term, docId);
            if (post == null || post.getPositions().isEmpty()) return false;
            termPositions.add(post.getPositions());
        }

        List<Integer> firstPositions = termPositions.get(0);
        for (int pos : firstPositions) {
            boolean matched = true;
            for (int offset = 1; offset < termPositions.size(); offset++) {
                if (!termPositions.get(offset).contains(pos + offset)) {
                    matched = false;
                    break;
                }
            }
            if (matched) return true;
        }
        return false;
    }
}
