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

AM={'void':'#181c30','edge':'#2b223d','dark':'#3c2d50','body':'#533a6a',
    'plane':'#654977','facet':'#74508d','crystal_dark':'#5e3c7b',
    'crystal':'#b18acf','glint':'#e5cef2','silver_dark':'#56647e',
    'silver_mid':'#8090a6','silver':'#b0bbc9','silver_light':'#e1e9ed'}

GOLD={'shade':'#865e32','body':'#c7a047','light':'#ecd18a'}

def altar_body():
    p=AM
    im=canvas(color=p['edge'])
    # A crystal lip has a sloping underside rather than a uniform stripe.
    rect(im,(0,0,15,1),p['facet'])
    line(im,[(0,0),(5,0)],p['crystal'])
    poly(im,[(0,1),(7,1),(9,2),(0,2)],p['plane'])
    line(im,[(9,1),(15,1)],p['body'])
    line(im,[(0,2),(15,2)],p['silver_dark'])
    line(im,[(1,2),(10,2)],p['silver_mid'])
    line(im,[(1,2),(4,2)],p['silver'])
    dots(im,[(1,2)],p['silver_light'])
    # Narrow shadow under the overhang, terminating at metal corner caps.
    line(im,[(0,3),(15,3)],p['void'])
    rect(im,(1,4,14,11),p['body'])
    line(im,[(1,4),(1,10)],p['facet'])
    line(im,[(14,4),(14,11)],p['edge'])
    poly(im,[(1,9),(3,11),(5,11),(2,8)],p['plane'])
    poly(im,[(12,9),(14,8),(14,11),(11,11)],p['dark'])
    rect(im,(1,3,2,4),p['silver_dark'])
    dots(im,[(1,3)],p['silver'])
    rect(im,(13,3,14,4),p['silver_dark'])
    dots(im,[(13,3)],p['silver_mid'])
    # A stepped plinth is brighter on the left and recedes below its lip.
    line(im,[(0,12),(15,12)],p['void'])
    line(im,[(2,12),(6,12)],p['silver_dark'])
    line(im,[(0,13),(15,13)],p['silver_dark'])
    line(im,[(1,13),(4,13)],p['silver'])
    line(im,[(5,13),(11,13)],p['silver_mid'])
    rect(im,(3,14,12,15),p['dark'])
    line(im,[(3,14),(11,14)],p['body'])
    line(im,[(3,14),(5,14)],p['plane'])
    rect(im,(1,13,2,15),p['silver_dark'])
    rect(im,(13,13,14,15),p['silver_dark'])
    dots(im,[(1,13),(1,14)],p['silver'])
    dots(im,[(2,13)],p['silver_light'])
    dots(im,[(13,13)],p['silver_mid'])
    return im

def ritual_altar_side():
    p=AM
    im=altar_body()
    # Sloping panel edges are readable as cuts in the cabinet's violet mass.
    poly(im,[(4,4),(11,4),(12,5),(12,9),(10,11),(4,11),(3,10),(3,5)],p['void'])
    poly(im,[(4,5),(10,5),(11,6),(11,9),(9,10),(4,10)],p['dark'])
    poly(im,[(4,5),(7,5),(8,6),(6,8),(4,8)],p['plane'])
    line(im,[(5,4),(10,4)],p['body'])
    line(im,[(4,11),(10,11)],p['edge'])
    # A crescent casting has a bright upper bevel and a muted lower face.
    line(im,[(7,5),(5,5),(4,6),(4,8),(5,9),(7,10),(8,10)],p['silver_dark'])
    line(im,[(6,5),(5,5),(4,6)],p['silver'])
    dots(im,[(5,5)],p['silver_light'])
    line(im,[(4,7),(4,8),(5,9),(7,10)],p['silver_mid'])
    # The small crystalline fin is cut into three coherent diagonal planes.
    poly(im,[(8,5),(9,5),(11,7),(11,8),(9,10),(8,9),(9,7)],p['crystal_dark'])
    line(im,[(8,5),(9,5),(10,6)],p['crystal'])
    dots(im,[(8,5)],p['glint'])
    poly(im,[(9,6),(10,7),(9,8),(8,8)],p['facet'])
    line(im,[(10,7),(10,8),(9,9)],p['crystal_dark'])
    line(im,[(9,10),(11,9)],p['silver_dark'])
    dots(im,[(9,10)],p['silver_mid'])
    line(im,[(2,5),(2,8)],p['dark'])
    line(im,[(13,5),(13,10)],p['edge'])
    dots(im,[(2,9)],p['plane'])
    # Hollow gold sign and small lower seal keep their accepted footprints.
    dots(im,[(5,6),(7,6),(5,8),(7,8)],p['edge'])
    dots(im,[(6,7)],p['void'])
    dots(im,[(6,6),(5,7),(7,7),(6,8),(6,9)],GOLD['body'])
    dots(im,[(6,6),(5,7)],GOLD['light'])
    dots(im,[(7,7),(6,9)],GOLD['shade'])
    dots(im,[(8,14)],p['edge'])
    dots(im,[(7,14),(8,15),(9,14)],GOLD['body'])
    dots(im,[(7,14)],GOLD['light'])
    dots(im,[(9,14)],GOLD['shade'])
    return im

