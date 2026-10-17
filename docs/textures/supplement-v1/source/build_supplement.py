"""Export the twelve requested additions with editable sources and previews."""
from pathlib import Path
import csv
import hashlib
import json
import shutil
import subprocess
import sys
from PIL import Image, ImageDraw, ImageFont
import supplement_art as art

SCRIPT=Path(__file__).resolve()
ROOT=SCRIPT.parents[3] if SCRIPT.parent.name=='source' else SCRIPT.parents[1]
PACK=SCRIPT.parents[1] if SCRIPT.parent.name=='source' else ROOT/'outputs/elemental_verse_supplement_bottle_states'
ASSET=PACK/'assets/elemental_verse'
SOURCE=PACK/'source'
PREVIEW=PACK/'previews'
for path in [ASSET/'textures/item',ASSET/'textures/block',ASSET/'models/item',
             ASSET/'models/block',ASSET/'blockstates',SOURCE/'bases',SOURCE/'masks',PREVIEW]:
    path.mkdir(parents=True,exist_ok=True)

def jsonfile(path,value):path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
if SCRIPT.parent.name!='source':
    accepted=ROOT/'outputs/elemental_verse_textures_v7/assets/elemental_verse/textures/item'
    for name in ['refraction_prism','shadow_bottle']:
        shutil.copy2(accepted/f'{name}.png',SOURCE/'bases'/f'{name}.png')
prism=Image.open(SOURCE/'bases/refraction_prism.png').convert('RGBA')
bottle=Image.open(SOURCE/'bases/shadow_bottle.png').convert('RGBA')
images=art.draw_all(prism,bottle)
assert len(images)==12

INFO={
 'item/dead_dust':('死寂尘','浅灰色松散粉堆，周围少量断开的颗粒；比固体残片更浅、更碎'),
 'item/prism_light':('折射棱镜 · 光','沿用原三角棱镜轮廓，浅金内容与象牙白光核'),
 'item/prism_dawn':('折射棱镜 · 晨','沿用原三角棱镜轮廓，桃色日轮与灰粉霞层'),
 'item/bottle_shadow':('聚影瓶 · 暗','保留原紫晶瓶体；紫色雾团用连续大色块和单侧亮边表达悬浮'),
 'item/bottle_shadow_essence':('聚影瓶 · 影','保留原紫晶瓶体；蓝灰凝影使用完整沉积面和浅弯边，收敛零碎高光'),
 'item/element_pouch':('元素小包','沿用原轮廓与配色，细化卷边袋口、暗部开口、受力褶皱、抽绳结与侧缝'),
 'block/sealed_vault_side':('石制密封仓 · 侧面','厚石盖倒角、压紧夹片、连续密封缝与侧面石质层次'),
 'block/sealed_vault_top':('石制密封仓 · 顶面','切角石盖、密封槽、三色压片和自然石质切面'),
 'block/sealed_vault_front':('石制密封仓 · 正面','凹入石板门、跨缝铅条、八角压印铅封与盖沿夹片'),
 'block/rune_cabinet_side':('符文柜 · 侧面','分层深板岩嵌板、下沉线槽与铜质护角'),
 'block/rune_cabinet_top':('符文柜 · 顶面','内凹石板顶、双层框边与四角铜卡扣'),
 'block/rune_cabinet_front':('符文柜 · 正面','有厚度的单扇柜门、双铰链、右侧锁扣与嵌入式符文环'),
}
records=[]
for name,im in images.items():
    im.save(ASSET/'textures'/f'{name}.png')
    label,design=INFO[name]
    records.append({'name':name.split('/')[-1],'label':label,'kind':name.split('/')[0],
                    'size':[16,16],'texture':f'assets/elemental_verse/textures/{name}.png',
                    'design':design})
    if name.startswith('item/'):
        jsonfile(ASSET/'models'/f'{name}.json',{'parent':'minecraft:item/generated',
                 'textures':{'layer0':f'elemental_verse:{name}'}})

