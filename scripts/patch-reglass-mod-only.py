#!/usr/bin/env python3
"""Limit the bundled ReGlass build to its custom rendering API/pipeline.

Keep the render pipeline and accessors required by ReGlassApi, but do not load
mixins that restyle vanilla Minecraft screens, HUD elements, buttons or sliders.
"""
from pathlib import Path

config = Path("src/main/resources/reglass.mixins.json")
source = config.read_text(encoding="utf-8")

# ReGlass' global slider mixin changes vanilla slider widgets.
source = source.replace('    "widgets.SliderWidgetMixin"\n', "")

start = source.index('  "client": [')
end = source.index('  ],', start) + len('  ],')
replacement = '''  "client": [
    "accessor.DrawContextAccessor",
    "accessor.GameRendererAccessor",
    "accessor.ScissorStackAccessor",
    "logical.GameRendererMixin",
    "widgets.DrawContextMixin",
    "widgets.GuiRendererMixin"
  ]'''
source = source[:start] + replacement + source[end:]

if '"client.ScreenMixin"' in source or '"client.InventoryScreenMixin"' in source:
    raise SystemExit("Vanilla screen mixins are still present; refusing to build.")

config.write_text(source, encoding="utf-8")
print("ReGlass restricted to custom mod-rendered glass elements.")
