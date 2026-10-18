"""Render editable 16px sprites and the native 176x166 GUI. Requires Pillow."""
import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'source/pixels.json').read_text(encoding='utf-8'))['sprites']
pending=[]
for name,src in data.items():
    assert name.startswith(('item/','block/','gui/')) and '..' not in name
    w,h=src['width'],src['height']
    assert (w,h) in [(16,16),(176,166)]
    rows=src['pixels'];assert len(rows)==h and all(len(row)==w for row in rows)
    image=Image.new('RGBA',(w,h))
    for y,row in enumerate(rows):
        for x,token in enumerate(row):
            if token!='.':image.putpixel((x,y),tuple(bytes.fromhex(src['palette'][token][1:]))+(255,))
    pending.append((root/'assets/elemental_verse/textures'/f'{name}.png',image))
for path,image in pending:path.parent.mkdir(parents=True,exist_ok=True);image.save(path)
print(f'Exported {len(pending)} native textures.')