for name in ['sealed_vault','rune_cabinet']:
    jsonfile(ASSET/'models/block'/f'{name}.json',{
        'parent':'minecraft:block/orientable','textures':{
        'front':f'elemental_verse:block/{name}_front',
        'side':f'elemental_verse:block/{name}_side',
        'top':f'elemental_verse:block/{name}_top',
        'bottom':f'elemental_verse:block/{name}_top'}})
    jsonfile(ASSET/'models/item'/f'{name}.json',{'parent':f'elemental_verse:block/{name}'})
    jsonfile(ASSET/'blockstates'/f'{name}.json',{'variants':{
        f'facing={direction}':{'model':f'elemental_verse:block/{name}',
            **({'y':rotation} if rotation else {})}
        for direction,rotation in [('north',0),('east',90),('south',180),('west',270)]}})

def pixel_source(im):
    colors=sorted({p for p in im.get_flattened_data() if p[3]})
    symbols='0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    assert len(colors)<=len(symbols)
    symbols={color:symbols[i] for i,color in enumerate(colors)}
    return {'size':16,'palette':{symbols[c]:'#'+''.join(f'{v:02x}' for v in c[:3]) for c in colors},
            'pixels':[''.join(symbols.get(im.getpixel((x,y)),'.') for x in range(16)) for y in range(16)]}
jsonfile(SOURCE/'pixels.json',{'format':'elemental-verse-native-pixels-v1',
                             'sprites':{name:pixel_source(im) for name,im in images.items()}})
mask=art.charge_mask()
mask.save(SOURCE/'masks/rune_cabinet_charge.png')
ring,core=art.charge_pixels()
jsonfile(SOURCE/'rune_charge_region.json',{
    'texture':'elemental_verse:block/rune_cabinet_front','size':[16,16],
    'coordinates':'zero-based [x,y], top left origin','ring_pixels':ring,'core_pixels':core,
    'mask':'masks/rune_cabinet_charge.png',
    'note':'只在此区域调整储能亮度。PNG 与掩膜本身不会使方块发光；运行时切换、材质或客户端渲染由模组接入。'})

font_path='C:/Windows/Fonts/msyh.ttc'
def font(n):return ImageFont.truetype(font_path,n)
NN=Image.Resampling.NEAREST
def text_center(draw,x,y,text,n=16,color='#dbe6df'):
    f=font(n);draw.text((x-draw.textlength(text,font=f)/2,y),text,font=f,fill=color)

def overview():
    sheet=Image.new('RGB',(1060,1010),'#151d24');draw=ImageDraw.Draw(sheet)
    draw.text((28,22),'补充素材 · 聚影瓶内容细化',font=font(31),fill='#e4eae3')
    draw.text((28,73),'暗：悬浮紫雾 / 影：沉积凝影  ·  原生 16 × 16',font=font(16),fill='#9eb2b3')
    for i,(name,im) in enumerate(images.items()):
        x=28+i%4*258;y=125+i//4*280
        draw.rounded_rectangle((x,y,x+244,y+262),radius=8,fill='#24313a')
        area=Image.new('RGB',(160,160),'#334048')
        big=im.resize((160,160),NN);area.paste(big,(0,0),big)
        sheet.paste(area,(x+42,y+12))
        text_center(draw,x+122,y+187,INFO[name][0],17)
        text_center(draw,x+122,y+219,name.split('/')[-1],11,'#9aafb6')
        sheet.paste(im,(x+215,y+232),im)
    draw.text((28,976),'小样为原尺寸；大图为最近邻 ×10。符文柜亮度区域另附掩膜。',font=font(14),fill='#91aaac')
    sheet.save(PREVIEW/'new_textures_overview.png')

