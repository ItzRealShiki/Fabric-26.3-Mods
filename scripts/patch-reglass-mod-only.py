#!/usr/bin/env python3
"""Keep ReGlass visual effects scoped to custom mod UI."""
from pathlib import Path

config = Path("src/main/resources/reglass.mixins.json")
source = config.read_text(encoding="utf-8")

# Remove ReGlass' global vanilla slider styling mixin.
source = "\n".join(line for line in source.splitlines()
                   if "SliderWidgetMixin" not in line)

# Keep only the client rendering infrastructure required by custom glass widgets.
start = source.index('  "client": [')
open_bracket = source.index('[', start)
depth = 0
end = None
for i in range(open_bracket, len(source)):
    if source[i] == '[':
        depth += 1
    elif source[i] == ']':
        depth -= 1
        if depth == 0:
            end = i + 1
            break
if end is None:
    raise SystemExit("Could not parse ReGlass client mixin list")

replacement = '''  "client": [
    "accessor.DrawContextAccessor",
    "accessor.GameRendererAccessor",
    "accessor.ScissorStackAccessor",
    "logical.GameRendererMixin",
    "widgets.DrawContextMixin",
    "widgets.GuiRendererMixin"
  ]'''
source = source[:start] + replacement + source[end:]

for forbidden in ("client.ScreenMixin", "client.InventoryScreenMixin",
                  "client.TitleScreenMixin", "client.InGameHudMixin",
                  "client.OptionsMixin", "SliderWidgetMixin"):
    if forbidden in source:
        raise SystemExit("Vanilla UI mixin remains enabled: " + forbidden)

# ReGlass' rendering API must be used only by PrivacyMod's own screen.
privacy_sources = Path("../PrivacyMod/src").rglob("*.java")
api_users = []
for java_file in privacy_sources:
    java_source = java_file.read_text(encoding="utf-8")
    if "ReGlassApi.create(" in java_source:
        api_users.append(java_file.as_posix())

expected_user = "../PrivacyMod/src/client/java/de/leon/privacy/client/PrivacyScreen.java"
if api_users != [expected_user]:
    raise SystemExit(
        "ReGlass API must be used only in PrivacyScreen.java; found: "
        + ", ".join(api_users)
    )

privacy_screen = Path(expected_user)
if not privacy_screen.is_file() or "ReGlassApi.create(" not in privacy_screen.read_text(encoding="utf-8"):
    raise SystemExit("PrivacyScreen is not configured to use ReGlass.")

config.write_text(source, encoding="utf-8")
print("ReGlass rendering infrastructure retained; API usage verified as PrivacyScreen-only.")
