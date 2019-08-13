package com.vladsch.flexmark.ext.enumerated.reference.internal;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.enumerated.reference.*;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.SegmentedSequenceBuilder;

@SuppressWarnings("WeakerAccess")
public class EnumRefTextCollectingVisitor {
    private SegmentedSequenceBuilder out;
    private final NodeVisitor visitor;
    private Runnable ordinalRunnable;

    public EnumRefTextCollectingVisitor() {
        this(-1);
    }

    public EnumRefTextCollectingVisitor(int ordinal) {
        ordinalRunnable = ordinal < 0 ? null : new Runnable() {
            @Override
            public void run() {
                out.append(String.valueOf(ordinal));
            }
        };

        visitor = new NodeVisitor(
                new VisitHandler<Text>(Text.class, new Visitor<Text>() {
                    @Override
                    public void visit(Text node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                }),
                new VisitHandler<TextBase>(TextBase.class, new Visitor<TextBase>() {
                    @Override
                    public void visit(TextBase node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                }),
                new VisitHandler<HtmlEntity>(HtmlEntity.class, new Visitor<HtmlEntity>() {
                    @Override
                    public void visit(HtmlEntity node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                }),
                new VisitHandler<SoftLineBreak>(SoftLineBreak.class, new Visitor<SoftLineBreak>() {
                    @Override
                    public void visit(SoftLineBreak node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                }),
                new VisitHandler<HardLineBreak>(HardLineBreak.class, new Visitor<HardLineBreak>() {
                    @Override
                    public void visit(HardLineBreak node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                }),
                new VisitHandler<EnumeratedReferenceText>(EnumeratedReferenceText.class, new Visitor<EnumeratedReferenceText>() {
                    @Override
                    public void visit(EnumeratedReferenceText node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                }),
                new VisitHandler<EnumeratedReferenceLink>(EnumeratedReferenceLink.class, new Visitor<EnumeratedReferenceLink>() {
                    @Override
                    public void visit(EnumeratedReferenceLink node) {
                        EnumRefTextCollectingVisitor.this.visit(node);
                    }
                })
        );
    }

    public String getText() {
        return out.toString();
    }

    /**
     * @param node format node
     * @deprecated use {@link #collect(BasedSequence, EnumeratedReferenceRendering[], String)}
     */
    @Deprecated
    public void collect(Node node) {
        out = new SegmentedSequenceBuilder(node.getChars());
        visitor.visit(node);
    }

    /**
     * @param node format node
     * @deprecated use {@link #collectAndGetText(BasedSequence, EnumeratedReferenceRendering[], String)}
     */
    @Deprecated
    public String collectAndGetText(Node node) {
        collect(node);
        return out.toString();
    }

    /**
     * @param node format node
     * @deprecated use {@link #collectAndGetSegments(BasedSequence, EnumeratedReferenceRendering[], String)}
     */
    @Deprecated
    public BasedSequence[] collectAndGetSegments(Node node) {
        collect(node);
        return out.toSegments();
    }

    /**
     * @param node format node
     * @deprecated use {@link #collectAndGetSequence(BasedSequence, EnumeratedReferenceRendering[], String)}
     */
    @Deprecated
    public BasedSequence collectAndGetSequence(Node node) {
        collect(node);
        return out.toBasedSequence();
    }

    public void collect(BasedSequence basedSequence, EnumeratedReferenceRendering[] renderings, String defaultFormat) {
        out = new SegmentedSequenceBuilder(basedSequence);
        EnumeratedReferences.renderReferenceOrdinals(renderings, new OrdinalRenderer(this));
    }

    private static class OrdinalRenderer implements EnumeratedOrdinalRenderer {
        final EnumRefTextCollectingVisitor renderer;

        public OrdinalRenderer(EnumRefTextCollectingVisitor renderer) {
            this.renderer = renderer;
        }

        @Override
        public void startRendering(EnumeratedReferenceRendering[] renderings) {

        }

        @Override
        public void setEnumOrdinalRunnable(Runnable runnable) {
            renderer.ordinalRunnable = runnable;
        }

        @Override
        public Runnable getEnumOrdinalRunnable() {
            return renderer.ordinalRunnable;
        }

        @Override
        public void render(int referenceOrdinal, EnumeratedReferenceBlock referenceFormat, String defaultText, boolean needSeparator) {
            Runnable compoundRunnable = renderer.ordinalRunnable;

            if (referenceFormat != null) {
                renderer.ordinalRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (compoundRunnable != null) compoundRunnable.run();
                        renderer.out.append(String.valueOf(referenceOrdinal));
                        if (needSeparator) renderer.out.append(".");
                    }
                };

                renderer.visitor.visitChildren(referenceFormat);
            } else {
                renderer.out.append(defaultText + " ");
                if (compoundRunnable != null) compoundRunnable.run();
                renderer.out.append(String.valueOf(referenceOrdinal));
                if (needSeparator) renderer.out.append(".");
            }
        }

        @Override
        public void endRendering() {

        }
    }

    public String collectAndGetText(BasedSequence basedSequence, EnumeratedReferenceRendering[] renderings, String defaultFormat) {
        collect(basedSequence, renderings, defaultFormat);
        return out.toString();
    }

    public BasedSequence[] collectAndGetSegments(BasedSequence basedSequence, EnumeratedReferenceRendering[] renderings, String defaultFormat) {
        collect(basedSequence, renderings, defaultFormat);
        return out.toSegments();
    }

    public BasedSequence collectAndGetSequence(BasedSequence basedSequence, EnumeratedReferenceRendering[] renderings, String defaultFormat) {
        collect(basedSequence, renderings, defaultFormat);
        return out.toBasedSequence();
    }

    private void visit(EnumeratedReferenceText node) {
        String text = node.getText().toString();

        if (text.isEmpty()) {
            // placeholder for ordinal
            if (ordinalRunnable != null) ordinalRunnable.run();
        }
    }

    private void visit(EnumeratedReferenceLink node) {
        String text = node.getText().toString();

        if (text.isEmpty()) {
            // placeholder for ordinal
            if (ordinalRunnable != null) ordinalRunnable.run();
        }
    }

    private void visit(SoftLineBreak node) {
        out.append(node.getChars());
    }

    private void visit(HardLineBreak node) {
        BasedSequence chars = node.getChars();
        out.append(chars.subSequence(chars.length() - 1, chars.length()));
    }

    private void visit(HtmlEntity node) {
        out.append(node.getChars().unescape());
    }

    private void visit(Text node) {
        if (!node.isOrDescendantOfType(DoNotCollectText.class)) {
            out.append(node.getChars());
        }
    }

    private void visit(TextBase node) {
        out.append(node.getChars());
    }
}
