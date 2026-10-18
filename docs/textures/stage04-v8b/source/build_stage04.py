"""Export six Stage 04 textures and their precise UI/tint contracts."""
from pathlib import Path
import csv
import hashlib
import json
import shutil
import sys
from PIL import Image,ImageDraw,ImageFont
import stage04_art as art

SCRIPT=Path(__file__).resolve()
ROOT=SCRIPT.parents[3] if SCRIPT.parent.name=='source' else SCRIPT.parents[1]
PACK=SCRIPT.parents[1] if SCRIPT.parent.name=='source' else ROOT/'outputs/elemental_verse_stage04_v8_altar_overall_refined'
ASSET=PACK/'assets/elemental_verse'
SOURCE=PACK/'source'
PREVIEW=PACK/'previews'
for folder in [ASSET/'textures/item',ASSET/'textures/block',ASSET/'textures/gui',
               SOURCE/'masks',SOURCE/'preview_samples',PREVIEW]:folder.mkdir(parents=True,exist_ok=True)
def jsonfile(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
images=art.draw_all()
INFO={
 'item/declaration_plate':('空申明板','切角石板、加厚底边、两层倒角与大块石质层理，无刻字'),
 'item/declaration_plate_written':('已写申明板','沿用空板轮廓，刻纹加深与高光槽唇，中央仍为 4×4 中性染色区'),
 'block/ritual_altar_side':('仪式台 · 侧面','斜切凹入面板、银弧分面、紫晶镶片与金色空心符印'),
 'block/ritual_altar_top':('仪式台 · 顶面','紫晶台面倒角、银质导槽与晶扣、三个投入口及双侧金印'),
 'block/ritual_altar_front':('仪式台 · 正面','重新分面的紫晶、带卡爪的银托、内凹台身与分层底座'),
 'gui/ritual_altar':('仪式台界面','176×166；内凹工作面、角片与分隔压线；槽位及文字留白坐标不变')}
records=[]
for name,im in images.items():
    path=ASSET/'textures'/f'{name}.png';im.save(path)
    label,design=INFO[name]
    records.append({'name':name.split('/')[-1],'kind':name.split('/')[0],
                    'label':label,'size':list(im.size),
                    'texture':f'assets/elemental_verse/textures/{name}.png','design':design})

mask=art.declaration_tint_mask();mask.save(SOURCE/'masks/declaration_plate_tint.png')
jsonfile(SOURCE/'declaration_tint_region.json',{
    'texture':'elemental_verse:item/declaration_plate_written','canvas':[16,16],
    'coordinates':'zero-based [x,y], origin at top left','rectangle_xywh':[6,6,4,4],
    'pixels':art.TINT_PIXELS,'mask':'masks/declaration_plate_tint.png',
    'operation':'multiply each RGB channel by the selected element color; preserve alpha',
    'note':'只染此区域；直接对整张物品模型染色会把石板本身也染色。如使用分层物品模型，将掩膜区域拆成独立 layer 再设置 tintindex。未生成版本相关模型。'})

def slotrecord(kind,xy,index):return {'kind':kind,'index':index,'content_xy':list(xy),
       'content_size':[16,16],'frame_xywh':[xy[0]-1,xy[1]-1,18,18]}
slots=[slotrecord('element',xy,i) for i,xy in enumerate(art.ELEMENT_SLOTS)]
slots += [slotrecord('declaration',art.PLATE_SLOT,0),slotrecord('output',art.OUTPUT_SLOT,0)]
slots += [slotrecord('player_inventory',xy,i) for i,xy in enumerate(art.INVENTORY_SLOTS)]
slots += [slotrecord('hotbar',xy,i) for i,xy in enumerate(art.HOTBAR_SLOTS)]
jsonfile(SOURCE/'ritual_altar_gui_layout.json',{
    'texture':'elemental_verse:gui/ritual_altar','texture_size':[176,166],
    'screen_size':[176,166],'uv_source_xy':[0,0],
    'coordinates':'zero-based GUI-relative [x,y]; slot coordinates are 16x16 CONTENT origins',
    'slots':slots,'blank_text_regions_xywh':art.GUI_TEXT_REGIONS,
    'recommended_text_color':'#404b44','text_baked_into_texture':False,
    'player_inventory_assumption':'27 slots start at (8,84), 18px spacing; hotbar starts at (8,142)',
    'note':'原生图集尺寸是 176×166。blit/绘制时纹理尺寸参数也需传 176,166，不能继续默认 256,256。'})
jsonfile(SOURCE/'ritual_altar_top_layout.json',{
    'texture':'elemental_verse:block/ritual_altar_top','canvas':[16,16],
    'coordinates':'zero-based top-face [x,y], north/rear at y=0',
    'declaration_recess_xywh':art.ALTAR_PLATE_RECT,'element_pits_xywh':art.ALTAR_PITS,
    'orientation':'前方对应顶面 y=15；三个元素口为后侧左、后侧右、前侧中。',
    'note':'这是像素视觉分区；实际交互/命中区域由模组代码决定。'})

# Exact pixel sources support both 16x16 art and the native 176x166 interface.
def pixel_source(im):
    colors=sorted({p for p in im.get_flattened_data() if p[3]})
    alphabet='0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    assert len(colors)<=len(alphabet)
    lookup={c:alphabet[i] for i,c in enumerate(colors)}
    return {'width':im.width,'height':im.height,
        'palette':{lookup[c]:'#'+''.join(f'{v:02x}' for v in c[:3]) for c in colors},
        'pixels':[''.join(lookup.get(im.getpixel((x,y)),'.') for x in range(im.width)) for y in range(im.height)]}
jsonfile(SOURCE/'pixels.json',{'format':'elemental-verse-native-pixels-v2',
    'sprites':{name:pixel_source(im) for name,im in images.items()}})

FONT='C:/Windows/Fonts/msyh.ttc'
def font(n):return ImageFont.truetype(FONT,n)
NN=Image.Resampling.NEAREST
def label(draw,cx,y,text,n=17,color='#dce5dc'):
    f=font(n);draw.text((cx-draw.textlength(text,font=f)/2,y),text,font=f,fill=color)
def tint(im,color):
    result=im.copy()
    rgb=tuple(bytes.fromhex(color.lstrip('#')))
    for xy in art.TINT_PIXELS:
        p=im.getpixel(xy);result.putpixel(xy,tuple(round(p[i]*rgb[i]/255) for i in range(3))+(p[3],))
    return result

def cube(draw,cx,cy,scale=5):
    def face(im,origin,u,v,shade):
        for y in range(16):
            for x in range(16):
                col=tuple(round(c*shade) for c in im.getpixel((x,y))[:3])
                pts=[(origin[0]+a*u[0]+b*v[0],origin[1]+a*u[1]+b*v[1])
                     for a,b in [(x,y),(x+1,y),(x+1,y+1),(x,y+1)]]
                draw.polygon(pts,fill=col)
    s=scale
    face(images['block/ritual_altar_front'],(cx-16*s,cy+8*s),(s,s/2),(0,s),.98)
    face(images['block/ritual_altar_side'],(cx+16*s,cy+8*s),(-s,s/2),(0,s),.76)
    face(images['block/ritual_altar_top'],(cx,cy),(s,s/2),(-s,s/2),1.0)

def item_board():
    sheet=Image.new('RGB',(1100,400),'#151d24');dr=ImageDraw.Draw(sheet)
    dr.text((28,20),'申明板 · 空白、刻写与染色',font=font(29),fill='#e2e8e1')
    dr.text((28,67),'石板轮廓一致；只有中央 4 × 4 区域参与元素染色。',font=font(16),fill='#9db3b4')
    written=images['item/declaration_plate_written']
    samples=[(images['item/declaration_plate'],'空申明板'),(written,'已写 · 中性底版'),
             (tint(written,'#65afe0'),'水 · 示例'),(tint(written,'#e28860'),'火 · 示例'),
             (tint(written,'#f2d78d'),'光 · 示例')]
    for i,(im,title) in enumerate(samples):
        x=28+i*214
        dr.rounded_rectangle((x,111,x+199,363),radius=7,fill='#26333c')
        dr.rectangle((x+20,125,x+179,284),fill='#35424a')
        big=im.resize((160,160),NN);sheet.paste(big,(x+20,125),big)
        label(dr,x+100,301,title,16)
        sheet.paste(im,(x+92,335),im)
    sheet.save(PREVIEW/'declaration_plates.png')

def altar_board():
    sheet=Image.new('RGB',(1080,800),'#151d24');dr=ImageDraw.Draw(sheet)
    dr.text((28,20),'仪式台 · 整体材质与结构细化',font=font(29),fill='#e2e8e1')
    dr.text((28,69),'中间长槽放申明板；后侧两个与前侧一个小坑为元素投入口。',font=font(16),fill='#9db3b4')
    for i,name in enumerate(['side','top','front']):
        im=images[f'block/ritual_altar_{name}'];x=66+i*345
        sheet.paste(im.resize((240,240),NN),(x,120))
        label(dr,x+120,380,{'side':'侧面','top':'顶面','front':'正面'}[name],20)
    cube(dr,310,460,7)
    top=images['block/ritual_altar_top'].resize((240,240),NN)
    sheet.paste(top,(680,465))
    # Coordinate overlays are confined to this review image, never the texture.
    for x,y,w,h in [art.ALTAR_PLATE_RECT]+art.ALTAR_PITS:
        dr.rectangle((680+x*15,465+y*15,680+(x+w)*15-1,465+(y+h)*15-1),outline='#cfb77d',width=2)
    label(dr,310,720,'实际贴图的方块投影示意',16,'#99b0b1')
    label(dr,800,720,'槽位视觉分区示意',16,'#99b0b1')
    sheet.save(PREVIEW/'ritual_altar_faces.png')

def gui_board():
    gui=images['gui/ritual_altar']
    sheet=Image.new('RGB',(1080,775),'#151d24');dr=ImageDraw.Draw(sheet)
    dr.text((28,20),'仪式台界面 · 176 × 166',font=font(29),fill='#e2e8e1')
    dr.text((28,69),'导出底图不带文字和物品。槽位坐标对应 16 × 16 内容区域的左上角。',font=font(16),fill='#9db3b4')
    sheet.paste(gui,(45,146),gui)
    label(dr,133,330,'原尺寸 1:1',17)
    sheet.paste(gui.resize((528,498),NN),(480,126))
    label(dr,744,645,'底图 ×3 · 精确槽位',17)
    # A separate populated illustration confirms icons fit inside the frames.
    sample=gui.copy();sd=ImageDraw.Draw(sample)
    sd.text((8,4),'仪式台',font=font(9),fill='#404b44')
    sd.text((8,58),'形：释放 · 发射',font=font(9),fill='#68726a')
    sd.text((8,71),'背包',font=font(9),fill='#404b44')
    names=['element_vial_water','element_vial_fire','element_vial_earth']
    if SCRIPT.parent.name!='source':
        base=ROOT/'outputs/elemental_verse_textures_v7/assets/elemental_verse/textures/item'
        for name in names:shutil.copy2(base/f'{name}.png',SOURCE/'preview_samples'/f'{name}.png')
    for xy,name in zip(art.ELEMENT_SLOTS,names):
        im=Image.open(SOURCE/'preview_samples'/f'{name}.png').convert('RGBA');sample.paste(im,xy,im)
    plate=tint(images['item/declaration_plate_written'],'#65afe0')
    sample.paste(plate,art.PLATE_SLOT,plate)
    sample.paste(plate,art.OUTPUT_SLOT,plate)
    sheet.paste(sample.resize((352,332),NN),(45,385))
    label(dr,221,735,'文字与物品排布示例 · 未烘焙进底图',13,'#99b0b1')
    dr.text((482,690),'背包内容起点 (8,84)，快捷栏起点 (8,142)。',font=font(14),fill='#99b0b1')
    sheet.save(PREVIEW/'ritual_altar_gui.png')

def overview():
    sheet=Image.new('RGB',(1140,1010),'#151d24');dr=ImageDraw.Draw(sheet)
    dr.text((28,20),'阶段 04 · 仪式台整体细化版',font=font(31),fill='#e2e8e1')
    dr.text((28,71),'台沿厚度 / 紫晶切面 / 银托分面 / 台身与底座 · 原生像素绘制',font=font(17),fill='#9db3b4')
    names=list(images)[:5]
    for i,name in enumerate(names):
        x=28+i*222
        dr.rounded_rectangle((x,123,x+208,384),radius=8,fill='#25323b')
        dr.rectangle((x+24,137,x+183,296),fill='#35424a')
        im=images[name];big=im.resize((160,160),NN);sheet.paste(big,(x+24,137),big)
        label(dr,x+104,312,INFO[name][0],17)
        sheet.paste(im,(x+96,348),im)
    cube(dr,260,460,8)
    label(dr,260,745,'仪式台 · 外观示意',20)
    gui=images['gui/ritual_altar'].resize((528,498),NN)
    sheet.paste(gui,(565,440),gui)
    label(dr,829,956,'界面底图 · 176 × 166 · 最近邻 ×3',16,'#99b0b1')
    dr.text((28,944),'物品与方块：16 × 16',font=font(16),fill='#99b0b1')
    dr.text((28,972),'已写板附中性染色区域、掩膜与坐标。',font=font(15),fill='#99b0b1')
    sheet.save(PREVIEW/'stage04_overview.png')

item_board();altar_board();gui_board();overview()
jsonfile(PACK/'texture_manifest.json',{'namespace':'elemental_verse','version_label':'v8',
       'texture_count':6,'audio_included':False,'textures':records})
with (PACK/'贴图清单.csv').open('w',encoding='utf-8-sig',newline='') as stream:
    writer=csv.writer(stream);writer.writerow(['中文名','类型','路径','尺寸','设计'])
    for r in records:writer.writerow([r['label'],r['kind'],r['texture'],f'{r["size"][0]}x{r["size"][1]}',r['design']])
readme='# 阶段 04 · 仪式台整体细化版 · v8\n\n本版整体细化深紫、紫晶、银饰与金印的仪式台三张方块贴图。台沿增加上下分面和接触阴影，台身的凹框改为斜切边，底座与包脚明确分层；正面紫晶重新安排连续的亮面、主面和暗面，银托补出卡爪与暗侧，侧面紫晶镶片和银弧也采用同一光照。顶面重新整理晶扣、导槽、紫色倒角与银边的明暗。金色符印的位置、色板与像素形状保留在上一版的基础上。所有方块贴图均在原生 16×16 网格内绘制，阴影和细节用于表现材质与厚度。\n\n## 内容\n\n- `assets/elemental_verse/textures/item/declaration_plate.png`：空申明板，16×16。\n- `assets/elemental_verse/textures/item/declaration_plate_written.png`：已写申明板，16×16。\n- `assets/elemental_verse/textures/block/ritual_altar_side.png`：仪式台侧面，16×16。\n- `assets/elemental_verse/textures/block/ritual_altar_top.png`：仪式台顶面，16×16。\n- `assets/elemental_verse/textures/block/ritual_altar_front.png`：仪式台正面，16×16。\n- `assets/elemental_verse/textures/gui/ritual_altar.png`：仪式台界面，原生 **176×166**，不是 256×256。\n\n全部为 RGBA PNG，透明度仅 0/255，无抗锯齿、平滑缩放或随机噪声。物品和方块直接在 16×16 网格绘制；GUI 在原生 176×166 画布按像素绘制。\n\n## 已写板染色\n\n已写板采用通用刻纹底版，中央 `[x=6,y=6,width=4,height=4]` 的 16 像素为中性灰，仅此区域参与元素染色。掩膜为 `source/masks/declaration_plate_tint.png`，完整坐标和 RGB 乘色说明见 `source/declaration_tint_region.json`。\n\nPNG 本身不会自动染色。代码应只处理指定区域；如果使用分层物品模型，把中央区域拆出为独立 layer 再设置 tintindex，避免将整个石板染色。没有生成 17 张元素版本。\n\n## 顶面\n\n深紫晶台面以窄银边和紫色倒角收口，中央板槽及三个投入口采用深色内壁、灰银下缘。银色嵌线绕开投入口。中央长槽放申明板，后侧左/右和前侧中各一个小坑接收元素。顶面前方为 y=15，后方为 y=0；分区见 `source/ritual_altar_top_layout.json`。图中的槽为视觉凹陷，未改变方块几何或命中检测。\n\n## GUI 坐标\n\n元素内容原点 `(62,17)`、`(80,17)`、`(98,17)`；申明板 `(62,35)`；产出 `(98,35)`。所有槽内容为 16×16，对应凹框从 `(x-1,y-1)` 开始，大小 18×18。\n\n按 176×166 标准容器布局绘制 27 格背包：起点 `(8,84)`、间距 18；快捷栏起点 `(8,142)`、间距 18。这是清单中“背包与原版箱子一致”的具体布局假设。\n\n标题留白 `[8,5,160,9]`；“形：释放 · 发射”留白 `[8,58,160,12]`；背包标签留白 `[8,72,160,9]`。最终 PNG 不含任何文字、物品、数字或槽内图标。预览中有一张填充示例，示例内容不会进入游戏贴图。\n\n**绘制时纹理宽高参数须使用 176、166**；不能继续按默认 256、256 的 UV 图集采样。所有 41 个槽框与留白坐标见 `source/ritual_altar_gui_layout.json`。\n\n## 接入与编辑\n\nZIP 内以 `assets/elemental_verse/` 为根资源路径，方便按项目已有集成工具导入。本包只交贴图，不包含会覆盖项目关键文件的模型、方块状态、分发或语言 JSON；未执行清单中的仓库集成命令。\n\n`source/pixels.json` 保存所有原生像素网格，包含 GUI。编辑后用 `source/render_pixels.py` 导出，依赖 Python 与 Pillow。`source/stage04_art.py` 为绘图源，`source/build_stage04.py` 可重建原稿、坐标与预览；重建会覆盖字符源与 PNG。\n\n仪式台按选择改为深紫晶体与银饰；布局参考先前认可的收纳方块，紫晶切面参考查看过的新生魔艺资源。其余素材参考农夫乐事容器界面的像素槽框，以及新生魔艺装置和通用机械平板的材质分层方式。实际查看的参考路径见 `source/reference_sources.json`。未复制参考 PNG；代码以整数坐标重新绘制，遵循 https://blockbench.net/wiki/guides/minecraft-style-guide/ 和用户提供的 `minecraft-compat-pixel-art` skill。\n\n验证范围为 PNG 尺寸、二值透明度、染色掩膜、全部槽内容与凹框的精确位置、文字区域留白、源文件可重建性和实际视觉预览。未运行 Minecraft 客户端，也未测试目标仓库的导入脚本。\n'
(PACK/'README.md').write_text(readme,encoding='utf-8')
renderer='''"""Render editable 16px sprites and the native 176x166 GUI. Requires Pillow."""
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
'''
(SOURCE/'render_pixels.py').write_text(renderer,encoding='utf-8')
if SCRIPT.parent.name!='source':
    shutil.copy2(SCRIPT,SOURCE/'build_stage04.py')
    shutil.copy2(Path(art.__file__),SOURCE/'stage04_art.py')
    ref=ROOT/'work/stage04_reference_board.png.sources.json'
    if ref.exists():shutil.copy2(ref,SOURCE/'reference_sources.json')
checks=[]
for name,im in images.items():
    expected=(176,166) if name.startswith('gui/') else (16,16)
    assert im.size==expected
    alphas={p[3] for p in im.get_flattened_data()};assert alphas<={0,255} and 255 in alphas
    if name.startswith('block/'):assert alphas=={255}
    path=ASSET/'textures'/f'{name}.png'
    checks.append({'texture':name,'size':list(im.size),'alpha':sorted(alphas),
                   'colors':len({p for p in im.get_flattened_data() if p[3]}),
                   'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
jsonfile(PACK/'validation.json',{'texture_count':6,'native_sizes':True,'binary_alpha':True,
    'gui_size':[176,166],'gui_slot_count':len(slots),'audio_included':False,
    'in_game_checked':False,'repository_integration_checked':False,'files':checks})
print(f'Generated six Stage 04 textures, tint/UI contracts and four previews: {PACK}')