def ritual_altar_front():
    p=AM
    im=altar_body()
    # A bevelled violet alcove has its own upper plane and lower shadow.
    poly(im,[(4,4),(11,4),(12,5),(12,9),(10,11),(5,11),(3,9),(3,5)],p['void'])
    rect(im,(4,5,11,9),p['dark'])
    line(im,[(4,5),(6,4),(9,4)],p['plane'])
    line(im,[(5,10),(10,10)],p['edge'])
    # Broken highlights follow the two planes of the cast metal upright.
    line(im,[(2,5),(2,8),(3,10),(4,11)],p['silver_dark'])
    line(im,[(2,5),(2,7)],p['silver_mid'])
    dots(im,[(2,5)],p['silver_light'])
    dots(im,[(2,6),(3,10)],p['silver'])
    line(im,[(3,5),(3,8),(4,9)],p['silver_dark'])
    line(im,[(13,5),(13,8),(12,10),(11,11)],p['silver_dark'])
    line(im,[(13,5),(13,7)],p['silver_mid'])
    dots(im,[(13,5)],p['silver'])
    line(im,[(12,5),(12,8),(11,9)],p['edge'])
    dots(im,[(12,10)],p['silver_mid'])
    # The eight-row faceted socket is fully editable at native pixel size.
    jewel=[
        '..SSss..',
        '.SGCCDs.',
        'SGGCCFDs',
        'SCCCFDDs',
        'sCCFFDDs',
        '.sFFDDs.',
        '..sDDs..',
        '..SSmm..',
    ]
    colors={'S':p['silver'],'s':p['silver_dark'],'m':p['silver_mid'],
            'G':p['glint'],'C':p['crystal'],'F':p['facet'],'D':p['crystal_dark']}
    for dy,row in enumerate(jewel):
        for dx,symbol in enumerate(row):
            if symbol!='.': dots(im,[(4+dx,4+dy)],colors[symbol])
    # Silver claws cross the rim, while the inner facets stay unobscured.
    dots(im,[(6,4)],p['silver_light'])
    dots(im,[(4,7),(5,9)],p['silver_mid'])
    dots(im,[(10,8)],p['silver_dark'])
    dots(im,[(7,3),(8,3)],p['silver_dark'])
    dots(im,[(7,3)],p['silver_mid'])
    # Same restrained open crown and inverse seal as the gold detail pass.
    dots(im,[(7,1),(8,1)],p['edge'])
    dots(im,[(6,1),(7,0),(8,0),(9,1)],GOLD['body'])
    dots(im,[(7,0)],GOLD['light'])
    dots(im,[(6,1),(9,1)],GOLD['shade'])
    dots(im,[(7,14),(8,14)],p['edge'])
    dots(im,[(6,14),(7,15),(8,15),(9,14)],GOLD['body'])
    dots(im,[(6,14)],GOLD['light'])
    dots(im,[(8,15),(9,14)],GOLD['shade'])
    return im

ALTAR_PLATE_RECT=[5,4,6,7]
ALTAR_PITS=[[2,2,3,3],[11,2,3,3],[6,12,3,3]]

