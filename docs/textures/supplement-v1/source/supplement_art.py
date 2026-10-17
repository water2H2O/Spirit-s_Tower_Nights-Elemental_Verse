"""Native 16px drawings for the 2026-10-17 missing-material list."""
from PIL import Image, ImageDraw

def canvas(color=None):return Image.new('RGBA',(16,16),color)
def rect(im,box,col):ImageDraw.Draw(im).rectangle(box,fill=col)
def poly(im,points,col):ImageDraw.Draw(im).polygon(points,fill=col)
def line(im,points,col):ImageDraw.Draw(im).line(points,fill=col,width=1)
def dots(im,points,col):
    draw=ImageDraw.Draw(im)
    for pt in points:draw.point(pt,fill=col)

def dead_dust():
    im=canvas()
    # A granular mound with several detached grains, not a continuous outline.
    p={'shade':'#777d7a','body':'#a5aaa1','light':'#c8cab9','ash':'#e2e0cc'}
    poly(im,[(4,12),(5,10),(7,10),(8,8),(9,9),(9,10),
             (11,11),(12,12),(10,13),(6,13)],p['body'])
    poly(im,[(5,11),(7,10),(8,8),(9,10),(10,11),(8,11),
             (7,12),(5,12)],p['light'])
    dots(im,[(8,9),(6,11)],p['ash'])
    line(im,[(7,13),(9,13),(10,12)],p['shade'])
    rect(im,(2,11,3,11),p['body'])
    rect(im,(11,8,12,8),p['light'])
    dots(im,[(5,7),(3,9),(13,12),(4,14),(11,14)],p['light'])
    dots(im,[(6,14),(9,6)],p['body'])
    dots(im,[(12,10)],p['shade'])
    return im

def prism_loaded(base,element):
    im=base.copy().convert('RGBA')
    # Keep the accepted triangular housing, blue side facet and outline.
    if element=='light':
        poly(im,[(4,11),(7,5),(10,11)],'#d3be7d')
        poly(im,[(5,10),(7,6),(9,10),(8,11),(6,11)],'#efda99')
        poly(im,[(7,7),(8,9),(7,10),(6,9)],'#fff2c1')
        line(im,[(4,12),(9,12)],'#ad9d70')
    elif element=='dawn':
        poly(im,[(4,11),(7,5),(10,11)],'#c49aa2')
        poly(im,[(5,9),(7,5),(9,9)],'#e6bf94')
        poly(im,[(6,9),(6,8),(7,7),(8,8),(8,9)],'#fff0be')
        line(im,[(5,10),(9,10)],'#efc4a4')
        line(im,[(4,11),(10,11)],'#bc849c')
        line(im,[(4,12),(9,12)],'#9b7c99')
    else:raise ValueError(element)
    # One retained reflective ridge states that the contents remain in glass.
    line(im,[(7,3),(11,12)],'#d9e9df')
    return im

def bottle_loaded(base,element):
    im=base.copy().convert('RGBA')
    contents=canvas()
    if element=='shadow':
        # A single suspended purple fog mass. Broad connected bands carry the
        # form, with one curved pale upper edge instead of scattered sparks.
        poly(contents,[(6,7),(9,6),(10,7),(11,8),(10,10),
                       (10,11),(8,12),(6,11),(5,10),(5,8)],'#49334f')
        poly(contents,[(6,8),(7,7),(9,7),(10,8),(9,9),(7,9),
                       (7,10),(8,11),(6,11),(5,9)],'#73527f')
        line(contents,[(6,8),(7,7),(8,7)],'#a389b0')
        poly(contents,[(8,8),(10,8),(10,10),(8,11),(7,10),(8,10)],'#31293e')
    elif element=='shadow_essence':
        # A dense, calm pool of blue-gray shadow: one broad surface and a short
        # taper down its right side distinguish it from suspended dark vapor.
        poly(contents,[(5,10),(7,10),(8,9),(10,9),(11,10),
                       (11,11),(10,12),(9,13),(6,12),(5,11)],'#273041')
        poly(contents,[(5,10),(7,10),(8,9),(10,9),(10,10),
                       (8,11),(6,11)],'#758c9b')
        line(contents,[(6,10),(8,9),(9,9)],'#a6b6bd')
        poly(contents,[(6,11),(8,11),(10,10),(10,12),(8,12)],'#4b6077')
        line(contents,[(10,11),(9,12),(8,12)],'#35465e')
    else:raise ValueError(element)
    # This fixed interior mask keeps the bottle silhouette, stopper, neck,
    # shoulder and reflected foreground glass exactly as in the empty bottle.
    interior=Image.new('1',(16,16))
    ImageDraw.Draw(interior).polygon([(6,6),(9,6),(11,8),(11,11),
                                     (9,13),(6,12),(5,11),(5,8)],fill=1)
    for y in range(16):
        for x in range(16):
            color=contents.getpixel((x,y))
            if color[3] and interior.getpixel((x,y)):
                im.putpixel((x,y),color)
    return im

