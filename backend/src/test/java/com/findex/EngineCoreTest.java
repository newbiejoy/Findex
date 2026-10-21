package com.findex;

import com.findex.core.index.InvertedIndex;
import com.findex.core.model.*;
import com.findex.core.ranking.BM25Scorer;
import com.findex.core.ranking.TfIdfScorer;
import com.findex.core.retrieval.SearchEngine;
import com.findex.core.snippet.SnippetGenerator;
import com.findex.core.tokenizer.StandardTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EngineCoreTest {

    private StandardTokenizer tokenizer;
    private InvertedIndex index;
    private SearchEngine searchEngine;
    private SnippetGenerator snippetGenerator;

    @BeforeEach
    void setUp() {
        tokenizer = new StandardTokenizer();
        index = new InvertedIndex();
        snippetGenerator = new SnippetGenerator();
        searchEngine = new SearchEngine(index, tokenizer, snippetGenerator);
    }

    @Test
    void testTokenizerBasic() {
        String input = "Hello, World! Java 21 is AMAZING.";
        List<Token> tokens = tokenizer.tokenize(input);

        assertNotNull(tokens);
        assertEquals(6, tokens.size());
        assertEquals("hello", tokens.get(0).getText());
        assertEquals(0, tokens.get(0).getPosition());
        assertEquals("world", tokens.get(1).getText());
        assertEquals("java", tokens.get(2).getText());
        assertEquals("21", tokens.get(3).getText());
        assertEquals("is", tokens.get(4).getText());
        assertEquals("amazing", tokens.get(5).getText());
    }

    @Test
    void testTokenizerWithStopwords() {
        StandardTokenizer stopwordTokenizer = new StandardTokenizer(true);
        String text = "the quick brown fox jumps over the lazy dog";
        List<Token> tokens = stopwordTokenizer.tokenize(text);

        assertFalse(tokens.stream().anyMatch(t -> t.getText().equals("the")));
        assertTrue(tokens.stream().anyMatch(t -> t.getText().equals("quick")));
        assertTrue(tokens.stream().anyMatch(t -> t.getText().equals("dog")));
    }

    @Test
    void testInvertedIndexPostings() {
        String doc1Text = "Information retrieval algorithms and data structures";
        List<Token> tokens1 = tokenizer.tokenize(doc1Text);
        IndexedDocument doc1 = new IndexedDocument(1, "f1", "/path/doc1.txt", "doc1.txt", "txt", 100, 1000, tokens1.size(), doc1Text);
        index.addDocument(doc1, tokens1);

        String doc2Text = "Data structures solve computational problems";
        List<Token> tokens2 = tokenizer.tokenize(doc2Text);
        IndexedDocument doc2 = new IndexedDocument(2, "f1", "/path/doc2.txt", "doc2.txt", "txt", 120, 1000, tokens2.size(), doc2Text);
        index.addDocument(doc2, tokens2);

        assertEquals(2, index.getTotalDocuments());
        assertEquals(2, index.getDocumentFrequency("data"));
        assertEquals(2, index.getDocumentFrequency("structures"));
        assertEquals(1, index.getDocumentFrequency("algorithms"));
        assertEquals(1, index.getDocumentFrequency("problems"));

        List<Posting> postings = index.getPostings("data");
        assertEquals(2, postings.size());
        assertEquals(1, postings.get(0).getDocId());
        assertEquals(2, postings.get(1).getDocId());
    }

    @Test
    void testBm25Ranking() {

        String doc1Text = "Algorithm design and algorithm analysis with efficient algorithm steps";
        List<Token> tokens1 = tokenizer.tokenize(doc1Text);
        IndexedDocument doc1 = new IndexedDocument(1, "f1", "/path/doc1.txt", "doc1.txt", "txt", 100, 1000, tokens1.size(), doc1Text);
        index.addDocument(doc1, tokens1);

        String doc2Text = "An introduction to computer systems and an algorithm basics";
        List<Token> tokens2 = tokenizer.tokenize(doc2Text);
        IndexedDocument doc2 = new IndexedDocument(2, "f1", "/path/doc2.txt", "doc2.txt", "txt", 100, 1000, tokens2.size(), doc2Text);
        index.addDocument(doc2, tokens2);

        SearchResponse response = searchEngine.search("algorithm", "bm25", null, null, 10);
        assertEquals(2, response.getTotalResults());
        assertEquals(1, response.getResults().get(0).getDocument().getId());
        assertTrue(response.getResults().get(0).getScore() > response.getResults().get(1).getScore());
    }

    @Test
    void testPhraseSearch() {
        String doc1Text = "A quick brown fox jumped high";
        List<Token> tokens1 = tokenizer.tokenize(doc1Text);
        IndexedDocument doc1 = new IndexedDocument(1, "f1", "/path/doc1.txt", "doc1.txt", "txt", 100, 1000, tokens1.size(), doc1Text);
        index.addDocument(doc1, tokens1);

        String doc2Text = "A brown quick fox was sleeping";
        List<Token> tokens2 = tokenizer.tokenize(doc2Text);
        IndexedDocument doc2 = new IndexedDocument(2, "f1", "/path/doc2.txt", "doc2.txt", "txt", 100, 1000, tokens2.size(), doc2Text);
        index.addDocument(doc2, tokens2);

        SearchResponse response = searchEngine.search("\"quick brown\"", "bm25", null, null, 10);
        assertEquals(1, response.getTotalResults());
        assertEquals(1, response.getResults().get(0).getDocument().getId());
    }

    @Test
    void testBooleanQueryOperators() {
        String doc1Text = "Java programming and Spring Boot microservices";
        List<Token> tokens1 = tokenizer.tokenize(doc1Text);
        IndexedDocument doc1 = new IndexedDocument(1, "f1", "/path/doc1.txt", "doc1.txt", "txt", 100, 1000, tokens1.size(), doc1Text);
        index.addDocument(doc1, tokens1);

        String doc2Text = "Python programming and Django web framework";
        List<Token> tokens2 = tokenizer.tokenize(doc2Text);
        IndexedDocument doc2 = new IndexedDocument(2, "f1", "/path/doc2.txt", "doc2.txt", "txt", 100, 1000, tokens2.size(), doc2Text);
        index.addDocument(doc2, tokens2);

        SearchResponse notResponse = searchEngine.search("programming NOT java", "bm25", null, null, 10);
        assertEquals(1, notResponse.getTotalResults());
        assertEquals(2, notResponse.getResults().get(0).getDocument().getId());

        SearchResponse minusResponse = searchEngine.search("programming -java", "bm25", null, null, 10);
        assertEquals(1, minusResponse.getTotalResults());
        assertEquals(2, minusResponse.getResults().get(0).getDocument().getId());

        SearchResponse andResponse = searchEngine.search("java AND spring", "bm25", null, null, 10);
        assertEquals(1, andResponse.getTotalResults());
        assertEquals(1, andResponse.getResults().get(0).getDocument().getId());

        SearchResponse orResponse = searchEngine.search("spring OR django", "bm25", null, null, 10);
        assertEquals(2, orResponse.getTotalResults());
    }

    @Test
    void testSnippetGenerator() {
        String text = "In computational theory, an inverted index is a database index storing a mapping from content to its locations in a document.";
        Set<String> matched = new HashSet<>(Arrays.asList("inverted", "index"));

        String snippet = snippetGenerator.generateSnippet(text, matched);
        assertNotNull(snippet);
        assertTrue(snippet.contains("<mark class=\"findex-highlight\">inverted</mark>"));
        assertTrue(snippet.contains("<mark class=\"findex-highlight\">index</mark>"));
    }
}
