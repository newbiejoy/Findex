package com.findex.core.model;

public class Token {
    private final String text;
    private final int position;
    private final int startOffset;
    private final int endOffset;

    public Token(String text, int position, int startOffset, int endOffset) {
        this.text = text;
        this.position = position;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public String getText() {
        return text;
    }

    public int getPosition() {
        return position;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    @Override
    public String toString() {
        return "Token{" + text + " @" + position + " [" + startOffset + ":" + endOffset + "]}";
    }
}
