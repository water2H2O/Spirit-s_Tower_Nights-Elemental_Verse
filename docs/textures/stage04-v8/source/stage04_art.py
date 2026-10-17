"""Native Minecraft-scale art for the Stage 04 declaration/altar additions."""
from PIL import Image,ImageDraw

def canvas(size=(16,16),color=None):return Image.new('RGBA',size,color)
def rect(im,box,col):ImageDraw.Draw(im).rectangle(box,fill=col)
def line(im,points,col):ImageDraw.Draw(im).line(points,fill=col,width=1)
def poly(im,points,col):ImageDraw.Draw(im).polygon(points,fill=col)
def dots(im,points,col):
    dr=ImageDraw.Draw(im)
    for pt in points:dr.point(pt,fill=col)

P={'edge':'#424d4d','shade':'#5b6964','body':'#7c8779',
   'light':'#a3ac94','chip':'#bec4a7','cut':'#3d4b48'}

def declaration_plate():
    im=canvas()
    # Retain the silhouette, carving plane and an uninterrupted writing field.
    poly(im,[(4,1),(11,1),(13,3),(13,12),(11,14),(3,14),
             (2,12),(2,4)],P['edge'])
    poly(im,[(4,2),(10,2),(12,3),(12,11),(10,13),(3,13),(3,4)],P['body'])
    line(im,[(3,10),(3,4),(4,2),(10,2)],P['light'])
    line(im,[(12,4),(12,11),(10,13),(4,13)],P['shade'])
    # Two-pixel lower edge, an inset field and a nick in the upper bevel make
    # the slab tangible without adding written marks to the empty version.
    line(im,[(4,12),(9,12),(11,10),(11,4)],'#68776d')
    poly(im,[(5,4),(8,3),(10,4),(10,5),(7,5),(5,7),(4,6)],'#939f8a')
    poly(im,[(9,9),(11,8),(11,10),(9,12),(6,12),(6,11)],'#6e7c70')
    line(im,[(4,3),(5,2)],P['chip'])
    dots(im,[(9,2)],'#8e9b86')
    dots(im,[(10,13),(11,12)],P['edge'])
    line(im,[(3,6),(3,8)],'#b0b99c')
    return im

TINT_PIXELS=[(x,y) for y in range(6,10) for x in range(6,10)]

def declaration_plate_written():
    im=declaration_plate()
    # Incised short strokes run around the seal. The left/top edges of a cut
    # are dark and the lower lip catches light, so script sits in the stone.
    line(im,[(5,4),(7,4),(7,3)],P['cut'])
    line(im,[(9,3),(9,4),(10,4)],P['cut'])
    dots(im,[(5,3),(8,4)],P['light'])
    line(im,[(4,6),(4,8)],P['cut'])
    line(im,[(4,10),(4,11),(5,11)],P['cut'])
    dots(im,[(11,6),(11,8)],P['cut'])
    line(im,[(6,11),(7,11),(7,12)],P['cut'])
    line(im,[(9,11),(10,11)],P['cut'])
    dots(im,[(5,9),(5,12),(8,11)],P['light'])
    rect(im,(5,5,10,10),P['shade'])
    line(im,[(5,10),(5,5),(10,5)],P['cut'])
    line(im,[(6,10),(10,10),(10,6)],'#a3ac94')
    # Exact same achromatic 4x4 tint footprint, now with two broad seal planes.
    rect(im,(6,6,9,9),'#bcbcbc')
    line(im,[(6,9),(6,6),(9,6)],'#dedede')
    line(im,[(9,7),(9,9),(7,9)],'#909090')
    dots(im,[(7,7),(8,7),(7,8)],'#cdcdcd')
    return im

def declaration_tint_mask():
    im=canvas()
    dots(im,TINT_PIXELS,'#ffffff')
    return im

A={'rim':'#a3aa98','bevel':'#8b9587','stone':'#737d76',
   'shade':'#5d6b64','edge':'#414f4c','deep':'#2e3d3b',
   'copper':'#917f5d','copper_light':'#b6a47e'}

def altar_body():
    im=canvas(color=A['stone'])
    rect(im,(0,0,15,1),A['bevel'])
    line(im,[(0,0),(15,0)],A['rim'])
    line(im,[(0,2),(15,2)],A['shade'])
    line(im,[(0,3),(15,3)],A['deep'])
    rect(im,(1,4,14,11),A['shade'])
    rect(im,(2,4,13,10),A['stone'])
    line(im,[(1,4),(1,11)],A['bevel'])
    line(im,[(14,4),(14,11)],A['edge'])
    # Plinth and feet share continuous edge heights on front and side faces.
    line(im,[(0,12),(15,12)],A['edge'])
    line(im,[(0,13),(15,13)],A['bevel'])
    line(im,[(0,14),(15,14)],A['shade'])
    line(im,[(0,15),(15,15)],A['edge'])
    line(im,[(1,13),(3,13)],A['rim'])
    rect(im,(1,14,2,15),A['deep'])
    rect(im,(13,14,14,15),A['deep'])
    # Folded corner straps, with a single rivet and contact shadow.
    for x in (1,13):
        rect(im,(x,1,x+1,2),A['copper'])
        dots(im,[(x,1)],A['copper_light'])
        dots(im,[(x+1,2)],'#6c624a')
    return im

