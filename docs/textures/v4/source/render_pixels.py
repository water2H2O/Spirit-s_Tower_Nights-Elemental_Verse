"""Export edited pixels.json to the asset PNGs. Requires Pillow."""
import json
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
sources=json.loads((root/'source/pixels.json').read_text(encoding='utf-8'))['sprites']
pending=[]
for name,src in sources.items():
    assert len(src['pixels'])==16 and all(len(row)==16 for row in src['pixels']), name
    assert name.startswith(('item/','block/')) and '..' not in name
    im=Image.new('RGBA',(16,16))
    for y,row in enumerate(src['pixels']):
        for x,c in enumerate(row):
            if c!='.':
                value=src['palette'][c].lstrip('#')
                im.putpixel((x,y),tuple(bytes.fromhex(value))+(255,))
    pending.append((root/'assets/elemental_verse/textures'/f'{name}.png',im))
for path,im in pending:
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path)
print(f'Exported {len(pending)} native 16x16 textures.')
