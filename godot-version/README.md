# Godot Versions Tracking

**Starting from v4.6.0**

> [!TIP]
>
> Use the script: `track.sh <version>` to track a new version.
>
> It's interactive and will ask you for the version to track OR the path to the Godot executable.

**Pre requisites**:
- Install the Godot version to track from [godotengine.org](https://godotengine.org/archive/)
- Use absolute path or put the executable in the PATH as `godot`

Scripts:

1. Create a new folder for the version to track

    ```bash
    cd godot-version
    mkdir vX_X_X
    ```

2. Dump the extension API without documentation:

    ```bash
    godot --dump-extension-api --headless
    ```

    Rename it to `extension_api_without_docs.json`
    ```bash
    mv extension_api.json extension_api_without_docs.json
    ```

3. Dump the extension API with documentation:

    ```bash
    godot --dump-extension-api-with-docs --headless
    ```

4. Dump the gdextension interface C header file

    ```bash
    godot --dump-gdextension-interface --headless
    ```

5. Dump the gdextension interface JSON

    ```bash
    godot --dump-gdextension-interface-json --headless
    ```

6. Download the gdextension interface JSON schema from GitHub

    _Replace the version with the one you want to track._
    ```http request
    GET https://github.com/godotengine/godot/blob/4.6/core/extension/gdextension_interface.schema.json
    ```
