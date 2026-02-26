package io.github.kingg22.godot.internal.initialization;

import io.github.kingg22.godot.internal.ffm.GDExtensionInitializationLevel;

import java.util.HashSet;
import java.util.Set;

/**
 * Coordinates initialization/deinitialization callbacks by GDExtension level.
 * Keeps ordering and idempotency guarantees on the JVM side.
 */
final class BridgeLifecycle {
    interface Hooks {
        void bootstrap(final long getProcAddressPointer, final long libraryPointer);

        void onCoreInit();

        void onServersInit();

        void onSceneInit();

        void onEditorInit();

        void onEditorDeinit();

        void onSceneDeinit();

        void onServersDeinit();

        void onCoreDeinit();

        void shutdown();
    }

    interface Logger {
        void info(final String message);

        void error(final String message);
    }

    private final Hooks hooks;
    private final Logger logger;
    private final Set<Short> initializedLevels = new HashSet<>();

    private boolean bootstrapped;

    BridgeLifecycle(final Hooks hooks, final Logger logger) {
        this.hooks = hooks;
        this.logger = logger;
    }

    synchronized void bootstrap(final long getProcAddressPointer, final long libraryPointer) {
        if (bootstrapped) {
            logger.info("Runtime already bootstrapped, skipping initialize(JJ)");
            return;
        }
        hooks.bootstrap(getProcAddressPointer, libraryPointer);
        bootstrapped = true;
        logger.info("Runtime bootstrap completed");
    }

    synchronized void onInitializationLevel(final short level) {
        if (!bootstrapped && level >= GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE) {
            logger.error("Level " + level + " reached before runtime bootstrap");
            return;
        }
        if (!initializedLevels.add(level)) {
            logger.info("Init level already applied: " + level);
            return;
        }

        switch (level) {
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE -> hooks.onCoreInit();
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SERVERS -> hooks.onServersInit();
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE -> hooks.onSceneInit();
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_EDITOR -> hooks.onEditorInit();
            default -> logger.error("Unknown init level: " + level);
        }
    }

    synchronized void onDeinitializationLevel(final short level) {
        if (!initializedLevels.remove(level)) {
            logger.info("Deinit level not active, skipping: " + level);
            return;
        }

        switch (level) {
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_EDITOR -> hooks.onEditorDeinit();
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE -> hooks.onSceneDeinit();
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SERVERS -> hooks.onServersDeinit();
            case GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE -> hooks.onCoreDeinit();
            default -> logger.error("Unknown deinit level: " + level);
        }
    }

    synchronized void shutdown() {
        hooks.shutdown();
        initializedLevels.clear();
        bootstrapped = false;
        logger.info("Lifecycle shutdown completed");
    }
}