def container_states():
    sheet=Image.new('RGB',(1000,616),'#151d24');draw=ImageDraw.Draw(sheet)
    draw.text((28,20),'容器状态',font=font(29),fill='#e2e9e1')
    draw.text((28,66),'装入元素后保留各自的容器外形；下方是原尺寸小样。',font=font(16),fill='#9eb2b3')
    rows=[[(prism,'空棱镜'),(images['item/prism_light'],'光'),(images['item/prism_dawn'],'晨')],
          [(bottle,'空聚影瓶'),(images['item/bottle_shadow'],'暗'),
           (images['item/bottle_shadow_essence'],'影')]]
    for r,row in enumerate(rows):
        for c,(im,label) in enumerate(row):
            x=125+c*285;y=121+r*231
            draw.rectangle((x,y,x+143,y+143),fill='#303c45')
            big=im.resize((144,144),NN);sheet.paste(big,(x,y),big)
            text_center(draw,x+72,y+154,label,19)
            sheet.paste(im,(x+64,y+188),im)
    sheet.save(PREVIEW/'container_states.png')

def cube(draw,front,side,top,cx,cy,scale=5):
    def face(tex,origin,u,v,shade=1.0):
        for y in range(16):
            for x in range(16):
                color=tuple(round(c*shade) for c in tex.getpixel((x,y))[:3])
                pts=[(origin[0]+a*u[0]+b*v[0],origin[1]+a*u[1]+b*v[1])
                     for a,b in [(x,y),(x+1,y),(x+1,y+1),(x,y+1)]]
                draw.polygon(pts,fill=color)
    s=scale
    face(front,(cx-16*s,cy+8*s),(s,s/2),(0,s),.98)
    face(side,(cx+16*s,cy+8*s),(-s,s/2),(0,s),.76)
    face(top,(cx,cy),(s,s/2),(-s,s/2),1.0)

def blocks_preview():
    sheet=Image.new('RGB',(1100,680),'#151d24');draw=ImageDraw.Draw(sheet)
    draw.text((28,22),'密封仓与符文柜 · 细化版',font=font(29),fill='#e2e9e1')
    draw.text((28,68),'由三面贴图投影的外观示意；下方是正面的原生像素放大图。',font=font(16),fill='#9eb2b3')
    for i,(name,label) in enumerate([('sealed_vault','石制密封仓'),('rune_cabinet','符文柜')]):
        cx=280+i*545
        cube(draw,images[f'block/{name}_front'],images[f'block/{name}_side'],
             images[f'block/{name}_top'],cx,123,5)
        text_center(draw,cx,383,label,22)
        front=images[f'block/{name}_front'].resize((160,160),NN)
        sheet.paste(front,(cx-80,445),front)
    draw.text((28,642),'以倒角、接触阴影、夹片和门缝表现结构；储能区域保留原掩膜。',font=font(15),fill='#92aaac')
    sheet.save(PREVIEW/'storage_blocks.png')

def charge_preview():
    sheet=Image.new('RGB',(990,355),'#151d24');draw=ImageDraw.Draw(sheet)
    draw.text((28,20),'符文柜 · 储能亮度区域',font=font(27),fill='#e2e9e1')
    draw.text((28,65),'以下为亮度变化示意；柜体和铜框保持相同像素。',font=font(15),fill='#9eb2b3')
    for i,level in enumerate([0,.33,.66,1]):
        im=images['block/rune_cabinet_front'].copy()
        for pt in ring+core:
            rgb=im.getpixel(pt)[:3]
            dark=(48,63,67)
            im.putpixel(pt,tuple(round(dark[j]+level*(rgb[j]-dark[j])) for j in range(3))+(255,))
        sheet.paste(im.resize((160,160),NN),(42+i*239,115))
        text_center(draw,122+i*239,295,f'{round(level*100)}%',17)
    sheet.save(PREVIEW/'rune_charge_region.png')

