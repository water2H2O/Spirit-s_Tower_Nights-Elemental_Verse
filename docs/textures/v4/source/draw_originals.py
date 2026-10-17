"""Native 16px Elemental Verse art. No resampling during drawing."""
from __future__ import annotations
import csv
import hashlib
import json
import math
import shutil
import zipfile
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

SCRIPT = Path(__file__).resolve()
if SCRIPT.parent.name == 'source':
    OUT = SCRIPT.parents[1]
    ROOT = OUT.parents[1]
else:
    ROOT = SCRIPT.parents[1]
    OUT = ROOT / 'outputs' / 'elemental_verse_textures_v4'
ASSET = OUT / 'assets' / 'elemental_verse'
PREVIEW = OUT / 'previews'
SOURCE = OUT / 'source'
for folder in [ASSET / 'textures/item', ASSET / 'textures/block', ASSET / 'models/item',
               ASSET / 'models/block', ASSET / 'blockstates', PREVIEW, SOURCE]:
    folder.mkdir(parents=True, exist_ok=True)

NN = Image.Resampling.NEAREST
FONT = 'C:/Windows/Fonts/msyh.ttc'
def font(n): return ImageFont.truetype(FONT, n)
def rgba(s): return tuple(bytes.fromhex(s.lstrip('#'))) + (255,)
def canvas(): return Image.new('RGBA', (16, 16))
def rect(im, box, col): ImageDraw.Draw(im).rectangle(box, fill=col)
def line(im, points, col): ImageDraw.Draw(im).line(points, fill=col, width=1)
def poly(im, points, col): ImageDraw.Draw(im).polygon(points, fill=col)
def dots(im, points, col):
    for point in points: im.putpixel(point, rgba(col) if isinstance(col,str) else col)
def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Three glass colors. One clean outline and one short reflected highlight.
G = {'edge':'#617782','mid':'#92adb6','light':'#c7dddc'}

def glass_base():
    return canvas()

def glass_overlay():
    im=canvas()
    # Plain glass stopper and long hollow neck. No alternating colored rings.
    rect(im,(7,0,8,1),G['mid'])
    rect(im,(6,2,9,2),G['mid'])
    line(im,[(6,3),(6,6)],G['mid'])
    line(im,[(9,3),(9,6)],G['edge'])
    line(im,[(7,4),(8,4)],G['edge'])
    # The single graduation and bulb preserve the volumetric-flask identity.
    dots(im,[(5,7),(4,8)],G['mid'])
    dots(im,[(10,7),(11,8)],G['edge'])
    line(im,[(3,9),(3,12)],G['mid'])
    line(im,[(12,9),(12,12)],G['edge'])
    dots(im,[(4,13),(5,14),(6,14),(7,14),(8,14),(9,14),(10,14),(11,13)],G['edge'])
    line(im,[(4,9),(4,10)],G['light'])
    line(im,[(5,13),(7,13)],G['mid'])
    return im

def vial(content=None):
    im=glass_base()
    if content: content(im)
    im.alpha_composite(glass_overlay())
    return im

# Original first-version contents, composed under the second-version glass.
def earth(im):
    poly(im,[(5,11),(6,9),(8,9),(9,10),(10,10),(11,12),(10,13),(5,13)],'#624435')
    poly(im,[(5,11),(6,9),(8,9),(8,11),(7,12),(5,12)],'#ae8653')
    line(im,[(6,10),(7,10)],'#d3ae70')
    rect(im,(8,11,10,12),'#88643e')
    dots(im,[(8,9),(9,12)],'#c1975c')
    line(im,[(6,13),(9,13)],'#473d35')

def water(im):
    poly(im,[(4,10),(6,10),(7,9),(9,9),(10,10),(11,10),(11,12),(10,13),(5,13),(4,12)],'#245b9c')
    poly(im,[(4,10),(6,10),(7,9),(9,9),(10,10),(9,11),(6,11),(5,12),(4,12)],'#429ac8')
    line(im,[(5,10),(6,10),(7,9),(8,9)],'#b0e4e5')
    line(im,[(6,12),(8,12),(9,11)],'#2a7fb8')
    dots(im,[(6,13),(9,12)],'#3175ae')

def wind(im):
    line(im,[(5,9),(6,8),(9,8),(10,9),(10,10),(9,11),(6,11),(5,12),(6,13),(9,13)],'#4b8f8d')
    line(im,[(5,9),(6,8),(8,8)],'#e2f3de')
    line(im,[(7,9),(9,9),(9,10),(6,10),(5,11),(5,12),(7,12),(9,12)],'#a4d7bd')
    dots(im,[(8,11),(10,12)],'#d8e9d1')

