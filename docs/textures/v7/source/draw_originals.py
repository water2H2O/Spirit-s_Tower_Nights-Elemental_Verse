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
    OUT = ROOT / 'outputs' / 'elemental_verse_textures_v7'
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
    # A short straight neck and two consistent sloping sides, widening to a
    # broad flat bottom, make the Erlenmeyer silhouette legible at 16px.
    rect(im,(7,0,8,1),G['mid'])
    rect(im,(6,2,9,2),G['mid'])
    line(im,[(6,3),(6,5)],G['mid'])
    line(im,[(9,3),(9,5)],G['edge'])
    line(im,[(5,6),(5,7),(4,8),(4,9),(3,10),(3,11),(2,12),(2,13)],G['mid'])
    line(im,[(10,6),(10,7),(11,8),(11,9),(12,10),(12,11),(13,12),(13,13)],G['edge'])
    line(im,[(3,14),(12,14)],G['edge'])
    line(im,[(4,10),(4,11)],G['light'])
    line(im,[(3,13),(6,13)],G['mid'])
    return im

def vial(content=None):
    im=glass_base()
    if content: content(im)
    im.alpha_composite(glass_overlay())
    return im

# First-version element motifs, fitted to the conical flask glass.
def earth(im):
    poly(im,[(5,11),(6,9),(8,9),(9,10),(10,10),(11,12),(10,13),(5,13)],'#624435')
    poly(im,[(5,11),(6,9),(8,9),(8,11),(7,12),(5,12)],'#ae8653')
    line(im,[(6,10),(7,10)],'#d3ae70')
    rect(im,(8,11,10,12),'#88643e')
    dots(im,[(8,9),(9,12)],'#c1975c')
    line(im,[(6,13),(9,13)],'#473d35')

def water(im):
    # The original blue wave and palette, fitted to the wider conical bottom.
    poly(im,[(4,10),(6,10),(7,9),(9,9),(10,10),(11,10),
             (11,11),(12,12),(12,13),(3,13),(3,12),(4,11)],'#245b9c')
    poly(im,[(4,10),(6,10),(7,9),(9,9),(10,10),(9,11),
             (6,11),(5,12),(3,12),(4,11)],'#429ac8')
    line(im,[(5,10),(6,10),(7,9),(8,9)],'#b0e4e5')
    line(im,[(5,13),(8,13),(10,12),(12,12)],'#3175ae')
    line(im,[(6,12),(8,12),(9,11)],'#2a7fb8')
    return im

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
    # Preserve the first sun and pink horizon while extending the liquid base.
    poly(im,[(4,11),(6,10),(9,10),(11,11),(12,12),(12,13),(3,13),(3,12)],'#b67283')
    poly(im,[(5,11),(10,11),(11,12),(4,12)],'#df9d8e')
    poly(im,[(6,10),(6,9),(7,8),(8,8),(9,9),(9,10)],'#efbd79')
    rect(im,(7,9,8,10),'#fff0bc')
    line(im,[(5,11),(10,11)],'#f5c9a0')
    line(im,[(5,13),(10,13)],'#9b718c')
    return im

def shadow_essence(im):
    # The original condensed shadow can now settle across the full flat base.
    poly(im,[(5,10),(7,10),(8,9),(8,8),(10,9),(10,11),
             (12,12),(12,13),(3,13),(3,12),(4,11)],'#252d40')
    line(im,[(6,11),(8,11),(9,10),(9,9)],'#62728e')
    line(im,[(4,12),(7,12),(8,13),(11,12)],'#45546e')
    dots(im,[(5,10),(8,8)],'#8d9ab0')
    return im

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
    p={'edge':'#45494e','dark':'#5c6062','body':'#7e807c',
       'face':'#a3a399','ash':'#bfbdaf'}
    # Three diagonally stacked flakes. Broken overhangs carry the shape;
    # each plate has one large matte face and a thin compressed dark edge.
    poly(im,[(2,11),(6,8),(10,8),(14,10),(13,11),
             (8,13),(5,13),(2,12)],p['edge'])
    poly(im,[(3,11),(6,9),(10,9),(13,10),(8,12),(5,12)],p['body'])
    line(im,[(5,12),(8,12),(12,10)],p['dark'])
    poly(im,[(1,9),(4,6),(8,6),(12,8),(11,10),
             (7,11),(3,11),(1,10)],p['edge'])
    poly(im,[(2,9),(5,7),(8,7),(11,8),(7,10),(4,10)],p['face'])
    line(im,[(3,10),(7,10),(10,8)],p['dark'])
    poly(im,[(4,7),(7,4),(11,3),(13,5),(12,7),
             (8,9),(5,9),(4,8)],p['edge'])
    poly(im,[(5,7),(7,5),(10,4),(12,5),(10,6),
             (11,6),(8,8),(6,8)],p['ash'])
    line(im,[(6,8),(8,8),(11,6)],p['body'])
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
 ('water','水元素瓶',water,'原蓝色波面与色阶，向锥形瓶底两侧延展'),
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
 ('dawn','晨元素瓶',dawn,'桃色霞层与浅金初阳，延展至锥形瓶底'),
 ('shadow_essence','影元素瓶',shadow_essence,'蓝灰凝影，贴合锥形瓶宽底沉积'),
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

