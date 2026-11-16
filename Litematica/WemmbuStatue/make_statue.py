#!/usr/bin/env python3
"""
Generate a 75-block-tall Wemmbu-inspired Unstable SMP statue.
Requires: pip install litemapy
Run: python make_statue.py
Output: Wemmbu_Mace_Shield_75.litematic

Note: the shield emblem is an original purple/cyan-inspired pattern, not an
official extracted Wemmbu asset. Armor uses vanilla Netherite block textures.
"""
from litemapy import Schematic, Region, BlockState

NAME = "Wemmbu - Mace & Shield"
AUTHOR = "Generated build"
W, H, D = 39, 75, 25
blocks = {}

def put(x, y, z, block):
    if 0 <= x < W and 0 <= y < H and 0 <= z < D:
        blocks[(x, y, z)] = block

def box(x1,y1,z1,x2,y2,z2,block):
    for x in range(x1,x2+1):
        for y in range(y1,y2+1):
            for z in range(z1,z2+1):
                put(x,y,z,block)

def ellipsoid(cx,cy,cz,rx,ry,rz,block):
    for x in range(cx-rx,cx+rx+1):
        for y in range(cy-ry,cy+ry+1):
            for z in range(cz-rz,cz+rz+1):
                if ((x-cx)/rx)**2 + ((y-cy)/ry)**2 + ((z-cz)/rz)**2 <= 1:
                    put(x,y,z,block)

def line(a,b,block,thickness=1):
    x1,y1,z1=a; x2,y2,z2=b
    n=max(abs(x2-x1),abs(y2-y1),abs(z2-z1),1)
    for i in range(n+1):
        x=round(x1+(x2-x1)*i/n); y=round(y1+(y2-y1)*i/n); z=round(z1+(z2-z1)*i/n)
        for dx in range(-thickness//2, thickness//2+1):
            for dy in range(-thickness//2, thickness//2+1):
                put(x+dx,y+dy,z,block)

# Palette: vanilla blocks only, preserving the original Minecraft look.
netherite = "minecraft:polished_blackstone"
netherite_hi = "minecraft:crying_obsidian"
dark = "minecraft:deepslate_tiles"
purple = "minecraft:purpur_block"
cyan = "minecraft:prismarine_bricks"
silver = "minecraft:iron_block"
gold = "minecraft:gold_block"
stone = "minecraft:polished_deepslate"
base = "minecraft:polished_blackstone_bricks"

# Pedestal and title-like accent bands.
box(5,0,4,33,2,20,base)
box(7,3,6,31,3,18,stone)
box(9,4,8,29,4,16,purple)

# Legs: armored, slightly separated stance.
box(13,5,9,18,24,15,netherite)
box(20,5,9,25,24,15,netherite)
box(12,5,8,19,8,16,dark)
box(19,5,8,26,8,16,dark)
# Knee and boot highlights.
box(13,13,9,18,15,15,netherite_hi)
box(20,13,9,25,15,15,netherite_hi)
box(12,5,8,19,7,16,silver)
box(19,5,8,26,7,16,silver)

# Torso and chestplate.
box(11,25,8,27,44,16,netherite)
box(10,27,7,28,42,17,dark)
box(12,29,6,26,40,7,netherite_hi)
box(12,29,17,26,40,18,netherite_hi)
box(14,34,6,24,39,7,silver)
box(17,30,6,21,34,7,purple)

# Shoulder armor and arms.
box(7,40,8,12,46,16,netherite_hi)
box(26,40,8,31,46,16,netherite_hi)
box(8,29,9,12,40,15,netherite)
box(27,29,9,30,40,15,netherite)
box(8,25,9,12,29,15,dark)
box(27,25,9,30,29,15,dark)

# Neck and fully Netherite helmet (no Elytra).
box(16,44,9,22,48,15,dark)
box(12,48,7,26,61,17,netherite)
box(11,50,6,27,59,18,netherite_hi)
box(13,48,5,25,61,6,dark)
box(13,48,18,25,61,19,dark)
# Face slit and helmet rim.
box(15,52,5,23,55,5,cyan)
box(12,48,7,26,49,17,silver)
box(12,60,7,26,61,17,silver)
# Helmet crest: dark netherite ridge, not cyan.
box(17,62,9,21,64,15,netherite_hi)

# Shield held on the viewer's left: broad vanilla-style shield silhouette.
# The patterned front faces toward the camera (z=4).
box(1,20,3,8,39,4,dark)
box(2,19,3,7,20,4,purple)
box(2,39,3,7,40,4,purple)
box(0,22,3,1,37,4,purple)
box(8,22,3,9,37,4,purple)
# Purple field and cyan geometric emblem.
box(2,22,2,7,37,2,purple)
line((3,34,1),(6,25,1),cyan,2)
line((6,34,1),(3,25,1),cyan,2)
box(4,28,1,5,31,1,silver)
box(3,21,2,6,22,2,cyan)

# Right-hand mace: long handle and oversized 3D cubic head.
line((29,28,11),(35,49,11),dark,3)
line((29,28,10),(35,49,10),silver,1)
# mace head, a chunky 3D cube with stepped metallic/cyan details.
box(31,48,7,38,56,15,stone)
box(32,49,6,37,55,16,netherite_hi)
box(33,50,5,36,54,17,silver)
box(34,51,4,35,53,18,cyan)
box(33,49,7,36,55,15,purple)
# top cap and lower grip.
box(33,56,8,36,57,14,netherite)
box(28,26,9,30,30,13,purple)

# Small decorative details on armor, kept in Minecraft block language.
for y in (27,31,35,39):
    put(11,y,12,purple); put(27,y,12,purple)
box(16,25,6,22,27,7,gold)

# Build the Litematica schematic.
region = Region(0, 0, 0, W, H, D)
for (x,y,z), block_name in blocks.items():
    region.setblock(x, y, z, BlockState(block_name))

schem = Schematic(name=NAME, author=AUTHOR,
                  description="75-block Netherite-armored statue with shield and 3D mace; no Elytra.")
schem.add_region("Wemmbu_Statue", region)
schem.save("Wemmbu_Mace_Shield_75.litematic")
print(f"Saved {len(blocks)} placed blocks to Wemmbu_Mace_Shield_75.litematic")