def fire(im):
    poly(im,[(5,12),(5,10),(6,11),(7,9),(7,7),(9,9),(9,10),(10,9),(11,12),(9,13),(7,13)],'#a3372e')
    poly(im,[(6,12),(7,10),(7,9),(8,8),(9,10),(10,11),(9,13),(7,13)],'#ec7436')
    poly(im,[(7,12),(8,10),(9,11),(9,12),(8,13)],'#ffd277')
    dots(im,[(8,11),(8,12)],'#fff0b5')

def grass(im):
    line(im,[(7,13),(7,10),(8,8)],'#487b3f')
    poly(im,[(7,10),(5,9),(5,8),(7,8),(8,9)],'#82b956')
    poly(im,[(7,12),(8,10),(10,9),(10,11),(8,12)],'#3c783e')
    dots(im,[(5,8),(6,8),(9,10)],'#bddb73')
    line(im,[(5,13),(9,13)],'#49613a')

def light(im):
    poly(im,[(7,8),(8,8),(8,9),(10,10),(9,11),(8,11),(8,13),(7,12),(7,11),(5,10),(7,9)],'#c39749')
    poly(im,[(7,9),(8,9),(9,10),(8,11),(7,12),(7,11),(6,10)],'#ffe8a3')
    dots(im,[(7,9),(7,10),(8,10),(7,11)],'#fff6d7')
    dots(im,[(5,8),(10,12)],'#dec176')

def shadow(im):
    poly(im,[(5,9),(6,8),(9,8),(10,9),(9,10),(8,10),(8,11),(9,12),(10,12),(9,13),(6,13),(5,12)],'#3e285b')
    line(im,[(6,9),(6,10),(5,11),(6,12),(8,12)],'#72538d')
    line(im,[(7,8),(9,8),(10,9),(8,9),(7,10),(7,11),(8,12),(9,12)],'#211c38')
    dots(im,[(6,9),(6,11)],'#a17bb2')

def ice(im):
    poly(im,[(5,10),(7,8),(10,9),(11,11),(9,13),(6,13),(5,12)],'#397f9f')
    poly(im,[(5,10),(7,8),(10,9),(8,11),(6,11)],'#c5edf0')
    poly(im,[(6,11),(8,11),(9,13),(6,13)],'#71bbd3')
    poly(im,[(8,11),(10,9),(11,11),(9,13)],'#4a9dbe')
    line(im,[(6,10),(7,9),(8,9)],'#f0f8ee')
    dots(im,[(6,12),(9,11)],'#a5dfe7')

def frost(im):
    line(im,[(5,9),(9,13)],'#6d8dac')
    line(im,[(9,9),(5,13)],'#6d8dac')
    line(im,[(7,8),(7,13)],'#d8eafa')
    line(im,[(5,10),(9,10)],'#b4cde7')
    line(im,[(6,9),(8,11)],'#f4f6f4')
    line(im,[(8,9),(6,11)],'#e9f4f5')
    dots(im,[(10,12),(6,13),(9,8)],'#9ab5d4')
    dots(im,[(7,10)],'#ffffff')

def lightning(im):
    line(im,[(6,9),(6,12),(9,13),(10,11)],'#514978')
    poly(im,[(8,7),(10,7),(8,10),(10,10),(7,13),(8,11),(6,11)],'#c39040')
    line(im,[(9,8),(7,10),(9,10),(7,12)],'#ffe394')
    dots(im,[(8,9),(8,10)],'#fff4c8')
    dots(im,[(5,10),(10,12)],'#8d8bbb')

def wood(im):
    poly(im,[(5,10),(7,8),(10,11),(10,12),(8,13),(7,13)],'#543b2f')
    poly(im,[(5,10),(7,8),(9,10),(7,12)],'#a87a47')
    line(im,[(6,10),(7,9),(9,11)],'#cfaa6d')
    line(im,[(7,11),(8,12)],'#775033')
    poly(im,[(7,12),(9,10),(10,11),(10,12),(9,13),(8,13)],'#c2985d')
    dots(im,[(8,12),(9,11)],'#704b33')
    dots(im,[(9,12)],'#ebca88')

