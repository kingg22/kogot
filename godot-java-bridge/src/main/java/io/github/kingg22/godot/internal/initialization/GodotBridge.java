package io.github.kingg22.godot.internal.initialization;

import io.github.kingg22.godot.internal.annotations.Initializer;
import io.github.kingg22.godot.internal.bridge.BridgeContext;
import io.github.kingg22.godot.internal.registry.GeneratedRegistry;

/** Entry point invoked by the GDExtension C bootstrap. */
@SuppressWarnings("unused") // invoked from native
final class GodotBridge {
    private static boolean initialized = false;

    /** Called from C when GDExtension initializes. */
    @Initializer
    public static void initialize(final long getProcAddressPointer, final long libraryPointer) {
        System.out.println("[Java] GodotBridge.initialize() called");

        if (initialized) {
            System.out.println("[Java] Already initialized, skipping...");
            return;
        }

        try {
            // Initialize your game systems here
            System.out.println("[Java] Setting up game systems...");

            BridgeContext.initialize(getProcAddressPointer, libraryPointer);
            GeneratedRegistry.registerAll(BridgeContext.getRegistry());

            // Example: Load configurations
            loadConfigurations();

            // Example: Initialize managers
            initializeManagers();

            initialized = true;
            System.out.println("[Java] Initialization complete!");
        } catch (Exception e) {
            System.err.println("[Java] Error during initialization:");
            e.printStackTrace();
        }
    }

    /** Called from C when GDExtension is being unloaded. */
    public static void shutdown() {
        System.out.println("[Java] GodotBridge.shutdown() called");

        if (!initialized) {
            return;
        }

        try {
            // Cleanup your resources here
            System.out.println("[Java] Cleaning up resources...");

            BridgeContext.shutdown();

            initialized = false;
            System.out.println("[Java] Shutdown complete!");

        } catch (Exception e) {
            System.err.println("[Java] Error during shutdown:");
            e.printStackTrace();
        }
    }

    /** Example method for loading configurations. */
    private static void loadConfigurations() {
        System.out.println("[Java] Loading configurations...");
        // Load your config files, databases, etc.
    }

    /** Example method for initializing managers. */
    private static void initializeManagers() {
        System.out.println("[Java] Initializing managers...");
        // Initialize your game managers (audio, network, etc.)
    }

    /** Example: Get JVM information. */
    public static void printJVMInfo() {
        final var runtime = Runtime.getRuntime();
        System.out.println("[Java] JVM Info:");
        System.out.println("  Max Memory: " + (runtime.maxMemory() / 1024 / 1024) + " MB");
        System.out.println("  Total Memory: " + (runtime.totalMemory() / 1024 / 1024) + " MB");
        System.out.println("  Free Memory: " + (runtime.freeMemory() / 1024 / 1024) + " MB");
        System.out.println("  Java Version: " + System.getProperty("java.version"));
    }
}
