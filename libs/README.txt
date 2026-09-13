Place your TerraFirmaCraft jar here before building.

Expected filename (matches gradle.properties -> tfc_jar_name):
    TerraFirmaCraft-NeoForge-1_21_1-4_2_6.jar

The build uses this jar as a compileOnly dependency so the mixin/imports resolve.
It is NOT bundled into the output jar - your modpack provides TFC at runtime.

If your jar has a different name/version, either rename it to match, or edit
tfc_jar_name in gradle.properties. A missing or misnamed jar is the usual cause
of "cannot find symbol: class Predator" during compilation.
