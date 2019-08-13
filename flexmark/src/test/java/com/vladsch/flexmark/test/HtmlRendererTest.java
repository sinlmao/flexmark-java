package com.vladsch.flexmark.test;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.html.*;
import com.vladsch.flexmark.html.renderer.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.html.Attributes;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class HtmlRendererTest {

    @Test
    public void htmlAllowingShouldNotEscapeInlineHtml() {
        String rendered = htmlAllowingRenderer().render(parse("paragraph with <span id='foo' class=\"bar\">inline &amp; html</span>"));
        assertEquals("<p>paragraph with <span id='foo' class=\"bar\">inline &amp; html</span></p>\n", rendered);
    }

    @Test
    public void htmlAllowingShouldNotEscapeBlockHtml() {
        String rendered = htmlAllowingRenderer().render(parse("<div id='foo' class=\"bar\">block &amp;</div>"));
        assertEquals("<div id='foo' class=\"bar\">block &amp;</div>\n", rendered);
    }

    @Test
    public void htmlEscapingShouldEscapeInlineHtml() {
        String rendered = htmlEscapingRenderer().render(parse("paragraph with <span id='foo' class=\"bar\">inline &amp; html</span>"));
        // Note that &amp; is not escaped, as it's a normal text node, not part of the inline HTML.
        assertEquals("<p>paragraph with &lt;span id='foo' class=&quot;bar&quot;&gt;inline &amp; html&lt;/span&gt;</p>\n", rendered);
    }

    @Test
    public void htmlEscapingShouldEscapeHtmlBlocks() {
        String rendered = htmlEscapingRenderer().render(parse("<div id='foo' class=\"bar\">block &amp;</div>"));
        assertEquals("<p>&lt;div id='foo' class=&quot;bar&quot;&gt;block &amp;amp;&lt;/div&gt;</p>\n", rendered);
    }

    @Test
    public void textEscaping() {
        String rendered = defaultRenderer().render(parse("escaping: & < > \" '"));
        assertEquals("<p>escaping: &amp; &lt; &gt; &quot; '</p>\n", rendered);
    }

    @Test
    public void percentEncodeUrlDisabled() {
        assertEquals("<p><a href=\"foo&amp;bar\">a</a></p>\n", defaultRenderer().render(parse("[a](foo&amp;bar)")));
        assertEquals("<p><a href=\"ä\">a</a></p>\n", defaultRenderer().render(parse("[a](ä)")));
        assertEquals("<p><a href=\"foo%20bar\">a</a></p>\n", defaultRenderer().render(parse("[a](foo%20bar)")));
    }

    @Test
    public void percentEncodeUrl() {
        // Entities are escaped anyway
        assertEquals("<p><a href=\"foo&amp;bar\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo&amp;bar)")));
        // Existing encoding is preserved
        assertEquals("<p><a href=\"foo%20bar\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo%20bar)")));
        assertEquals("<p><a href=\"foo%61\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo%61)")));
        // Invalid encoding is escaped
        assertEquals("<p><a href=\"foo%25\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo%)")));
        assertEquals("<p><a href=\"foo%25a\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo%a)")));
        assertEquals("<p><a href=\"foo%25a_\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo%a_)")));
        assertEquals("<p><a href=\"foo%25xx\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](foo%xx)")));
        // Reserved characters are preserved, except for '[' and ']'
        assertEquals("<p><a href=\"!*'();:@&amp;=+$,/?#%5B%5D\">a</a></p>\n", percentEncodingRenderer().render(parse("[a](!*'();:@&=+$,/?#[])")));
        // Unreserved characters are preserved
        assertEquals("<p><a href=\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~\">a</a></p>\n",
                percentEncodingRenderer().render(parse("[a](ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~)")));
        // Other characters are percent-encoded (LATIN SMALL LETTER A WITH DIAERESIS)
        assertEquals("<p><a href=\"%C3%A4\">a</a></p>\n",
                percentEncodingRenderer().render(parse("[a](ä)")));
        // Other characters are percent-encoded (MUSICAL SYMBOL G CLEF, surrogate pair in UTF-16)
        assertEquals("<p><a href=\"%F0%9D%84%9E\">a</a></p>\n",
                percentEncodingRenderer().render(parse("[a](\uD834\uDD1E)")));
    }

    @Test
    public void attributeProviderForCodeBlock() {
        AttributeProviderFactory factory = new IndependentAttributeProviderFactory() {
            @Override
            public AttributeProvider apply(LinkResolverContext context) {
                //noinspection ReturnOfInnerClass
                return new AttributeProvider() {
                    @Override
                    public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
                        if (node instanceof FencedCodeBlock && part == CoreNodeRenderer.CODE_CONTENT) {
                            FencedCodeBlock fencedCodeBlock = (FencedCodeBlock) node;
                            // Remove the default attribute for info
                            attributes.remove("class");
                            // Put info in custom attribute instead
                            attributes.replaceValue("data-custom", fencedCodeBlock.getInfo().toString());
                        }
                    }
                };
            }
        };

        HtmlRenderer renderer = HtmlRenderer.builder().attributeProviderFactory(factory).build();
        String rendered = renderer.render(parse("```info\ncontent\n```"));
        assertEquals("<pre><code data-custom=\"info\">content\n</code></pre>\n", rendered);

        String rendered2 = renderer.render(parse("```evil\"\ncontent\n```"));
        assertEquals("<pre><code data-custom=\"evil&quot;\">content\n</code></pre>\n", rendered2);
    }

    @Test
    public void attributeProviderForImage() {
        AttributeProviderFactory factory = new IndependentAttributeProviderFactory() {
            @Override
            public AttributeProvider apply(LinkResolverContext context) {
                return new AttributeProvider() {
                    @Override
                    public void setAttributes(Node node, AttributablePart part, Attributes attributes) {
                        if (node instanceof Image) {
                            attributes.remove("alt");
                            attributes.replaceValue("test", "hey");
                        }
                    }
                };
            }
        };

        HtmlRenderer renderer = HtmlRenderer.builder().attributeProviderFactory(factory).build();
        String rendered = renderer.render(parse("![foo](/url)\n"));
        assertEquals("<p><img src=\"/url\" test=\"hey\" /></p>\n", rendered);
    }

    @Test
    public void overrideNodeRender() {
        NodeRendererFactory nodeRendererFactory = new NodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                context.getHtmlWriter().text("test");
                            }
                        }));

                        return set;
                    }
                };
            }
        };

        HtmlRenderer renderer = HtmlRenderer.builder().nodeRendererFactory(nodeRendererFactory).build();
        String rendered = renderer.render(parse("foo [bar](/url)"));
        assertEquals("<p>foo test</p>\n", rendered);
    }

    @Test
    public void overrideInheritNodeRender() {
        NodeRendererFactory nodeRendererFactory = new NodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                if (node.getText().equals("bar")) {
                                    context.getHtmlWriter().text("test");
                                } else {
                                    context.delegateRender();
                                }
                            }
                        }));

                        return set;
                    }
                };
            }
        };

        HtmlRenderer renderer = HtmlRenderer.builder().nodeRendererFactory(nodeRendererFactory).build();
        String rendered = renderer.render(parse("foo [bar](/url)"));
        assertEquals("<p>foo test</p>\n", rendered);

        rendered = renderer.render(parse("foo [bars](/url)"));
        assertEquals("<p>foo <a href=\"/url\">bars</a></p>\n", rendered);
    }

    @Test
    public void overrideInheritNodeRenderSubContext() {
        NodeRendererFactory nodeRendererFactory = new NodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                if (node.getText().equals("bar")) {
                                    context.getHtmlWriter().text("test");
                                } else {
                                    NodeRendererContext subContext = context.getDelegatedSubContext(true);
                                    if (node.getText().equals("raw")) {
                                        subContext.doNotRenderLinks();
                                    }
                                    subContext.delegateRender();
                                    String s = subContext.getHtmlWriter().toString(-1);
                                    html.raw(s);
                                }
                            }
                        }));

                        return set;
                    }
                };
            }
        };

        HtmlRenderer renderer = HtmlRenderer.builder().nodeRendererFactory(nodeRendererFactory).build();
        String rendered = renderer.render(parse("foo [bar](/url)"));
        assertEquals("<p>foo test</p>\n", rendered);

        rendered = renderer.render(parse("foo [bars](/url)"));
        assertEquals("<p>foo <a href=\"/url\">bars</a></p>\n", rendered);

        rendered = renderer.render(parse("foo [raw](/url)"));
        assertEquals("<p>foo raw</p>\n", rendered);

        rendered = renderer.render(parse("[bar](/url) foo [raw](/url) [bars](/url)"));
        assertEquals("<p>test foo raw <a href=\"/url\">bars</a></p>\n", rendered);
    }

    @Test
    public void overrideInheritDependentNodeRender() {
        NodeRendererFactory nodeRendererFactory = new NodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                if (node.getText().equals("bar")) {
                                    context.getHtmlWriter().text("test");
                                } else if (node.getText().equals("bars")) {
                                    context.getHtmlWriter().text("tests");
                                } else {
                                    context.delegateRender();
                                }
                            }
                        }));

                        return set;
                    }
                };
            }
        };

        NodeRendererFactory nodeRendererFactory2 = new DelegatingNodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                if (node.getText().equals("bar")) {
                                    context.getHtmlWriter().text("testing");
                                } else {
                                    context.delegateRender();
                                }
                            }
                        }));

                        return set;
                    }
                };
            }

            @Override
            public Set<Class<? extends NodeRendererFactory>> getDelegates() {
                Set<Class<? extends NodeRendererFactory>> set = new HashSet<Class<? extends NodeRendererFactory>>();
                set.add(nodeRendererFactory.getClass());
                return set;
            }
        };

        HtmlRenderer renderer = HtmlRenderer.builder().nodeRendererFactory(nodeRendererFactory).nodeRendererFactory(nodeRendererFactory2).build();
        String rendered = renderer.render(parse("foo [bar](/url)"));
        assertEquals("<p>foo testing</p>\n", rendered);

        rendered = renderer.render(parse("foo [bars](/url)"));
        assertEquals("<p>foo tests</p>\n", rendered);
    }

    @Test
    public void overrideInheritDependentNodeRenderReversed() {
        NodeRendererFactory nodeRendererFactory = new NodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                if (node.getText().equals("bar")) {
                                    context.getHtmlWriter().text("test");
                                } else if (node.getText().equals("bars")) {
                                    context.getHtmlWriter().text("tests");
                                } else {
                                    context.delegateRender();
                                }
                            }
                        }));

                        return set;
                    }
                };
            }
        };

        NodeRendererFactory nodeRendererFactory2 = new DelegatingNodeRendererFactory() {
            @Override
            public NodeRenderer apply(DataHolder options) {
                return new NodeRenderer() {
                    @Override
                    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
                        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
                        set.add(new NodeRenderingHandler<Link>(Link.class, new CustomNodeRenderer<Link>() {
                            @Override
                            public void render(Link node, NodeRendererContext context, HtmlWriter html) {
                                if (node.getText().equals("bar")) {
                                    context.getHtmlWriter().text("testing");
                                } else {
                                    context.delegateRender();
                                }
                            }
                        }));

                        return set;
                    }
                };
            }

            @Override
            public Set<Class<? extends NodeRendererFactory>> getDelegates() {
                Set<Class<? extends NodeRendererFactory>> set = new HashSet<Class<? extends NodeRendererFactory>>();
                set.add(nodeRendererFactory.getClass());
                return set;
            }
        };

        // reverse the renderer order
        HtmlRenderer renderer = HtmlRenderer.builder().nodeRendererFactory(nodeRendererFactory2).nodeRendererFactory(nodeRendererFactory).build();
        String rendered = renderer.render(parse("foo [bar](/url)"));
        assertEquals("<p>foo testing</p>\n", rendered);

        rendered = renderer.render(parse("foo [bars](/url)"));
        assertEquals("<p>foo tests</p>\n", rendered);
    }

    @Test
    public void orderedListStartZero() {
        assertEquals("<ol start=\"0\">\n<li>Test</li>\n</ol>\n", defaultRenderer().render(parse("0. Test\n")));
    }

    @Test
    public void imageAltTextWithSoftLineBreak() {
        assertEquals("<p><img src=\"/url\" alt=\"foo\nbar\" /></p>\n",
                defaultRenderer().render(parse("![foo\nbar](/url)\n")));
    }

    @Test
    public void imageAltTextWithHardLineBreak() {
        assertEquals("<p><img src=\"/url\" alt=\"foo\nbar\" /></p>\n",
                defaultRenderer().render(parse("![foo  \nbar](/url)\n")));
    }

    @Test
    public void imageAltTextWithEntities() {
        assertEquals("<p><img src=\"/url\" alt=\"foo \u00E4\" /></p>\n",
                defaultRenderer().render(parse("![foo &auml;](/url)\n")));
    }

    static class CustomLinkResolverImpl implements LinkResolver {
        public static final DataKey<String> DOC_RELATIVE_URL = new DataKey<>("DOC_RELATIVE_URL", "");

        final String docUrl;

        public CustomLinkResolverImpl(LinkResolverContext context) {
            docUrl = DOC_RELATIVE_URL.getFrom(context.getOptions());
        }

        @Override
        public ResolvedLink resolveLink(Node node, LinkResolverContext context, ResolvedLink link) {
            if (node instanceof Link) {
                Link linkNode = (Link) node;
                if (linkNode.getUrl().equals("/url")) {
                    return link.withUrl("www.url.com" + docUrl);
                }
            }
            return null;
        }

        static class Factory extends IndependentLinkResolverFactory {
            @Override
            public LinkResolver apply(LinkResolverContext context) {
                return new CustomLinkResolverImpl(context);
            }
        }
    }

    @Test
    public void withOptions_customLinkResolver() {
        // make sure custom link resolver is preserved when using withOptions() on HTML builder
        DataHolder OPTIONS = new MutableDataSet().set(CustomLinkResolverImpl.DOC_RELATIVE_URL, "/url");
        DataHolder OPTIONS1 = new MutableDataSet().set(CustomLinkResolverImpl.DOC_RELATIVE_URL, "/url1");
        DataHolder OPTIONS2 = new MutableDataSet().set(CustomLinkResolverImpl.DOC_RELATIVE_URL, "/url2");
        HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS).linkResolverFactory(new CustomLinkResolverImpl.Factory()).build();
        HtmlRenderer renderer1 = renderer.withOptions(OPTIONS1);
        HtmlRenderer renderer2 = renderer.withOptions(OPTIONS2);
        String rendered = renderer.render(parse("foo [bar](/url)"));
        String rendered1 = renderer1.render(parse("foo [bar](/url)"));
        String rendered2 = renderer2.render(parse("foo [bar](/url)"));

        assertEquals("<p>foo <a href=\"www.url.com/url\">bar</a></p>\n", rendered);
        assertEquals("<p>foo <a href=\"www.url.com/url1\">bar</a></p>\n", rendered1);
        assertEquals("<p>foo <a href=\"www.url.com/url2\">bar</a></p>\n", rendered2);
    }

    private static HtmlRenderer defaultRenderer() {
        return HtmlRenderer.builder().build();
    }

    private static HtmlRenderer htmlAllowingRenderer() {
        return HtmlRenderer.builder().escapeHtml(false).build();
    }

    private static HtmlRenderer htmlEscapingRenderer() {
        return HtmlRenderer.builder().escapeHtml(true).build();
    }

    private static HtmlRenderer percentEncodingRenderer() {
        return HtmlRenderer.builder().percentEncodeUrls(true).build();
    }

    private static Node parse(String source) {
        return Parser.builder().build().parse(source);
    }
}
