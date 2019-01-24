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

import java.util.List;

public class StackTraceNode extends Node {
    
    private final String className;
    private final String methodName;

    public StackTraceNode(String className, String methodName) {
        super(className + "." + methodName + "()");
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public String getNameHtml(McpMapping mapping) {
        ClassMapping classMapping = mapping.mapClass(className);
        if (classMapping != null) {
            String className = "<span class=\"matched\" title=\"" +
                    escapeHtml(this.className) + "\">" +
                    escapeHtml(classMapping.getActual()) + "</span>";

            List<String> actualMethods = classMapping.mapMethod(methodName);
            if (actualMethods.size() == 0) {
                return className + "." + escapeHtml(methodName) + "()";
            } else if (actualMethods.size() == 1) {
                return className +
                        ".<span class=\"matched\" title=\"" + 
                        escapeHtml(methodName) + "\">" +
                        escapeHtml(actualMethods.get(0)) + "</span>()";
            } else {
                StringBuilder builder = new StringBuilder();
                boolean first = true;
                for (String m : actualMethods) {
                    if (!first) {
                        builder.append(" ");
                    }
                    builder.append(m);
                    first = false;
                }
                return className +
                        ".<span class=\"multiple-matches\" title=\"" + 
                            builder.toString() + "\">" + escapeHtml(methodName) + "</span>()";
            }
        } else {
            String actualMethod = mapping.mapMethodId(methodName);
            if (actualMethod == null) {
                return escapeHtml(className) + "." + escapeHtml(methodName) + "()";
            } else {
                return className +
                        ".<span class=\"matched\" title=\"" + 
                        escapeHtml(methodName) + "\">" +
                        escapeHtml(actualMethod) + "</span>()";
            }
        }
    }

    @Override
    public String getNameJson(McpMapping mapping) {
        ClassMapping classMapping = mapping.mapClass(className);
        if (classMapping != null) {
            // TODO finish writing this
            return null;
        } else {
            String actualMethod = mapping.mapMethodId(methodName);
            if (actualMethod == null) {
                return escapeJson(className + "." + methodName);
            } else {
                return escapeJson(className + "." + actualMethod);
            }
        }
    }

    @Override
    public int compareTo(Node o) {
        if (getTotalTime() == o.getTotalTime()) {
            return 0;
        } else if (getTotalTime()> o.getTotalTime()) {
            return -1;
        } else {
            return 1;
        }
    }

}
