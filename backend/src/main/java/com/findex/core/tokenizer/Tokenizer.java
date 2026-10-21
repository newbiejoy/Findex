package com.findex.core.tokenizer;

import com.findex.core.model.Token;
import java.util.List;

public interface Tokenizer {
    List<Token> tokenize(String text);
    List<String> normalizeQueryTerms(String query);
}
