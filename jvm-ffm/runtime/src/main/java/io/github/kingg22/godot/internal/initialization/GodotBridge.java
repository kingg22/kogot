package io.github.kingg22.godot.internal.initialization;

import io.github.kingg22.godot.internal.annotations.Initializer;
import io.github.kingg22.godot.internal.bridge.BridgeContext;

/** Entry point invoked by the GDExtension C bootstrap. */
@SuppressWarnings("unused") // invoked from native
final class GodotBridge {

    private GodotBridge() {
        throw new UnsupportedOperationException("Internal native bridge class, cannot be instantiated");
    }

    private static final BridgeLifecycle LIFECYCLE = new BridgeLifecycle(
            new BridgeLifecycle.Hooks() {
                @Override
                public void bootstrap(final long getProcAddressPointer, final long libraryPointer) {
                    BridgeContext.initialize(getProcAddressPointer, libraryPointer);
                    printJVMInfo();
                }

                @Override
                public void onCoreInit() {
                    System.out.println("[Java] Init level CORE");
                }

                @Override
                public void onServersInit() {
                    System.out.println("[Java] Init level SERVERS");
                }

                @Override
                public void onSceneInit() {
                    // SCENE is where runtime class/script registration should happen.
                    System.out.println("[Java] Init level SCENE");
                    BridgeContext.onSceneInitialized();
                }

                @Override
                public void onEditorInit() {
                    // EDITOR should hold only editor-specific registrations.
                    System.out.println("[Java] Init level EDITOR");
                    BridgeContext.onEditorInitialized();
                }

                @Override
                public void onEditorDeinit() {
                    System.out.println("[Java] Deinit level EDITOR");
                    BridgeContext.onEditorDeinitialized();
                }

                @Override
                public void onSceneDeinit() {
                    System.out.println("[Java] Deinit level SCENE");
                    BridgeContext.onSceneDeinitialized();
                }

                @Override
                public void onServersDeinit() {
                    System.out.println("[Java] Deinit level SERVERS");
                }

                @Override
                public void onCoreDeinit() {
                    System.out.println("[Java] Deinit level CORE");
                }

                @Override
                public void shutdown() {
                    BridgeContext.shutdown();
                }
            },
            new BridgeLifecycle.Logger() {
                @Override
                public void info(final String message) {
                    System.out.println("[Java] " + message);
                }

                @Override
                public void error(final String message) {
                    System.err.println("[Java] " + message);
                }
            });

    /** Called once from C when the runtime reaches SCENE and JVM startup succeeded. */
    @Initializer
    public static void initialize(final long getProcAddressPointer, final long libraryPointer) {
        System.out.println("[Java] GodotBridge.initialize() called");
        LIFECYCLE.bootstrap(getProcAddressPointer, libraryPointer);
    }

    /** Called from C for each level in initialization order. */
    public static void onInitializationLevel(final short level) {
        LIFECYCLE.onInitializationLevel(level);
    }

    /** Called from C for each level in deinitialization order. */
    public static void onDeinitializationLevel(final short level) {
        LIFECYCLE.onDeinitializationLevel(level);
    }

    /** Called from C when JVM is going down. */
    public static void shutdown() {
        System.out.println("[Java] GodotBridge.shutdown() called");
        LIFECYCLE.shutdown();
        System.out.println("[Java] Shutdown complete");
    }

    private static void printJVMInfo() {
        final var runtime = Runtime.getRuntime();
        System.out.println("[Java] JVM Info:");
        System.out.println("  Max Memory: " + (runtime.maxMemory() / 1024 / 1024) + " MB");
        System.out.println("  Total Memory: " + (runtime.totalMemory() / 1024 / 1024) + " MB");
        System.out.println("  Free Memory: " + (runtime.freeMemory() / 1024 / 1024) + " MB");
        System.out.println("  Java Version: " + System.getProperty("java.version"));
    }
}
