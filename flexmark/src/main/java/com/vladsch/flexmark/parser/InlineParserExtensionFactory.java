package com.vladsch.flexmark.parser;

import com.vladsch.flexmark.util.dependency.Dependent;

import java.util.function.Function;

public interface InlineParserExtensionFactory extends Function<LightInlineParser, InlineParserExtension>, Dependent<InlineParserExtensionFactory> {
    /**
     * Starting Characters for this inline processor
     *
     * @return set of characters for which this processor should be invoiked
     */
    CharSequence getCharacters();

    /**
     * Create a paragraph pre processor for the document
     *
     * @param inlineParser inline parser instance
     * @return inline parser extension
     */
    InlineParserExtension apply(LightInlineParser inlineParser);
}
