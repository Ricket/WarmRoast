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

import com.google.common.collect.Iterables;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class WarmRoast extends TimerTask {

    private final int interval;
    private final VirtualMachine vm;
    private final Timer timer = new Timer("Roast Pan", true);
    private final McpMapping mapping = new McpMapping();
    private final SortedMap<String, ThreadNode> nodes = new TreeMap<>();
    private MBeanServerConnection mbsc;
    private ThreadMXBean threadBean;
    private String filterThread;
    private long endTime = -1;
    
    public WarmRoast(VirtualMachine vm, int interval) {
        this.vm = vm;
        this.interval = interval;
    }

    void setFilterThread(String filterThread) {
        this.filterThread = filterThread;
    }

    void setEndTime(long l) {
        this.endTime = l;
    }
    
    Map<String, ThreadNode> getData() {
        return nodes;
    }
    
    private Node getNode(String name) {
        ThreadNode node = nodes.get(name);
        if (node == null) {
            node = new ThreadNode(name);
            nodes.put(name, node);
        }
        return node;
    }
    
    McpMapping getMapping() {
        return mapping;
    }

    void connect()
            throws IOException, AgentLoadException, AgentInitializationException {
        // Load the agent
        String connectorAddr = vm.getAgentProperties().getProperty(
                "com.sun.management.jmxremote.localConnectorAddress");
        if (connectorAddr == null) {
            String agent = vm.getSystemProperties().getProperty("java.home")
                    + File.separator + "lib" + File.separator
                    + "management-agent.jar";
            vm.loadAgent(agent);
            connectorAddr = vm.getAgentProperties().getProperty(
                    "com.sun.management.jmxremote.localConnectorAddress");
        }

        // Connect
        JMXServiceURL serviceURL = new JMXServiceURL(connectorAddr);
        JMXConnector connector = JMXConnectorFactory.connect(serviceURL);
        mbsc = connector.getMBeanServerConnection();
        try {
            threadBean = getThreadMXBean();
        } catch (MalformedObjectNameException e) {
            throw new IOException("Bad MX bean name", e);
        }
    }

    void start(InetSocketAddress address) throws Exception {
        timer.scheduleAtFixedRate(this, interval, interval);

        Server server = new Server(address);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new DataViewServlet(this)), "/stack");

        ResourceHandler resources = new ResourceHandler();
        String filesDir = WarmRoast.class.getResource("/www").toExternalForm();
        resources.setResourceBase(filesDir);
        resources.setDirectoriesListed(true);
        resources.setWelcomeFiles(new String[]{ "index.html" });

        HandlerList handlers = new HandlerList();
        handlers.addHandler(context);
        handlers.addHandler(resources);
        server.setHandler(handlers);

        server.start();
        server.join();
    }

    private ThreadMXBean getThreadMXBean() 
            throws IOException, MalformedObjectNameException {
        ObjectName objName = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
        ObjectName name = Iterables.getOnlyElement(mbsc.queryNames(objName, null));
        if (name == null) {
            throw new IOException("No thread MX bean found");
        }

        return ManagementFactory.newPlatformMXBeanProxy(
                mbsc, name.toString(), ThreadMXBean.class);
    }

    @Override
    public synchronized void run() {
        if (endTime >= 0) {
            if (endTime <= System.currentTimeMillis()) {
                cancel();
                System.err.println("Sampling has stopped.");
                return;
            }
        }
        
        ThreadInfo[] threadDumps = threadBean.dumpAllThreads(false, false);
        for (ThreadInfo threadInfo : threadDumps) {
            String threadName = threadInfo.getThreadName();
            StackTraceElement[] stack = threadInfo.getStackTrace();
            
            if (threadName == null || stack == null) {
                continue;
            }
            
            if (filterThread != null && !filterThread.equals(threadName)) {
                continue;
            }
            
            Node node = getNode(threadName);
            node.log(stack, interval);
        }
    }



}