def native_preview():
    sheet=Image.new('RGB',(1000,348),'#151d24');draw=ImageDraw.Draw(sheet)
    draw.text((28,20),'原尺寸检查',font=font(27),fill='#e2e9e1')
    draw.text((28,64),'左侧为 1:1，右侧为相同像素的 ×3 放大。',font=font(15),fill='#9eb2b3')
    for scale,x0,y0 in [(1,28,125),(3,330,116)]:
        for i,im in enumerate(images.values()):
            x=x0+i%6*18*scale;y=y0+i//6*18*scale
            draw.rectangle((x,y,x+18*scale-1,y+18*scale-1),fill='#8b8b8b')
            draw.line([(x,y+18*scale-1),(x,y),(x+18*scale-1,y)],fill='#373737',width=scale)
            draw.line([(x+scale,y+18*scale-1),(x+18*scale-1,y+18*scale-1),(x+18*scale-1,y+scale)],fill='#e8e8e8',width=scale)
            big=im.resize((16*scale,16*scale),NN);sheet.paste(big,(x+scale,y+scale),big)
    draw.text((28,294),'实际游戏内观感仍由客户端缩放、光照与材质设置决定。',font=font(15),fill='#92aaac')
    sheet.save(PREVIEW/'native_scale.png')

overview();container_states();blocks_preview();charge_preview();native_preview()
jsonfile(PACK/'texture_manifest.json',{'namespace':'elemental_verse','new_textures':12,
       'item_textures':6,'block_textures':6,'frame_size':[16,16],'textures':records})
with (PACK/'贴图清单.csv').open('w',encoding='utf-8-sig',newline='') as stream:
    writer=csv.writer(stream);writer.writerow(['中文名','类型','路径','尺寸','设计'])
    for r in records:writer.writerow([r['label'],r['kind'],r['texture'],'16x16',r['design']])

readme='''# 素材补充 · 聚影瓶内容细化

按 2026-10-17 的《素材需求清单》制作。本次仅调整 `bottle_shadow.png` 与 `bottle_shadow_essence.png` 的内容效果。暗为连续的紫色悬浮雾团，影为低位蓝灰凝影沉积面；减少零碎高光。空聚影瓶继续沿用原图，瓶塞、瓶颈、外轮廓与前景玻璃反光均保留。其余 10 张贴图沿用已细化的小包与仓储版本。素材为原生 16×16 RGBA，二值透明，无抗锯齿、平滑缩放和随机噪声。

## 新增素材

| 类型 | 文件 | 数量 |
| --- | --- | --- |
| 物品 | `dead_dust.png` | 1 |
| 折射棱镜内容状态 | `prism_light.png`、`prism_dawn.png` | 2 |
| 聚影瓶内容状态 | `bottle_shadow.png`、`bottle_shadow_essence.png` | 2 |
| 便携容器 | `element_pouch.png` | 1 |
| 石制密封仓 | `sealed_vault_side.png`、`sealed_vault_top.png`、`sealed_vault_front.png` | 3 |
| 符文柜 | `rune_cabinet_side.png`、`rune_cabinet_top.png`、`rune_cabinet_front.png` | 3 |

前六张位于 `assets/elemental_verse/textures/item/`，后六张位于 `assets/elemental_verse/textures/block/`。

死寂尘是浅色松散颗粒堆，与之前的死寂物质薄片图标是两份不同素材。棱镜和聚影瓶状态沿用已选定的容器轮廓。小包露出瓶口与浅蓝冷却剂，卷边袋口遮住瓶子下半部分；本轮细化：石仓增加石盖倒角、压紧夹片、连续密封槽、凹入的石板门和八角铅封压印。符文柜增加柜门厚度、铰链、锁扣、铜护角与内凹顶板。符文储能区域仍为原来的 25 个像素，掩膜文件与上一版完全一致。

## 接入

将 `assets/elemental_verse/` 按路径合并到模组的 `src/main/resources/`。本包只包含新增素材，配合前一版使用，不覆盖已有锥形元素瓶、空棱镜、空聚影瓶和死寂物质方块。

6 个新增物品模型使用传统 `minecraft:item/generated`。两个仓储方块附 `minecraft:block/orientable` 模型与四向 `facing` 方块状态示例。仅需三张纹理，底面复用顶面，背面复用侧面。

光/晨在棱镜中的模型映射为 `elemental_verse:item/prism_light` / `prism_dawn`；暗/影在聚影瓶中对应 `elemental_verse:item/bottle_shadow` / `bottle_shadow_essence`。按数据组件切换模型以及新版物品定义需按实际 MC 版本接入。

符文柜的储能区域掩膜见 `source/masks/rune_cabinet_charge.png`，坐标与说明见 `source/rune_charge_region.json`。只调整掩膜内像素即可表达亮度；普通 PNG 与该掩膜不会自动产生动态发光或改变光照，运行时逻辑由模组实现。预览里的 4 档亮度只是效果示意。

## 编辑与验证

`source/pixels.json` 是 12 张图的逐像素源，编辑后用 `source/render_pixels.py` 导出。`source/supplement_art.py` 是绘图逻辑，`source/build_supplement.py` 可重建原稿、模型和预览；需要 Python 和 Pillow。重建会覆盖字符源与最终贴图。容器底图保存在 `source/bases/`，只用于源文件重建。

遵循 https://blockbench.net/wiki/guides/minecraft-style-guide/ ，按用户提供的 `minecraft-compat-pixel-art` skill 以整数坐标绘制。本次实际查看过的参考包括 Ender IO 袋子、Create 箱体和农夫乐事柜门；只提取轮廓组织、材质色阶和结构方式，新增图标均重新绘制。参考路径见 `source/reference_sources.json`。

已检查尺寸、透明度、模型路径、像素源重建、棱镜与聚影瓶的轮廓一致性和符文掩膜边界，并实际查看原尺寸及放大预览。未运行 Minecraft 客户端。结果见 `validation.json`。
'''
(PACK/'README.md').write_text(readme,encoding='utf-8')
if SCRIPT.parent.name!='source':
    shutil.copy2(SCRIPT,SOURCE/'build_supplement.py')
    shutil.copy2(Path(art.__file__),SOURCE/'supplement_art.py')
    shutil.copy2(ROOT/'outputs/elemental_verse_textures_v7/source/render_pixels.py',SOURCE/'render_pixels.py')
    ref=ROOT/'work/supplement_reference_board.png.sources.json'
    if ref.exists():shutil.copy2(ref,SOURCE/'reference_sources.json')

