/*
 * WarmRoast
 * Copyright (C) 2013 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.sk89q.warmroast;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import com.google.common.html.HtmlEscapers;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Node implements Comparable<Node> {
    
    private static final NumberFormat cssDec = NumberFormat.getPercentInstance(Locale.US);
    private static final Escaper htmlEscaper = HtmlEscapers.htmlEscaper();
    private static final Escaper jsonEscaper;
    private final String name;
    private final Map<String, StackTraceNode> children = new HashMap<>();
    private long totalTime;
    
    static {
        cssDec.setGroupingUsed(false);
        cssDec.setMaximumFractionDigits(2);

        Escapers.Builder builder = Escapers.builder()
                .addEscape('\\', "\\\\")
                .addEscape('"', "\\\"")
                .addEscape('/', "\\/")
                .addEscape('\b', "\\b")
                .addEscape('\t', "\\t")
                .addEscape('\n', "\\n")
                .addEscape('\f', "\\f")
                .addEscape('\r', "\\r");
        for (char c = 0; c < ' '; c++) {
            String t = "000" + Integer.toHexString(c);
            builder.addEscape(c, "\\u" + t.substring(t.length() - 4));
        }
        builder.setSafeRange((char) 0, (char) 0x7f);
        builder.setUnsafeReplacement("");
        jsonEscaper = builder.build();
    }

    Node(String name) {
        this.name = name;
    }
    
    String getName() {
        return name;
    }
    
    protected abstract String getNameHtml(McpMapping mapping);

    protected abstract String getNameJson(McpMapping mapping);

    private Collection<StackTraceNode> getChildren() {
        return children.values().stream().sorted().collect(Collectors.toList());
    }

    private Node getChild(String className, String methodName) {
        String name = StackTraceNode.getName(className, methodName);
        StackTraceNode child = children.get(name);
        if (child == null) {
            child = new StackTraceNode(className, methodName);
            children.put(child.getName(), child);
        }
        return child;
    }
    
    long getTotalTime() {
        return totalTime;
    }
    
    private void log(StackTraceElement[] elements, int skip, long time) {
        totalTime += time;
        
        if (elements.length - skip == 0) {
            return;
        }
        
        StackTraceElement bottom = elements[elements.length - (skip + 1)];
        getChild(bottom.getClassName(), bottom.getMethodName())
                .log(elements, skip + 1, time);
    }
    
    void log(StackTraceElement[] elements, long time) {
        log(elements, 0, time);
    }

    @Override
    public int compareTo(Node o) {
        return getName().compareTo(o.getName());
    }
    
    private void writeHtml(StringBuilder builder, McpMapping mapping, long totalTime) {
        builder.append("<div class=\"node collapsed\">");
        builder.append("<div class=\"name\">");
        builder.append(getNameHtml(mapping));
        builder.append("<span class=\"percent\">");
        builder
                .append(String.format("%.2f", getTotalTime() / (double) totalTime * 100))
                .append("%");
        builder.append("</span>");
        builder.append("<span class=\"time\">");
        builder.append(getTotalTime()).append("ms");
        builder.append("</span>");
        builder.append("<span class=\"bar\">");
        builder.append("<span class=\"bar-inner\" style=\"width:")
                .append(formatCssPct(getTotalTime() / (double) totalTime))
                .append("\">");
        builder.append("</span>");
        builder.append("</span>");
        builder.append("</div>");
        builder.append("<ul class=\"children\">");
        for (Node child : getChildren()) {
            builder.append("<li>");
            child.writeHtml(builder, mapping, totalTime);
            builder.append("</li>");
        }
        builder.append("</ul>");
        builder.append("</div>");
    }

    String toHtml(McpMapping mapping) {
        StringBuilder builder = new StringBuilder();
        writeHtml(builder, mapping, getTotalTime());
        return builder.toString();
    }

    protected void writeJson(StringBuilder builder, McpMapping mapping, long totalTime) {
        builder.append("{");
        builder.append("\"name\":\"");
        builder.append(getNameJson(mapping));
        builder.append("\",\"percent\":");
        builder.append(formatCssPct(getTotalTime() / (double) totalTime));
        builder.append(",\"timeMs\":");
        builder.append(getTotalTime());
        builder.append(",\"children\":[");
        for (Iterator<StackTraceNode> it = getChildren().iterator(); it.hasNext(); ) {
            StackTraceNode node = it.next();
            node.writeJson(builder, mapping, totalTime);
            if (it.hasNext()) {
                builder.append(",");
            }
        }
        builder.append("]}");
    }

    String toJson(McpMapping mapping) {
        StringBuilder builder = new StringBuilder();
        writeJson(builder, mapping, getTotalTime());
        return builder.toString();
    }
    
    private void writeString(StringBuilder builder, int indent) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            b.append(" ");
        }
        String padding = b.toString();
        
        for (Node child : getChildren()) {
            builder.append(padding).append(child.getName());
            builder.append(" ");
            builder.append(getTotalTime()).append("ms");
            builder.append("\n");
            child.writeString(builder, indent + 1);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        writeString(builder, 0);
        return builder.toString();
    }
    
    private static String formatCssPct(double pct) {
        return cssDec.format(pct);
    }
    
    static String escapeHtml(String str) {
        return htmlEscaper.escape(str);
    }

    static String escapeJson(String str) {
        return jsonEscaper.escape(str);
    }

}
