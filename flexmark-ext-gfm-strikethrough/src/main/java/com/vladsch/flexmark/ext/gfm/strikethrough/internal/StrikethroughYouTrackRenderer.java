package com.vladsch.flexmark.ext.gfm.strikethrough.internal;

import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.Subscript;
import com.vladsch.flexmark.html.CustomNodeRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;

import java.util.HashSet;
import java.util.Set;

public class StrikethroughYouTrackRenderer implements NodeRenderer {

    public StrikethroughYouTrackRenderer(DataHolder options) {
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        HashSet<NodeRenderingHandler<?>> set = new HashSet<NodeRenderingHandler<?>>();
        set.add(new NodeRenderingHandler<Strikethrough>(Strikethrough.class, new CustomNodeRenderer<Strikethrough>() {
            @Override
            public void render(Strikethrough node, NodeRendererContext context, HtmlWriter html) {
                StrikethroughYouTrackRenderer.this.render(node, context, html);
            }
        }));
        set.add(new NodeRenderingHandler<Subscript>(Subscript.class, new CustomNodeRenderer<Subscript>() {
            @Override
            public void render(Subscript node, NodeRendererContext context, HtmlWriter html) {
                StrikethroughYouTrackRenderer.this.render(node, context, html);
            }
        }));

        return set;
    }

    private void render(Strikethrough node, NodeRendererContext context, HtmlWriter html) {
        html.raw("--");
        context.renderChildren(node);
        html.raw("--");
    }

    private void render(Subscript node, NodeRendererContext context, HtmlWriter html) {
        html.raw("~");
        context.renderChildren(node);
        html.raw("~");
    }

    public static class Factory implements NodeRendererFactory {
        @Override
        public NodeRenderer apply(DataHolder options) {
            return new StrikethroughYouTrackRenderer(options);
        }
    }
}