checks=[]
for name,im in images.items():
    assert im.size==(16,16)
    alphas={p[3] for p in im.get_flattened_data()}
    assert alphas<={0,255} and 255 in alphas,name
    if name.startswith('block/'):assert alphas=={255},name
    path=ASSET/'textures'/f'{name}.png'
    checks.append({'texture':name,'size':[16,16],'alpha':sorted(alphas),
                   'colors':len({p for p in im.get_flattened_data() if p[3]}),
                   'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
for name,base in [('item/prism_light',prism),('item/prism_dawn',prism),
                  ('item/bottle_shadow',bottle),('item/bottle_shadow_essence',bottle)]:
    assert images[name].getchannel('A').tobytes()==base.getchannel('A').tobytes(),name
assert len(set(ring+core))==len(ring+core)
for pt in ring+core:assert images['block/rune_cabinet_front'].getpixel(pt)[3]==255
for path in (ASSET/'models').rglob('*.json'):
    data=json.loads(path.read_text(encoding='utf-8'))
    for value in data.get('textures',{}).values():
        assert (ASSET/'textures'/f'{value.split(":",1)[1]}.png').is_file(),(path,value)
jsonfile(PACK/'validation.json',{'new_texture_count':12,'native_16x16':True,'binary_alpha':True,
           'block_faces_opaque':True,'container_silhouettes_preserved':True,
           'local_model_textures_resolve':True,'charge_mask_pixel_count':len(ring+core),
           'in_game_checked':False,'files':checks})
print(f'Generated 12 native textures, model examples, editable sources and five previews: {PACK}')