def ritual_altar_top():
    p=AM
    im=canvas(color=p['body'])
    # Outer silver rim, amethyst bevel, recessed inner working surface.
    line(im,[(0,15),(0,0),(15,0)],p['silver_mid'])
    line(im,[(0,4),(0,0),(5,0)],p['silver'])
    dots(im,[(0,0)],p['silver_light'])
    line(im,[(15,1),(15,15),(1,15)],p['silver_dark'])
    line(im,[(1,13),(1,1),(14,1)],p['facet'])
    line(im,[(14,2),(14,14),(2,14)],p['dark'])
    line(im,[(2,13),(5,13)],p['plane'])
    line(im,[(10,13),(13,13)],p['edge'])
    # Joined corner collars give the rim an attachment to the cap below.
    dots(im,[(1,1)],p['silver'])
    dots(im,[(14,1),(1,14)],p['silver_mid'])
    dots(im,[(14,14)],p['silver_dark'])
    # Four connected slab facets use light from upper-left consistently.
    poly(im,[(2,5),(3,4),(4,5),(3,9),(5,11),(4,12),(2,10)],p['plane'])
    poly(im,[(12,5),(13,5),(13,10),(11,12),(10,11),(12,9)],p['edge'])
    line(im,[(3,12),(4,13),(5,12)],p['facet'])
    line(im,[(10,12),(11,13),(12,12)],p['dark'])
    # Crystal clasp has its own silver setting and a shaded cut tip.
    line(im,[(6,1),(9,1)],p['silver_mid'])
    dots(im,[(6,2),(9,2)],p['silver_dark'])
    poly(im,[(7,1),(8,1),(9,2),(8,3),(7,3),(6,2)],p['crystal_dark'])
    dots(im,[(7,1)],p['glint'])
    dots(im,[(6,2),(7,2)],p['crystal'])
    dots(im,[(8,2)],p['facet'])
    # The surrounding silver brace has broad planes and a dark lower lip.
    line(im,[(4,5),(4,8),(3,9),(3,10),(4,11),(5,11)],p['silver_dark'])
    line(im,[(4,5),(4,7)],p['silver_mid'])
    dots(im,[(4,5)],p['silver_light'])
    dots(im,[(4,6),(3,9)],p['silver'])
    line(im,[(11,5),(11,8),(12,9),(12,10),(11,11),(10,11)],p['silver_dark'])
    dots(im,[(11,5),(12,9)],p['silver_mid'])
    line(im,[(4,11),(5,12)],p['silver_dark'])
    line(im,[(10,11),(9,12)],p['silver_dark'])
    # Original [5,4,6,7] tablet recess: dark wall, floor, low silver ledge.
    rect(im,(5,4,10,10),p['void'])
    line(im,[(5,10),(5,4),(10,4)],p['edge'])
    rect(im,(6,5,9,9),p['dark'])
    rect(im,(6,5,8,6),p['void'])
    line(im,[(6,9),(9,9),(9,6)],p['silver_dark'])
    dots(im,[(9,9)],p['silver_mid'])
    # Three exact established 3x3 sockets remain dark functional openings.
    for x,y,w,h in ALTAR_PITS:
        rect(im,(x,y,x+w-1,y+h-1),p['void'])
        line(im,[(x,y),(x+w-1,y)],p['edge'])
        line(im,[(x,y+h-1),(x+w-1,y+h-1),(x+w-1,y+1)],p['silver_dark'])
        dots(im,[(x+w-1,y+h-1)],p['silver_mid'])
    # Gold glyphs remain narrow, with an incised dark-purple inner edge.
    dots(im,[(3,5),(3,7),(3,8),(2,9)],p['edge'])
    dots(im,[(2,5),(2,6),(3,6),(2,7),(2,8)],GOLD['body'])
    dots(im,[(2,5),(3,6)],GOLD['light'])
    dots(im,[(2,8)],GOLD['shade'])
    dots(im,[(12,5),(12,7),(12,8),(13,9)],p['edge'])
    dots(im,[(13,5),(13,6),(12,6),(13,7),(13,8)],GOLD['body'])
    dots(im,[(12,6)],GOLD['light'])
    dots(im,[(13,7),(13,8)],GOLD['shade'])
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
