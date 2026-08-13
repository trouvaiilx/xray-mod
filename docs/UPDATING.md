# Updating Guide

This guide provides step-by-step instructions for updating the **X-Ray Mod** codebase to support new versions of **Minecraft**, **Sodium**, **Fabric Loader**, or **Fabric API**.

---

## Table of Contents
- [Overview](#overview)
- [Step-by-Step Update Process](#step-by-step-update-process)
  - [1. Update `gradle.properties`](#1-update-gradleproperties)
  - [2. Update `fabric.mod.json`](#2-update-fabricmodjson)
  - [3. Check Java & Compiler Specs](#3-check-java--compiler-specs)
  - [4. Rebuild & Resolve Dependency Changes](#4-rebuild--resolve-dependency-changes)
  - [5. Verify Sodium Mixins & Compat Layer](#5-verify-sodium-mixins--compat-layer)
- [Troubleshooting Common Update Issues](#troubleshooting-common-update-issues)
- [Verification & Testing](#verification--testing)

---

## Overview

The X-Ray Mod depends on three primary external targets:
1. **Minecraft**: Target game version.
2. **Fabric API & Fabric Loader**: Modding platform framework.
3. **Sodium**: The chunk rendering engine into which X-Ray injects custom block visibility and lighting logic.

Because X-Ray hooks directly into Sodium's rendering pipeline via Mixins, updating the mod requires verifying both standard Fabric API changes and Sodium internal API modifications.

---

## Step-by-Step Update Process

### 1. Update `gradle.properties`

All primary component versions are declared in `gradle.properties`.

Open `gradle.properties` and update the following version variables:

```properties
# Target Minecraft version
minecraft_version=26.2

# Fabric Loader version
loader_version=0.19.3

# Fabric Loom Gradle plugin version
loom_version=1.17-SNAPSHOT

# Mod version (e.g. increment patch or minor)
mod_version=1.5.1

# Fabric API dependency version (matching target MC version)
fabric_version=0.156.0+26.2

# Sodium version (retrieved from Modrinth Maven)
sodium_version=mc26.2-0.9.1-fabric
```

> **Note on Fabric Loom**: For Minecraft 26.1+ (unobfuscated game binaries), Loom uses `net.fabricmc.fabric-loom` without remapping configurations. Ensure `loom_version` supports the target version.

---

### 2. Update `fabric.mod.json`

Open `src/main/resources/fabric.mod.json` and update the version requirement bounds in the `depends` section:

```json
  "depends": {
    "fabricloader": ">=0.16.0",
    "minecraft": "26.2",
    "fabric-api": "*"
  }
```

- Update `"minecraft"` to the target version or version range (e.g. `"26.2"` or `">=26.2"`).
- Update `"fabricloader"` if a newer minimum loader release is required.

---

### 3. Check Java & Compiler Specs

Minecraft versions mandate specific Java Development Kit (JDK) versions:
- **Minecraft 26.1+**: Requires **JDK 25**.
- **Minecraft 1.20.5+**: Requires **JDK 21**.

Verify that `build.gradle` specifies the correct Java version compatibility:

```groovy
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 25
}
```

---

### 4. Rebuild & Resolve Dependency Changes

Run a clean build command to download new dependencies and recompile the project:

```powershell
.\gradlew.bat clean build
```

If compilation fails due to missing or changed classes:
1. **Fabric API changes**: Update imports or method references in `XrayClient.java` or command registration handlers.
2. **Minecraft Mojang mapping changes**: Check method signature changes if Mojang updated underlying internal class names.

---

### 5. Verify Sodium Mixins & Compat Layer

Sodium updates frequently refactor internal chunk rendering pipelines. Check the following components when upgrading Sodium:

1. **Sodium Mixin Targets**: Inspect `src/main/java/io/github/trouvaiilx/xray/mixin/`
   - Verify that target Sodium methods in chunk rendering pipelines still exist and have not changed parameter signatures.
2. **Sodium Compatibility Layer**: Inspect `src/main/java/io/github/trouvaiilx/xray/compat/`
   - Verify that block occlusion, lightmap overriding, or chunk re-meshing API calls remain compatible.

---

## Troubleshooting Common Update Issues

### Issue 1: `Cannot use Mojang mappings in a non-obfuscated environment`
- **Cause**: Minecraft 26.1+ ships unobfuscated from Mojang.
- **Solution**: Ensure `build.gradle` does **not** call `loom.officialMojangMappings()` or declare Yarn mappings.

### Issue 2: Mixin Injection Failures at Runtime
- **Cause**: Sodium renamed or refactored internal methods targeted by Mixin annotations.
- **Solution**: Inspect the runtime stack trace in `logs/latest.log` to identify the failing mixin annotation. Update the `@Inject` or `@Redirect` target method signature to match the updated Sodium source.

---

## Verification & Testing

After completing updates, run the following verification steps:

1. **Build Artifact Verification**:
   ```powershell
   .\gradlew.bat clean build
   ```
   Ensure the output JAR in `build/libs/` follows the naming convention `xray-mod-<mod_version>+<mc_version>.jar`.

2. **Development Runtime Test**:
   ```powershell
   .\gradlew.bat runClient
   ```
   - Press **Right Shift** to test the GUI menu.
   - Use `/xray toggle` to test chunk re-meshing and fullbright rendering.
