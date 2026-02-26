package io.github.kingg22.godot.internal.initialization;

import io.github.kingg22.godot.internal.ffm.GDExtensionInitializationLevel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeLifecycleTest {
    @Test
    void initializesLevelsInOrderAfterBootstrap() {
        final var calls = new ArrayList<String>();
        final var lifecycle = new BridgeLifecycle(new RecordingHooks(calls), new RecordingLogger(calls));

        lifecycle.bootstrap(11L, 22L);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SERVERS);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_EDITOR);

        assertEquals(
                List.of(
                        "bootstrap:11:22",
                        "info:Runtime bootstrap completed",
                        "core-init",
                        "servers-init",
                        "scene-init",
                        "editor-init"),
                calls);
    }

    @Test
    void sceneInitWithoutBootstrapIsRejected() {
        final var calls = new ArrayList<String>();
        final var lifecycle = new BridgeLifecycle(new RecordingHooks(calls), new RecordingLogger(calls));

        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_SCENE);

        assertEquals(1, calls.size());
        assertTrue(calls.getFirst().startsWith("error:Level "));
    }

    @Test
    void duplicateInitAndDeinitAreIgnored() {
        final var calls = new ArrayList<String>();
        final var lifecycle = new BridgeLifecycle(new RecordingHooks(calls), new RecordingLogger(calls));

        lifecycle.bootstrap(1L, 2L);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);
        lifecycle.onDeinitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);
        lifecycle.onDeinitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);

        assertTrue(calls.contains("core-init"));
        assertTrue(calls.contains("core-deinit"));
        assertTrue(calls.contains("info:Init level already applied: 0"));
        assertTrue(calls.contains("info:Deinit level not active, skipping: 0"));
    }

    @Test
    void shutdownResetsLifecycleState() {
        final var calls = new ArrayList<String>();
        final var lifecycle = new BridgeLifecycle(new RecordingHooks(calls), new RecordingLogger(calls));

        lifecycle.bootstrap(7L, 9L);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);
        lifecycle.shutdown();
        lifecycle.bootstrap(7L, 9L);
        lifecycle.onInitializationLevel(GDExtensionInitializationLevel.GDEXTENSION_INITIALIZATION_CORE);

        assertEquals(2, calls.stream().filter("bootstrap:7:9"::equals).count());
        assertEquals(2, calls.stream().filter("core-init"::equals).count());
        assertTrue(calls.contains("shutdown"));
    }

    private record RecordingHooks(List<String> calls) implements BridgeLifecycle.Hooks {

        @Override
        public void bootstrap(final long getProcAddressPointer, final long libraryPointer) {
            calls.add("bootstrap:" + getProcAddressPointer + ":" + libraryPointer);
        }

        @Override
        public void onCoreInit() {
            calls.add("core-init");
        }

        @Override
        public void onServersInit() {
            calls.add("servers-init");
        }

        @Override
        public void onSceneInit() {
            calls.add("scene-init");
        }

        @Override
        public void onEditorInit() {
            calls.add("editor-init");
        }

        @Override
        public void onEditorDeinit() {
            calls.add("editor-deinit");
        }

        @Override
        public void onSceneDeinit() {
            calls.add("scene-deinit");
        }

        @Override
        public void onServersDeinit() {
            calls.add("servers-deinit");
        }

        @Override
        public void onCoreDeinit() {
            calls.add("core-deinit");
        }

        @Override
        public void shutdown() {
            calls.add("shutdown");
        }
    }

    private record RecordingLogger(List<String> calls) implements BridgeLifecycle.Logger {
        @Override
        public void info(final String message) {
            calls.add("info:" + message);
        }

        @Override
        public void error(final String message) {
            calls.add("error:" + message);
        }
    }
}