def metal(im):
    poly(im,[(5,10),(8,8),(10,9),(11,11),(8,13),(5,12)],'#3e5666')
    poly(im,[(5,10),(8,8),(10,9),(8,11),(5,11)],'#afc6cc')
    line(im,[(6,10),(8,9),(9,9)],'#eff1dd')
    poly(im,[(5,11),(8,11),(8,13),(5,12)],'#718c9d')
    poly(im,[(8,11),(10,9),(11,11),(8,13)],'#597381')
    line(im,[(8,11),(10,10)],'#d4ded4')

def ember(im):
    poly(im,[(5,11),(6,9),(8,9),(9,11),(10,10),(11,12),(9,13),(5,13)],'#413b40')
    line(im,[(6,10),(7,10),(8,11)],'#79605a')
    rect(im,(5,12,7,13),'#574b49')
    line(im,[(7,11),(7,12),(9,12),(10,11)],'#d06b38')
    dots(im,[(7,11),(9,12)],'#ffc76a')
    dots(im,[(8,8)],'#a8573c')

def sand(im):
    poly(im,[(4,12),(6,11),(7,9),(9,10),(10,12),(11,12),(10,13),(5,13)],'#a07d45')
    poly(im,[(5,12),(6,11),(7,9),(8,10),(8,11),(9,12)],'#ddbd79')
    line(im,[(6,11),(7,10),(8,11)],'#f3d99e')
    line(im,[(6,13),(9,13)],'#b99559')
    dots(im,[(10,9),(9,8)],'#c8a565')

def bud(im):
    line(im,[(7,12),(7,10),(8,9)],'#4c8b67')
    poly(im,[(7,10),(8,7),(9,8),(9,9),(8,10)],'#d79b9f')
    dots(im,[(8,8)],'#ffe2bd')
    poly(im,[(7,11),(5,9),(6,9),(7,10)],'#9acb8e')
    rect(im,(6,12,8,13),'#a37c53')
    line(im,[(6,12),(7,12)],'#e1c297')

def dawn(im):
    poly(im,[(4,11),(6,10),(9,10),(11,11),(11,12),(10,13),(5,13),(4,12)],'#b67283')
    rect(im,(5,11,10,12),'#df9d8e')
    poly(im,[(6,10),(6,9),(7,8),(8,8),(9,9),(9,10)],'#efbd79')
    rect(im,(7,9,8,10),'#fff0bc')
    line(im,[(5,11),(10,11)],'#f5c9a0')
    line(im,[(6,13),(9,13)],'#9b718c')

def shadow_essence(im):
    poly(im,[(5,10),(7,10),(8,9),(8,8),(10,9),(10,11),(11,12),(10,13),(5,13),(4,12)],'#252d40')
    line(im,[(6,11),(8,11),(9,10),(9,9)],'#62728e')
    line(im,[(5,12),(7,12),(8,13),(10,12)],'#45546e')
    dots(im,[(5,10),(8,8)],'#8d9ab0')

def prism():
    im=canvas()
    poly(im,[(2,12),(7,2),(9,2),(14,11),(11,13),(3,13)],'#4b6276')
    poly(im,[(7,3),(9,3),(13,11),(10,11)],'#7c94b0')
    poly(im,[(3,12),(7,3),(11,12)],'#b8dfdc')
    poly(im,[(4,11),(7,4),(7,10)],'#eff2d8')
    poly(im,[(7,5),(10,11),(8,11)],'#87c3cf')
    line(im,[(3,12),(10,12)],'#719aa9')
    line(im,[(7,3),(11,12)],'#d9e9df')
    line(im,[(9,4),(12,10)],'#a59fbe')
    dots(im,[(8,7),(9,8)],'#dbc5ae')
    dots(im,[(9,9),(10,10)],'#b2c7a9')
    dots(im,[(11,11),(12,11)],'#9e9cb8')
    return im

def shadow_bottle():
    im=canvas()
    # Violet, light-shielding glass, broader and heavier than the element flask.
    poly(im,[(6,1),(9,1),(9,4),(11,5),(12,6),(13,8),(13,11),(12,13),(10,14),(5,14),(3,13),(2,11),(2,8),(3,6),(4,5),(6,4)],'#252535')
    rect(im,(6,1,9,1),'#777b86')
    line(im,[(6,1),(8,1)],'#c6c9c5')
    rect(im,(7,2,8,3),'#43415a')
    rect(im,(5,4,10,4),'#5a4b77')
    line(im,[(5,4),(8,4)],'#9d8ab2')
    poly(im,[(4,6),(6,5),(9,5),(11,7),(12,9),(11,12),(9,13),(5,13),(3,11),(3,8)],'#403950')
    poly(im,[(5,6),(6,5),(8,5),(8,6),(5,8),(4,10),(3,10),(3,8)],'#625172')
    line(im,[(4,7),(4,9)],'#a18eaf')
    dots(im,[(5,6),(4,10)],'#7c698a')
    poly(im,[(8,7),(10,7),(11,9),(10,12),(7,13),(5,12),(6,10)],'#292735')
    line(im,[(11,8),(12,10),(11,12),(10,13)],'#4f4165')
    line(im,[(5,13),(8,13)],'#574964')
    return im

