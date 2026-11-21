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

config.write_text(source, encoding="utf-8")
print("ReGlass vanilla UI mixins disabled; custom mod rendering retained.")