def ritual_altar_side():
    im=altar_body()
    # Recessed fluted panel bounded by the structural supports.
    rect(im,(3,5,12,10),A['edge'])
    rect(im,(4,5,11,9),A['shade'])
    poly(im,[(4,5),(7,5),(8,6),(6,8),(4,7)],A['stone'])
    line(im,[(4,5),(7,5)],A['bevel'])
    line(im,[(5,6),(5,8)],A['bevel'])
    line(im,[(6,7),(6,9)],A['edge'])
    line(im,[(9,6),(9,8)],A['stone'])
    line(im,[(10,7),(10,9)],A['deep'])
    line(im,[(4,10),(11,10)],A['deep'])
    # Restrained stone shoulders and the shallow relief make this an altar,
    # distinct from the sealed vault's opaque access panel and seal.
    dots(im,[(2,5),(13,5)],A['bevel'])
    line(im,[(3,11),(5,11)],A['shade'])
    return im

def ritual_altar_front():
    im=altar_body()
    rect(im,(4,4,11,11),A['edge'])
    rect(im,(5,5,10,10),A['shade'])
    line(im,[(3,4),(3,10)],A['bevel'])
    line(im,[(4,5),(4,11)],A['deep'])
    line(im,[(11,4),(11,10)],A['bevel'])
    line(im,[(12,5),(12,11)],A['edge'])
    dots(im,[(3,4),(11,4)],A['rim'])
    # A carved tablet medallion echoes the actual item, seated in the stone.
    rect(im,(6,5,9,10),A['deep'])
    rect(im,(6,5,8,9),A['bevel'])
    line(im,[(6,8),(6,5),(8,5)],A['rim'])
    line(im,[(7,6),(7,8),(8,8)],A['edge'])
    line(im,[(6,11),(9,11)],A['copper'])
    dots(im,[(6,11)],A['copper_light'])
    dots(im,[(9,11)],'#6c624a')
    return im

ALTAR_PLATE_RECT=[5,4,6,7]
ALTAR_PITS=[[2,2,3,3],[11,2,3,3],[6,12,3,3]]

def ritual_altar_top():
    im=canvas(color=A['stone'])
    line(im,[(0,15),(0,0),(15,0)],A['rim'])
    line(im,[(15,1),(15,15),(1,15)],A['edge'])
    line(im,[(1,14),(1,1),(14,1)],A['bevel'])
    line(im,[(14,2),(14,14),(2,14)],A['shade'])
    # A few slab planes, placed away from the four receiving recesses.
    poly(im,[(6,1),(9,1),(10,2),(9,3),(6,3)],A['bevel'])
    poly(im,[(2,7),(3,6),(4,7),(4,10),(2,10)],A['shade'])
    poly(im,[(12,6),(13,7),(13,10),(12,10)],A['shade'])
    # Inlaid channels use lit upper edges and one darker end for thickness.
    for pts in [[(4,5),(4,9),(3,10),(3,11),(6,11)],
                [(11,5),(11,9),(12,10),(12,11),(9,11)]]:
        line(im,pts,A['copper'])
    dots(im,[(4,6),(3,11),(11,6)],A['copper_light'])
    dots(im,[(5,11),(10,11)],'#6c624a')
    # Deep side wall and lower ledge make the board slot visibly recessed.
    rect(im,(5,4,10,10),A['deep'])
    rect(im,(6,5,9,9),'#4b5b56')
    line(im,[(5,10),(5,4),(10,4)],A['edge'])
    line(im,[(6,5),(8,5)],A['deep'])
    line(im,[(6,9),(9,9),(9,6)],A['bevel'])
    dots(im,[(9,9)],A['rim'])
    for x,y,w,h in ALTAR_PITS:
        rect(im,(x,y,x+w-1,y+h-1),A['deep'])
        line(im,[(x,y),(x+w-1,y)],A['edge'])
        line(im,[(x,y+h-1),(x+w-1,y+h-1),(x+w-1,y+1)],A['bevel'])
        dots(im,[(x+1,y+1)],'#374945')
        dots(im,[(x+w-1,y+h-1)],A['rim'])
    return im