STONE={'base':'#68696a','dark':'#484a50','shadow':'#57595e','middle':'#727475','light':'#828582','lit':'#92938a'}
def dead_matter():
    im=canvas()
    poly(im,[(2,10),(3,7),(5,7),(6,4),(10,3),(12,5),(13,8),(13,11),(10,13),(5,13)],STONE['dark'])
    poly(im,[(3,9),(4,7),(6,7),(7,5),(10,4),(12,6),(12,9),(9,11),(5,11)],STONE['base'])
    poly(im,[(4,8),(6,7),(7,5),(10,4),(11,6),(8,7),(7,9),(4,10)],STONE['middle'])
    line(im,[(7,5),(9,4),(10,4)],STONE['light'])
    line(im,[(4,8),(5,8),(6,7)],STONE['light'])
    poly(im,[(5,11),(8,10),(9,8),(12,8),(12,10),(10,12),(5,12)],STONE['shadow'])
    line(im,[(8,7),(9,8),(8,9),(7,9)],STONE['dark'])
    dots(im,[(10,6),(11,7)],STONE['shadow'])
    line(im,[(5,11),(6,10),(7,10)],STONE['base'])
    return im

def block_side():
    # Soft consolidated stone. Broad offset strata avoid a distinctive central
    # crack or the isolated bright flecks that advertise a repeating tile.
    im=Image.new('RGBA',(16,16),'#696c6d')
    def wrapped_poly(points,col):
        for dx in (-16,0,16):
            for dy in (-16,0,16):
                poly(im,[(x+dx,y+dy) for x,y in points],col)
    def wrapped_line(points,col):
        for dx in (-16,0,16):
            for dy in (-16,0,16):
                line(im,[(x+dx,y+dy) for x,y in points],col)
    wrapped_poly([(-3,0),(1,-1),(4,0),(5,2),(3,3),(-1,3),(-3,2)],'#737777')
    wrapped_poly([(7,1),(10,0),(13,1),(14,3),(11,4),(8,3)],'#62666a')
    wrapped_poly([(2,5),(6,4),(8,5),(7,7),(3,8),(1,7)],'#747875')
    wrapped_poly([(11,6),(14,5),(17,6),(18,8),(15,9),(11,8)],'#777b77')
    wrapped_poly([(-1,10),(3,9),(5,10),(5,12),(2,13),(-2,12)],'#616569')
    wrapped_poly([(7,10),(10,9),(13,10),(13,12),(10,13),(6,12)],'#727676')
    wrapped_poly([(3,14),(6,13),(8,14),(7,16),(4,16),(2,15)],'#63676a')
    wrapped_poly([(12,14),(15,13),(18,14),(18,16),(14,17),(11,16)],'#717574')
    wrapped_line([(-1,0),(1,0),(2,1)],'#7e817b')
    wrapped_line([(3,5),(5,5),(6,4)],'#7e817b')
    wrapped_line([(12,6),(14,6)],'#7f827b')
    wrapped_line([(8,10),(10,10),(11,11)],'#7b7e79')
    wrapped_line([(1,11),(3,11)],'#5e6268')
    wrapped_line([(9,3),(11,3)],'#5b6066')
    return im

ELEMENTS=[
 ('earth','土元素瓶',earth,'赭色棱角土团、沉积暗底'),
 ('water','水元素瓶',water,'蓝色波面和清楚的弯月液面'),
 ('wind','风元素瓶',wind,'薄荷白色、留有负空间的气旋'),
 ('fire','火元素瓶',fire,'向上收尖的橙红火舌与浅黄火心'),
 ('grass','草元素瓶',grass,'两簇方向不同的青绿叶片'),
 ('light','光元素瓶',light,'象牙金色光核与短光芒'),
 ('shadow','暗元素瓶',shadow,'紫色空心卷雾，中心保留负空间'),
 ('ice','冰元素瓶',ice,'蓝青色透明冰块、两组不同方向的切面'),
 ('frost','霜元素瓶',frost,'淡蓝白色分叉结晶与霜点'),
 ('lightning','电元素瓶',lightning,'金黄色折线电弧、紫灰余辉'),
 ('wood','木元素瓶',wood,'斜置短木段、浅色切端与深色纹理'),
 ('metal','金元素瓶',metal,'冷灰金属块、大面积平直反光'),
 ('ember','烬元素瓶',ember,'灰黑炭块，局部橙色灼缝'),
 ('sand','砂元素瓶',sand,'淡赭色尖顶砂堆和少量落砂'),
 ('bud','芽元素瓶',bud,'单枝嫩芽、粉色芽尖与浅色种子'),
 ('dawn','晨元素瓶',dawn,'桃色水平霞层与初升浅金光斑'),
 ('shadow_essence','影元素瓶',shadow_essence,'深蓝灰凝影、低位聚积的细尾'),
]