save_item('element_vial','空元素瓶',vial(),'锥形瓶：短直颈、直斜肩、宽平底，三色玻璃','empty')
for element,label,painter,design in ELEMENTS:
    save_item('element_vial_'+element,label,vial(painter),design,element)
save_item('refraction_prism','折射棱镜',prism(),'三角棱镜、象牙白前面、蓝紫侧面与克制的折光色带')
save_item('shadow_bottle','聚影瓶',shadow_bottle(),'矮宽紫晶遮光玻璃、灰银瓶塞、低亮度深色瓶腹')
save_item('dead_matter','死寂物质',dead_matter(),'层叠灰渣：三片不规则薄片错开叠放、破碎断边、五种哑灰色')

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
    dr.text((30,24),'元素 · 衰变 / 锥形瓶与层叠灰渣',font=font(32),fill='#e4ebe5')
    dr.text((30,75),'锥形元素瓶 + 层叠灰渣  /  原生 16 × 16  /  24 张贴图',font=font(16),fill='#97abae')
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
    dr.text((30,1050),'锥形瓶：短颈 / 斜肩 / 宽平底       像素光源：左上       放大方式：最近邻 ×8',font=font(14),fill='#9aacaa')
    dr.text((30,1077),'三色简约玻璃；延续原元素配色与形态；灰渣采用破碎薄片错层堆叠。',font=font(13),fill='#718892')
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

readme='# 阶段 01 · 锥形元素瓶与层叠灰渣\n\n24 张原生 16×16 静态 PNG：空瓶与 17 种元素状态、折射棱镜、聚影瓶、死寂物质物品，以及死寂物质方块的侧/顶/底面。\n\n## 本版外观\n\n- 18 种元素瓶状态改用锥形瓶：短直瓶颈、连续斜肩、宽平底；玻璃仍采用三种颜色和少量反光。\n- 元素内容延续第一版的配色与形态。水、晨、影的底部略作延展以贴合锥形瓶底；其余内容物保留原像素结构。\n- 死寂物质物品采用用户选定的“更像灰渣：薄片堆叠、边缘破碎”：三片薄灰渣错开叠放，用五种哑灰色区分片面与薄断边。\n- 死寂物质方块保留第一版三张原图；折射棱镜和聚影瓶也沿用第一版。\n- 所有图像直接在 16×16 网格绘制；RGBA 透明度仅 0/255；没有抗锯齿、平滑缩放或随机噪声。预览为最近邻整数倍放大。\n\n## 使用\n\n把 `assets/elemental_verse/` 按路径合并到模组的 `src/main/resources/`。\n\n默认瓶子为 `textures/item/element_vial.png`，装载元素为 `element_vial_<组件值>.png`。这是同一物品的 18 种状态，模型映射见 `element_state_models.json`。\n\n包内含传统物品模型、死寂物质方块模型与方块状态。按自定义数据组件选择物品模型仍需按目标 Minecraft 版本在客户端接入；仅复制素材不会自动切换元素图标。本包不附带猜测版本的 `pack.mcmeta`。\n\n## 编辑\n\n`source/pixels.json` 是 24 张贴图的完整逐像素源；`.` 表示透明，每张图的字符网格均为 16×16。编辑后运行 `source/render_pixels.py` 导出 PNG，依赖 Python 和 Pillow。\n\n`source/draw_originals.py` 是本版绘图与预览生成器；运行它会还原本版原稿并覆盖像素源和 PNG。\n\n`source/layers/` 包含锥形瓶玻璃层和 17 个元素内容层。先绘制内容层，再叠加玻璃层即可重建图标；对应关系和拟合说明见 `source/layer_composition.json`。\n\n## 参考与验证\n\n遵循 https://blockbench.net/wiki/guides/minecraft-style-guide/ ，并按用户提供的 `minecraft-compat-pixel-art` skill 使用原生整数像素绘制。原始参考库中实际查看过的贴图路径保存在 `source/reference_sources.json`；本包没有复制原模组 PNG。\n\n已检查原生尺寸、二值透明度、模型纹理路径、像素源导出一致性、分层合成、容器边界和改动范围。已查看原尺寸、最近邻放大图和整组预览。没有运行 Minecraft 客户端。检查结果见 `validation.json`。\n'
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