ELEMENT_SLOTS=[(62,17),(80,17),(98,17)]
PLATE_SLOT=(62,35)
OUTPUT_SLOT=(98,35)
INVENTORY_SLOTS=[(8+18*x,84+18*y) for y in range(3) for x in range(9)]
HOTBAR_SLOTS=[(8+18*x,142) for x in range(9)]
GUI_TEXT_REGIONS={'title':[8,5,160,9],'shape_text':[8,58,160,12],
                  'inventory_label':[8,72,160,9]}
GUI={'body':'#c6c8bf','light':'#f2f1e6','bevel':'#dce0d0','shade':'#858e82',
     'edge':'#505d55','slot':'#8b8b8b','slot_dark':'#373737','slot_light':'#ffffff'}

def slot(im,x,y):
    # The documented positions are the 16x16 content origins; the recess
    # starts one pixel earlier and occupies the standard 18x18 footprint.
    rect(im,(x-1,y-1,x+16,y+16),GUI['slot'])
    line(im,[(x-1,y+15),(x-1,y-1),(x+15,y-1)],GUI['slot_dark'])
    line(im,[(x,y+16),(x+16,y+16),(x+16,y)],GUI['slot_light'])
    rect(im,(x,y,x+15,y+15),GUI['slot'])

def ritual_altar_gui():
    im=canvas((176,166),GUI['body'])
    line(im,[(2,163),(1,162),(1,3),(3,1),(172,1)],GUI['edge'])
    line(im,[(3,2),(172,2),(173,3),(173,162)],GUI['shade'])
    line(im,[(2,162),(2,3),(3,2),(171,2)],GUI['light'])
    line(im,[(3,161),(3,4),(4,3),(171,3)],GUI['bevel'])
    line(im,[(3,163),(172,163),(174,161),(174,3)],GUI['edge'])
    line(im,[(3,162),(172,162),(173,161),(173,4)],GUI['shade'])
    for xy in [(0,0),(1,0),(174,0),(175,0),(0,1),(175,1),
               (0,164),(175,164),(0,165),(1,165),(174,165),(175,165)]:
        im.putpixel(xy,(0,0,0,0))
    # Discrete carved joins and bronze corner insets tie the interface to the
    # stone altar. They live entirely outside slot interiors and text fields.
    for y in (16,54,81,139,159):
        line(im,[(4,y),(5,y)],'#919c8e')
        line(im,[(170,y),(171,y)],'#919c8e')
    for x in (5,168):
        rect(im,(x,16,x+2,18),'#a39678')
        line(im,[(x,16),(x+1,16)],'#ddd4b6')
        dots(im,[(x+2,18)],'#7c7f6c')
    # A shallow work-surface recess frames the exact five-slot arrangement.
    rect(im,(56,15,119,53),'#adb5a7')
    line(im,[(56,53),(56,15),(119,15)],'#899486')
    line(im,[(57,53),(119,53),(119,16)],'#e5e5d7')
    rect(im,(58,16,117,51),'#bec4b6')
    # Two short copper inset leads point towards the upper input row.
    line(im,[(46,22),(50,22),(50,25),(55,25)],'#9a987d')
    line(im,[(129,22),(125,22),(125,25),(120,25)],'#9a987d')
    line(im,[(46,21),(50,21)],'#e0deca')
    line(im,[(125,21),(129,21)],'#e0deca')
    # Inset brackets and chiseled lower termination retain breathing room.
    line(im,[(43,31),(40,31),(40,47),(44,47)],'#9ba694')
    line(im,[(132,31),(135,31),(135,47),(131,47)],'#9ba694')
    line(im,[(41,32),(41,45)],'#d9decd')
    line(im,[(134,32),(134,45)],'#d9decd')
    # Arrow and fine dividers stay clear of the title, shape and inventory text.
    poly(im,[(82,42),(88,42),(88,39),(94,44),(88,49),(88,46),(82,46)],'#778474')
    line(im,[(83,42),(88,42),(88,40)],'#d5dcc9')
    line(im,[(8,55),(74,55)],'#b0b8a8')
    line(im,[(101,55),(167,55)],'#b0b8a8')
    line(im,[(8,56),(74,56)],'#e1e3d5')
    line(im,[(101,56),(167,56)],'#e1e3d5')
    line(im,[(8,139),(167,139)],'#a6b09e')
    line(im,[(8,140),(167,140)],'#dee2d3')
    for x,y in ELEMENT_SLOTS+[PLATE_SLOT,OUTPUT_SLOT]+INVENTORY_SLOTS+HOTBAR_SLOTS:
        slot(im,x,y)
    return im

def draw_all():
    return {'item/declaration_plate':declaration_plate(),
            'item/declaration_plate_written':declaration_plate_written(),
            'block/ritual_altar_side':ritual_altar_side(),
            'block/ritual_altar_top':ritual_altar_top(),
            'block/ritual_altar_front':ritual_altar_front(),
            'gui/ritual_altar':ritual_altar_gui()}