sprites={}
records=[]
def save_item(name, label, im, design, component=None):
    im.save(ASSET / f'textures/item/{name}.png')
    write_json(ASSET / f'models/item/{name}.json',{
      'parent':'minecraft:item/generated','textures':{'layer0':f'elemental_verse:item/{name}'}})
    sprites[name]=im
    records.append({'name':name,'label':label,'kind':'item','size':[16,16],
      'component':component,'texture':f'assets/elemental_verse/textures/item/{name}.png',
      'model':f'elemental_verse:item/{name}','design':design})

save_item('element_vial','空元素瓶',vial(),'容量瓶：玻璃塞、细长颈、单刻度、圆腹、平底','empty')
for element,label,painter,design in ELEMENTS:
    save_item('element_vial_'+element,label,vial(painter),design,element)
save_item('refraction_prism','折射棱镜',prism(),'三角棱镜、象牙白前面、蓝紫侧面与克制的折光色带')
save_item('shadow_bottle','聚影瓶',shadow_bottle(),'矮宽紫晶遮光玻璃、灰银瓶塞、低亮度深色瓶腹')
save_item('dead_matter','死寂物质',dead_matter(),'灰色凝固惰性块，保留层理和短裂纹')

side=block_side()
top=side.transpose(Image.Transpose.ROTATE_90)
bottom=side.copy()
for y in range(16):
    for x in range(16):
        r,g,b,a=bottom.getpixel((x,y))
        bottom.putpixel((x,y),(round(r*.86),round(g*.86),round(b*.87),a))
blocks={'dead_matter':side,'dead_matter_top':top,'dead_matter_bottom':bottom}
for name,im in blocks.items():
    im.save(ASSET / f'textures/block/{name}.png')
    records.append({'name':name,'label':{'dead_matter':'死寂物质 · 侧面','dead_matter_top':'死寂物质 · 顶面',
      'dead_matter_bottom':'死寂物质 · 底面'}[name],'kind':'block','size':[16,16],
      'texture':f'assets/elemental_verse/textures/block/{name}.png','design':'低饱和、无发光、可平铺的凝固层理'})
write_json(ASSET/'models/block/dead_matter.json',{
    'parent':'minecraft:block/cube_bottom_top','textures':{
    'side':'elemental_verse:block/dead_matter','top':'elemental_verse:block/dead_matter_top',
    'bottom':'elemental_verse:block/dead_matter_bottom'}})
write_json(ASSET/'blockstates/dead_matter.json',{'variants':{'':{'model':'elemental_verse:block/dead_matter'}}})

# Exact per-pixel sources: every sprite is directly editable as a 16x16 matrix.
def pixel_source(im):
    colors=sorted(set(im.get_flattened_data())-{(0,0,0,0)})
    chars='0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'
    assert len(colors)<=len(chars)
    index={c:chars[i] for i,c in enumerate(colors)}
    return {'size':16,'palette':{index[c]:'#'+''.join(f'{v:02x}' for v in c[:3]) for c in colors},
      'pixels':[''.join('.' if im.getpixel((x,y))[3]==0 else index[im.getpixel((x,y))]
                         for x in range(16)) for y in range(16)]}
sources={f'item/{k}':pixel_source(v) for k,v in sprites.items()}
sources.update({f'block/{k}':pixel_source(v) for k,v in blocks.items()})
write_json(SOURCE/'pixels.json',{'format':'elemental-verse-native-pixels-v1','sprites':sources})

