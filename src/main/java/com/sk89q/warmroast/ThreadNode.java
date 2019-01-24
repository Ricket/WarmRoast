package com.sk89q.warmroast;

public class ThreadNode extends Node {
    ThreadNode(String threadName) {
        super(threadName);
    }

    @Override
    protected String getNameHtml(McpMapping mapping) {
        return escapeHtml(getName());
    }

    @Override
    protected String getNameJson(McpMapping mapping) {
        return escapeJson(getName());
    }
}