def element_pouch():
    im=canvas()
    p={'edge':'#4a4140','dark':'#6a5b4b','body':'#9b8b68',
       'cloth':'#c6b48a','light':'#ded0a6','cord':'#9b764d'}
    # Retain the soft outline. The left fold catches light; the lower right
    # and base turn away from it, leaving a broad quiet fabric panel.
    poly(im,[(4,5),(11,5),(12,7),(13,8),(13,12),(11,14),
             (4,14),(2,12),(2,9),(3,7)],p['edge'])
    poly(im,[(4,8),(10,7),(12,9),(12,12),(10,13),(4,13),
             (3,11),(3,9)],p['body'])
    poly(im,[(4,8),(6,8),(6,9),(5,10),(5,12),(4,12),
             (3,11),(3,9)],p['cloth'])
    line(im,[(4,9),(4,10)],p['light'])
    poly(im,[(8,11),(10,10),(11,9),(12,10),(12,12),(10,13),
             (5,13),(5,12),(8,12)],p['dark'])
    poly(im,[(7,10),(9,9),(10,10),(10,12),(8,12),(7,11)],p['body'])
    dots(im,[(6,11),(6,12)],p['cloth'])
    # Two quiet seam marks sit along the curved right edge.
    dots(im,[(11,10),(11,12)],p['cloth'])
    line(im,[(6,13),(8,13)],p['cord'])
    # The dark opening is drawn behind the bottles, so their lower parts
    # disappear into the bag rather than sitting on a flat horizontal rim.
    poly(im,[(4,5),(10,5),(12,6),(11,8),(7,9),(4,7),(3,7)],p['dark'])
    line(im,[(4,6),(10,6)],p['edge'])
    rect(im,(4,3,5,3),'#758d98')
    rect(im,(4,4,6,6),'#8bbacb')
    line(im,[(4,4),(4,5)],'#d6e6df')
    dots(im,[(6,6)],'#758d98')
    rect(im,(8,2,9,2),'#6b806a')
    poly(im,[(8,3),(9,3),(10,5),(9,6),(7,6),(7,5)],'#83a26d')
    line(im,[(8,3),(8,4)],'#c1d3a5')
    dots(im,[(9,6)],'#6b806a')
    poly(im,[(10,5),(11,4),(12,5),(12,7),(10,7)],'#a2c9d1')
    dots(im,[(11,5)],'#e0efea')
    # A rolled lip covers the contents. Small gathers pull towards a single
    # drawstring knot; no new decorative palette or stray particles are added.
    line(im,[(3,7),(6,8),(10,7),(12,7)],p['cord'])
    line(im,[(4,7),(5,7),(6,8),(7,8)],p['light'])
    line(im,[(9,7),(11,7)],p['cloth'])
    dots(im,[(12,8)],p['dark'])
    dots(im,[(7,9)],p['dark'])
    line(im,[(7,8),(8,9),(7,10)],p['cord'])
    dots(im,[(7,8)],p['light'])
    dots(im,[(9,9)],p['cloth'])
    return im

S={'base':'#737d76','light':'#a3aa98','mid':'#8b9587','shade':'#5d6b64',
   'dark':'#414f4c','seal':'#2e3d3b','lead':'#8d9d94','lead_light':'#c2c9b3',
   'lead_dark':'#607b73'}

def sealed_vault_side():
    im=stone_body()
    # Three clean stone clusters with an inset edge, a short seam and a nick.
    poly(im,[(2,6),(6,5),(9,6),(8,8),(5,9),(2,8)],S['mid'])
    poly(im,[(8,9),(11,7),(13,8),(13,12),(10,13),(7,12)],S['shade'])
    poly(im,[(3,9),(6,8),(9,9),(8,11),(4,12),(2,11)],S['base'])
    line(im,[(2,6),(4,6),(5,5)],S['light'])
    line(im,[(3,12),(6,12)],S['dark'])
    line(im,[(12,10),(11,11),(11,12)],S['dark'])
    clamp(im,2,1);clamp(im,12,1)
    return im