def checker(size, step=8, light='#343d45',dark='#2d353c'):
    im=Image.new('RGB',size,light)
    dr=ImageDraw.Draw(im)
    for y in range(0,size[1],step):
      for x in range(0,size[0],step):
        if (x//step+y//step)%2: dr.rectangle((x,y,x+step-1,y+step-1),fill=dark)
    return im

def center_text(dr, xy, txt, f, col):
    dr.text((xy[0]-dr.textlength(txt,font=f)/2,xy[1]),txt,font=f,fill=col)

# Main reference sheet: 21 item silhouettes plus three actual block faces.
def contact_sheet():
    width=1100; cellw=174; cellh=224; left=28; y0=130
    im=Image.new('RGB',(width,1110),'#151c22');dr=ImageDraw.Draw(im)
    dr.text((30,24),'元素 · 衰变 / 调整版',font=font(32),fill='#e4ebe5')
    dr.text((30,75),'组合版元素瓶 + 第一版棱镜、聚影瓶与死寂物质  /  原生 16 × 16',font=font(16),fill='#97abae')
    allitems=[(r,sprites[r['name']]) for r in records if r['kind']=='item']
    allitems += [(r,blocks[r['name']]) for r in records if r['kind']=='block']
    for i,(r,sprite) in enumerate(allitems):
      x=left+(i%6)*cellw;y=y0+(i//6)*cellh
      dr.rounded_rectangle((x,y,x+cellw-12,y+cellh-13),radius=9,fill='#202a32')
      backdrop=checker((128,128),8)
      big=sprite.resize((128,128),NN)
      backdrop.paste(big,(0,0),big)
      im.paste(backdrop,(x+17,y+12))
      center_text(dr,(x+80,y+150),r['label'],font(15),'#e1e8df')
      id_=r.get('component') or r['name']
      center_text(dr,(x+80,y+176),id_,font(11),'#899ca5')
      # Honest native-sized specimen, next to the label.
      im.paste(sprite,(x+137,y+178),sprite)
    dr.text((30,1050),'容量瓶：长颈 / 单刻度 / 圆腹       像素光源：左上       放大方式：最近邻 ×8',font=font(14),fill='#9aacaa')
    dr.text((30,1077),'玻璃使用第二版的简约轮廓；17 种元素恢复第一版的形态、颜色与细节。',font=font(13),fill='#718892')
    im.save(PREVIEW/'texture_overview.png')

def inventory_sheet():
    im=Image.new('RGB',(980,450),'#151c22');dr=ImageDraw.Draw(im)
    dr.text((28,22),'物品栏辨识度',font=font(26),fill='#e2e9e0')
    dr.text((28,67),'下方左侧是 1:1 原始尺寸；右侧是相同像素的 ×3 最近邻放大。',font=font(14),fill='#9badb0')
    for scale,x0,y0 in [(1,30,132),(3,310,120)]:
      step=18*scale
      for i,r in enumerate([r for r in records if r['kind']=='item']):
        x=x0+i%7*step;y=y0+i//7*step
        dr.rectangle((x,y,x+step-1,y+step-1),fill='#8b8b8b')
        dr.line([(x,y+step-1),(x,y),(x+step-1,y)],fill='#373737',width=max(1,scale))
        dr.line([(x+1,y+step-1),(x+step-1,y+step-1),(x+step-1,y+1)],fill='#e9e9e9',width=max(1,scale))
        sprite=sprites[r['name']].resize((16*scale,16*scale),NN)
        im.paste(sprite,(x+scale,y+scale),sprite)
    dr.text((30,215),'1:1 / 16 px',font=font(13),fill='#a6b7b5')
    dr.text((311,303),'×3 / 48 px',font=font(13),fill='#a6b7b5')
    dr.text((30,377),'顺序：空瓶 → 7 种基础 → 10 种衍生 → 棱镜 → 聚影瓶 → 死寂物质',font=font(15),fill='#becbc4')
    im.save(PREVIEW/'inventory_scale.png')

def tile_sheet():
    im=Image.new('RGB',(1100,660),'#151c22');dr=ImageDraw.Draw(im)
    dr.text((28,22),'死寂物质 · 平铺检查',font=font(27),fill='#e2e9e0')
    dr.text((28,67),'侧面、顶面、底面均为原生 16 × 16；以下各显示 3 × 3 块拼接。',font=font(14),fill='#9badb0')
    for i,(name,tex) in enumerate(blocks.items()):
      tile=Image.new('RGBA',(48,48))
      for y in range(3):
        for x in range(3): tile.paste(tex,(16*x,16*y))
      im.paste(tile.resize((288,288),NN),(30+i*358,125))
      title=['侧面','顶面','底面'][i]
      center_text(dr,(174+i*358,435),title,font(20),'#d1dcd3')
      single=tex.resize((96,96),NN)
      im.paste(single,(126+i*358,480))
    dr.text((30,610),'设计：低饱和凝固灰质，短层理与不规则块面；无刻意描边的方块接缝。',font=font(15),fill='#a6b8b4')
    im.save(PREVIEW/'dead_matter_tiling.png')

def decay_sheet():
    im=Image.new('RGB',(1080,1010),'#151c22');dr=ImageDraw.Draw(im)
    dr.text((28,22),'七条衰变链',font=font(28),fill='#e2e9e0')
    dr.text((28,69),'瓶内元素变化 → 容器耗尽，留下惰性固体',font=font(16),fill='#9badb0')
    chains=[('earth','sand'),('water','ice'),('wind','lightning'),('fire','ember'),
            ('grass','wood'),('light','dawn'),('shadow','shadow_essence')]
    labels={r['name']:r['label'] for r in records if r['kind']=='item'}
    for row,(a,b) in enumerate(chains):
      y=124+row*122
      for col,name in enumerate(['element_vial_'+a,'element_vial_'+b,'dead_matter']):
        x=95+col*355
        sprite=sprites[name].resize((80,80),NN)
        im.paste(sprite,(x,y),sprite)
        dr.text((x+94,y+27),labels[name],font=font(19),fill='#d4dfd7')
        if col<2: dr.text((x+275,y+22),'→',font=font(31),fill='#637c84')
    dr.text((28,977),'霜、金、芽也包含在素材中；按策划表，它们不加入以上七条衰变链。',font=font(14),fill='#8fa4a7')
    im.save(PREVIEW/'decay_chains.png')

contact_sheet(); inventory_sheet(); tile_sheet(); decay_sheet()

write_json(OUT/'texture_manifest.json',{'namespace':'elemental_verse','frame_size':[16,16],
 'item_texture_count':21,'block_texture_count':3,'textures':records})
write_json(OUT/'element_state_models.json',{
 'item':'elemental_verse:element_vial','component':'elemental_verse:element',
 'note':'此表仅列出资源映射。实际按组件选择模型需要根据目标 Minecraft 版本在模组客户端实现。',
 'states':{'empty':'elemental_verse:item/element_vial',**{e:f'elemental_verse:item/element_vial_{e}' for e,_,_,_ in ELEMENTS}}})
with (OUT/'贴图清单.csv').open('w',encoding='utf-8-sig',newline='') as stream:
    writer=csv.writer(stream);writer.writerow(['中文名','类型','元素组件','贴图路径','尺寸','设计'])
    for r in records:writer.writerow([r['label'],r['kind'],r.get('component',''),r['texture'],'16x16',r['design']])

readme='''# 阶段 01 · 元素与衰变贴图 · 调整版

## 内容

原创绘制 24 张静态 PNG：21 张物品贴图（空容量瓶、17 种元素瓶、折射棱镜、聚影瓶、死寂物质），以及死寂物质的侧面、顶面、底面 3 张方块贴图。

全部原生 16×16、RGBA、透明度仅为 0/255。没有抗锯齿、缩小大图、随机噪声、平滑渐变或外发光。物品光源位于左上；预览仅用最近邻整数倍放大。

本次按“第二版的容量瓶装第一版的东西”组合：容量瓶玻璃完整沿用第二版三色外壳；瓶内 17 种元素使用第一版原始绘图，恢复其形态、颜色与细节。空瓶沿用第二版；折射棱镜、聚影瓶、死寂物质及其三张方块贴图恢复第一版原图。

## 造型

- 元素瓶以容量瓶为参考：简化玻璃塞、细长颈、一条定容刻线、圆腹和平底。18 种瓶子状态共用第二版三色玻璃轮廓，瓶内 17 种元素采用第一版的逐像素绘图。
- 折射棱镜恢复第一版：三角轮廓、浅色正面、蓝紫侧面与少量折光色带。
- 聚影瓶恢复第一版：紫晶遮光玻璃、灰银瓶塞和瓶腹反光。
- 死寂物质及其方块恢复第一版：物品为带层理短裂纹的凝固灰块，方块为低饱和凝灰材质。
- 文件中的“金”按 metal 处理为冷色金属；“暗”是 shadow，“影”是 shadow_essence。没有增加策划以外的新物品。
- 这里只补充素材；不修改衰变、采集、配方等玩法。也不添加每种元素“亮/暗/灰”三阶段贴图。

## 接入

`assets/elemental_verse/` 可按同名路径合并至模组 `src/main/resources/`。其中包含传统物品模型 JSON、死寂物质方块模型和方块状态。

`element_vial.png` 为默认空瓶；其余瓶子为 `element_vial_<组件值>.png`。17 种元素是同一个物品的组件状态，不是 17 个注册物品。模型映射见 `element_state_models.json`。

实际按数据组件选择模型的代码或新版物品定义取决于目标 Minecraft 版本；本包未接入版本特定的客户端调度。仅复制 PNG 和传统模型不会自动让瓶内元素切换。死寂物质的物品模型默认使用灰块图标，放置后使用完整方块贴图。

不附带猜测版本的 `pack.mcmeta`：这是模组素材目录，不是声明可独立加载的资源包。

## 编辑

`source/layers/` 另含第二版玻璃层与第一版 17 张元素内容层，全部为原生 16×16 PNG。绘制内容层后再叠加玻璃层，即为最终图标。分层来源和文件对应表见 `source/layer_composition.json`。

`source/pixels.json` 为 24 张图的逐像素可编辑源。每张图有自己的色板与 16 行字符；`.` 表示透明。`source/render_pixels.py` 可重新导出 PNG 并检查网格。依赖 Python 与 Pillow。

原始绘图逻辑同时保存在 `source/draw_originals.py`，用于重建本次原稿与预览。手改字符源后使用 `render_pixels.py` 导出；重跑 `draw_originals.py` 会覆盖字符源与贴图，将它们还原为本次原稿。

## 参考与验证

风格约束来自 https://blockbench.net/wiki/guides/minecraft-style-guide/ 。容量瓶形态参考实验室容量瓶长颈、刻线和大腹的结构。

实际查看了用户参考库内 Ender IO 的空/满灵魂瓶、GTM 玻璃管、农夫乐事奶瓶、匠魂倒置瓶，以及 Create 的抛光玫瑰石英、暗影钢、小块深板岩砖。仅提取玻璃边缘、切面明暗、色阶与块面组织方式，没有复制或重新着色现成图标；原参考图不包含在交付包内。路径记录见 `source/reference_sources.json`。

按用户提供的 ZIP 中 `minecraft-compat-pixel-art` skill 执行，使用字符/整数坐标原生绘图。

已检查实际尺寸、透明度、色板、资源路径和字符源可重建性；已查看原尺寸/放大排列和方块 3×3 平铺预览。未运行 Minecraft 客户端，没有声称完成游戏内验证。详细检查见 `validation.json`。
'''
(OUT/'README.md').write_text(readme,encoding='utf-8')

renderer='''"""Export edited pixels.json to the asset PNGs. Requires Pillow."""
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
'''
(SOURCE/'render_pixels.py').write_text(renderer,encoding='utf-8')
if SCRIPT != (SOURCE/'draw_originals.py').resolve():
    shutil.copy2(SCRIPT,SOURCE/'draw_originals.py')
ref=ROOT/'work/reference-board.png.sources.json'
if ref.exists():shutil.copy2(ref,SOURCE/'reference_sources.json')

# Validation of the exact exported files and each editable source, not the previews.
checks=[]
for r in records:
    path=OUT/r['texture'];im=Image.open(path).convert('RGBA')
    assert im.size==(16,16),path
    alphas={px[3] for px in im.get_flattened_data()}
    assert alphas<={0,255} and 255 in alphas,path
    if r['kind']=='block': assert alphas=={255},path
    src=sources[r['kind']+'/'+r['name']]
    rebuilt=canvas()
    for y,row in enumerate(src['pixels']):
      assert len(row)==16
      for x,c in enumerate(row):
        if c!='.': rebuilt.putpixel((x,y),rgba(src['palette'][c]))
    assert rebuilt.tobytes()==im.tobytes(),path
    checks.append({'file':r['texture'],'size':[16,16],'alpha':sorted(alphas),
      'opaque_colors':len(set(im.get_flattened_data())-{(0,0,0,0)}),
      'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'source_roundtrip':True})
for path in (ASSET/'models').rglob('*.json'):
    data=json.loads(path.read_text(encoding='utf-8'))
    for value in data.get('textures',{}).values():
      if value.startswith('elemental_verse:'):
        assert (ASSET/'textures'/f'{value.split(":",1)[1]}.png').is_file(),(path,value)
write_json(OUT/'validation.json',{'native_size':True,'binary_alpha':True,'source_roundtrip':True,
  'all_local_model_textures_resolve':True,'item_textures':21,'block_textures':3,
  'in_game_checked':False,'files':checks})
print(f'Saved 21 item textures and 3 block faces to {OUT}')
print('Preview: '+str(PREVIEW/'texture_overview.png'))
