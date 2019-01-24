package com.sk89q.warmroast;

import com.beust.jcommander.JCommander;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static final String SEPARATOR =
            "------------------------------------------------------------------------";

    public static void main(String[] args) throws IOException {
        RoastOptions opt = new RoastOptions();
        JCommander jc = new JCommander(opt, args);
        jc.setProgramName("warmroast");

        if (opt.help) {
            jc.usage();
            System.exit(0);
        }

        System.err.println(SEPARATOR);
        System.err.println("WarmRoast");
        System.err.println("http://github.com/sk89q/warmroast");
        System.err.println(SEPARATOR);
        System.err.println("");

        VirtualMachine vm = null;

        if (opt.pid != null) {
            try {
                vm = VirtualMachine.attach(String.valueOf(opt.pid));
                System.err.println("Attaching to PID " + opt.pid + "...");
            } catch (AttachNotSupportedException | IOException e) {
                System.err.println("Failed to attach VM by PID " + opt.pid);
                e.printStackTrace();
                System.exit(1);
            }
        } else if (opt.vmName != null) {
            for (VirtualMachineDescriptor desc : VirtualMachine.list()) {
                if (desc.displayName().contains(opt.vmName)) {
                    try {
                        vm = VirtualMachine.attach(desc);
                        System.err.println("Attaching to '" + desc.displayName() + "'...");

                        break;
                    } catch (AttachNotSupportedException | IOException e) {
                        System.err.println("Failed to attach VM by name '" + opt.vmName + "'");
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        }

        if (vm == null) {
            List<VirtualMachineDescriptor> descriptors = VirtualMachine.list();
            descriptors.sort(Comparator.comparing(VirtualMachineDescriptor::displayName));

            System.err.println("Choose a VM:");

            // Print list of VMs
            int i = 1;
            for (VirtualMachineDescriptor desc : descriptors) {
                System.err.println("[" + (i++) + "] " + desc.displayName());
            }

            // Ask for choice
            System.err.println("");
            System.err.print("Enter choice #: ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String s = reader.readLine();

            // Get the VM
            try {
                int choice = Integer.parseInt(s) - 1;
                if (choice < 0 || choice >= descriptors.size()) {
                    System.err.println("");
                    System.err.println("Given choice is out of range.");
                    System.exit(1);
                }
                vm = VirtualMachine.attach(descriptors.get(choice));
            } catch (NumberFormatException e) {
                System.err.println("");
                System.err.println("That's not a number. Bye.");
                System.exit(1);
            } catch (AttachNotSupportedException | IOException e) {
                System.err.println("");
                System.err.println("Failed to attach VM");
                e.printStackTrace();
                System.exit(1);
            }
        }

        InetSocketAddress address = new InetSocketAddress(opt.bindAddress, opt.port);

        WarmRoast roast = new WarmRoast(vm, opt.interval);
        if (opt.mappingsDir != null) {
            File dir = new File(opt.mappingsDir);
            File joined = new File(dir, "joined.srg");
            File methods = new File(dir, "methods.csv");
            try {
                roast.getMapping().read(joined, methods);
            } catch (IOException e) {
                System.err.println(
                        "Failed to read the mappings files (joined.srg, methods.csv) " +
                        "from " + dir.getAbsolutePath() + ": " + e.getMessage());
                System.exit(2);
            }
        }

        System.err.println(SEPARATOR);

        roast.setFilterThread(opt.threadName);

        if (opt.timeout != null && opt.timeout > 0) {
            roast.setEndTime(System.currentTimeMillis() + opt.timeout * 1000);
            System.err.println("Sampling set to stop in " + opt.timeout + " seconds.");
        }

        System.err.println("Starting a server on " + address.toString() + "...");
        System.err.println("Once the server starts (shortly), visit the URL in your browser.");
        System.err.println("Note: The longer you wait before using the output of that " +
        		"webpage, the more accurate the results will be.");

        try {
            roast.connect();
            roast.start(address);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(3);
        }
    }
}