def sealed_vault_top():
    im=canvas(S['mid'])
    line(im,[(0,15),(0,0),(15,0)],S['light'])
    line(im,[(15,1),(15,15),(1,15)],S['dark'])
    rect(im,(1,1,14,14),S['base'])
    rect(im,(2,2,13,13),S['seal'])
    poly(im,[(4,3),(11,3),(12,4),(12,11),(11,12),
             (4,12),(3,11),(3,4)],S['base'])
    line(im,[(3,10),(3,4),(4,3),(11,3)],S['mid'])
    line(im,[(12,5),(12,11),(11,12),(5,12)],S['shade'])
    # Natural surface variation is arranged as slabs, not random speckling.
    poly(im,[(4,4),(8,4),(8,5),(6,7),(4,6)],S['mid'])
    poly(im,[(7,9),(11,8),(11,11),(8,11)],S['shade'])
    line(im,[(5,10),(7,9)],S['base'])
    rect(im,(6,0,8,3),S['lead_dark'])
    rect(im,(6,0,7,2),S['lead'])
    line(im,[(6,0),(8,0)],S['lead_light'])
    rect(im,(7,12,9,15),S['lead_dark'])
    rect(im,(7,12,8,14),S['lead'])
    line(im,[(7,12),(9,12)],S['lead_light'])
    return im

def sealed_vault_front():
    im=stone_body()
    # A recessed stone access plate with a strongly shaded lower bevel.
    rect(im,(2,5,13,12),S['dark'])
    rect(im,(3,6,12,11),S['base'])
    line(im,[(3,11),(3,6),(12,6)],S['mid'])
    line(im,[(12,7),(12,11),(4,11)],S['shade'])
    dots(im,[(4,6),(5,6)],S['light'])
    poly(im,[(4,8),(5,7),(7,7),(6,9),(4,10)],S['mid'])
    line(im,[(10,10),(11,10)],S['shade'])
    clamp(im,2,1);clamp(im,12,1)
    # A bent lead strip crosses the lid seam into a stamped, octagonal seal.
    line(im,[(7,2),(7,6)],S['lead_dark'])
    line(im,[(8,2),(8,6)],S['lead_light'])
    poly(im,[(7,7),(9,7),(10,8),(10,9),(9,10),(7,10),(6,9),(6,8)],S['seal'])
    poly(im,[(7,7),(9,7),(9,9),(7,9),(6,8)],S['lead'])
    line(im,[(7,7),(8,7)],S['lead_light'])
    dots(im,[(7,8),(8,9)],S['lead_dark'])
    line(im,[(8,10),(8,11)],S['lead_dark'])
    return im

R={'edge':'#303944','stone':'#535f6b','stone_light':'#77818a',
   'inset':'#414c5b','deep':'#252f3c','bronze':'#8b7957',
   'gold':'#bda779','rune':'#7bb4a9','glow':'#c0ded0','metal_shade':'#615741'}

def rune_cabinet_side():
    im=cabinet_frame()
    # Bronze corner shoes and recessed stone framing form a coherent cabinet
    # carcass. Two broad slabs avoid the previous single flat triangular patch.
    poly(im,[(3,3),(7,3),(8,5),(6,8),(3,7)],R['stone'])
    poly(im,[(8,9),(11,7),(12,8),(12,12),(8,12)],R['deep'])
    line(im,[(4,5),(4,10),(6,10)],R['stone_light'])
    line(im,[(5,5),(5,9)],R['edge'])
    line(im,[(7,11),(10,11)],R['stone'])
    rect(im,(1,2,2,4),R['bronze']);rect(im,(13,11,14,13),R['metal_shade'])
    line(im,[(1,2),(2,2)],R['gold'])
    line(im,[(13,11),(14,11)],R['bronze'])
    rect(im,(1,11,2,13),R['bronze']);rect(im,(13,2,14,4),R['metal_shade'])
    dots(im,[(1,11),(13,2)],R['gold'])
    return im

def rune_cabinet_top():
    im=cabinet_frame()
    rect(im,(2,2,13,13),R['metal_shade'])
    line(im,[(2,13),(2,2),(13,2)],R['bronze'])
    rect(im,(3,3,12,12),R['edge'])
    rect(im,(4,4,11,11),R['inset'])
    poly(im,[(4,4),(8,4),(9,6),(7,7),(4,7)],R['stone'])
    poly(im,[(8,9),(11,8),(11,11),(7,11)],R['deep'])
    line(im,[(4,10),(4,4),(10,4)],R['stone_light'])
    line(im,[(11,5),(11,11),(5,11)],R['stone'])
    # Four small bracket fasteners, placed for a functional cap assembly.
    for x,y in [(1,1),(12,1),(1,12),(12,12)]:
        rect(im,(x,y,x+2,y+2),R['metal_shade'])
        line(im,[(x,y+2),(x,y),(x+2,y)],R['bronze'])
        dots(im,[(x+1,y+1)],R['gold'])
    return im

def charge_pixels():
    # A single contained stepped ring and center bar. All charge pixels live
    # in this explicit mask, leaving the cabinet frame independent of charge.
    ring=[(6,4),(7,4),(8,4),(9,4),(5,5),(10,5),
          (4,6),(4,7),(4,8),(4,9),(11,6),(11,7),(11,8),(11,9),
          (5,10),(10,10),(6,11),(7,11),(8,11),(9,11)]
    core=[(7,6),(7,7),(8,7),(8,8),(8,9)]
    return ring,core

def rune_cabinet_front():
    im=cabinet_frame()
    # One inset door, two left hinges and a separate right latch. The luminous
    # ring keeps the same exact 25-pixel charge mask as the original version.
    rect(im,(2,2,13,13),R['metal_shade'])
    line(im,[(2,13),(2,2),(13,2)],R['gold'])
    rect(im,(3,3,12,12),R['deep'])
    line(im,[(3,12),(3,3),(11,3)],R['inset'])
    line(im,[(12,4),(12,12),(4,12)],R['edge'])
    for y in (5,10):
        rect(im,(1,y,2,y+1),R['bronze'])
        line(im,[(1,y),(2,y)],R['gold'])
        dots(im,[(2,y+1)],R['metal_shade'])
    rect(im,(13,6,14,9),R['deep'])
    line(im,[(13,6),(13,8)],R['bronze'])
    dots(im,[(13,6)],R['gold'])
    dots(im,[(14,9)],R['metal_shade'])
    ring,core=charge_pixels()
    # The channel surrounding the rune is cut into the dark door, so the
    # bright pixels read as inlay rather than a flat painted circle.
    for x,y in ring:
        for pt in [(x+1,y),(x,y+1)]:
            if pt not in ring and pt not in core and 3<pt[0]<12 and 3<pt[1]<12:
                dots(im,[pt],R['edge'])
    dots(im,ring,R['rune']);dots(im,core,R['glow'])
    dots(im,[(1,2),(14,2),(1,13)],R['bronze'])
    return im

def charge_mask():
    im=canvas()
    ring,core=charge_pixels()
    dots(im,ring+core,'#ffffff')
    return im

def draw_all(prism,bottle):
    return {
      'item/dead_dust':dead_dust(),
      'item/prism_light':prism_loaded(prism,'light'),
      'item/prism_dawn':prism_loaded(prism,'dawn'),
      'item/bottle_shadow':bottle_loaded(bottle,'shadow'),
      'item/bottle_shadow_essence':bottle_loaded(bottle,'shadow_essence'),
      'item/element_pouch':element_pouch(),
      'block/sealed_vault_side':sealed_vault_side(),
      'block/sealed_vault_top':sealed_vault_top(),
      'block/sealed_vault_front':sealed_vault_front(),
      'block/rune_cabinet_side':rune_cabinet_side(),
      'block/rune_cabinet_top':rune_cabinet_top(),
      'block/rune_cabinet_front':rune_cabinet_front(),
    }


def stone_body():
    im=canvas(S['base'])
    # The cap projects over the dark continuous gasket. Its bevel wraps the
    # block horizontally rather than drawing a generic square around the face.
    rect(im,(0,0,15,1),S['mid'])
    line(im,[(0,0),(15,0)],S['light'])
    line(im,[(0,2),(15,2)],S['shade'])
    line(im,[(0,3),(15,3)],S['seal'])
    line(im,[(0,4),(15,4)],S['dark'])
    line(im,[(0,5),(0,13)],S['mid'])
    line(im,[(15,5),(15,13)],S['shade'])
    line(im,[(0,14),(15,14)],S['shade'])
    line(im,[(0,15),(15,15)],S['dark'])
    # Two small cut-stone footings, with offset highlights.
    line(im,[(1,13),(3,13)],S['mid'])
    line(im,[(12,13),(14,13)],S['dark'])
    return im

def clamp(im,x,y):
    rect(im,(x,y,x+1,y+3),S['lead_dark'])
    line(im,[(x,y),(x+1,y)],S['lead_light'])
    line(im,[(x,y+1),(x,y+2)],S['lead'])

def cabinet_frame():
    im=canvas(R['stone'])
    line(im,[(0,15),(0,0),(15,0)],R['stone_light'])
    line(im,[(15,1),(15,15),(1,15)],R['edge'])
    rect(im,(2,2,13,13),R['edge'])
    rect(im,(3,3,12,12),R['inset'])
    line(im,[(1,1),(13,1)],R['stone'])
    line(im,[(1,14),(14,14)],R['deep'])
    return im

