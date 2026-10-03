#!/usr/bin/env python3
"""Build an original, freely reusable NES NROM test cartridge; no external assets.

This file and its generated ROM are dedicated to the public domain under CC0 1.0.
Registers are documented at https://www.nesdev.org/wiki/PPU_registers and
https://www.nesdev.org/wiki/Standard_controller .
"""
from pathlib import Path
import hashlib, json, struct

class Asm:
    def __init__(self): self.data=bytearray(); self.labels={}; self.fix=[]
    @property
    def pc(self): return 0xC000 + len(self.data)
    def label(self,n): self.labels[n]=self.pc
    def emit(self,*bs): self.data.extend(bs)
    def abs(self,op,address):
        self.emit(op)
        if isinstance(address,str): self.fix.append((len(self.data),address,'abs')); self.emit(0,0)
        else:self.data.extend(struct.pack('<H',address))
    def branch(self,op,label):self.emit(op);self.fix.append((len(self.data),label,'rel'));self.emit(0)
    def finish(self):
        for pos,label,typ in self.fix:
            address=self.labels[label]
            if typ=='abs':self.data[pos:pos+2]=struct.pack('<H',address)
            else:
                delta=address-(0xC000+pos+1)
                assert -128<=delta<128,(label,delta)
                self.data[pos]=delta&255
        return self.data

# Minimal readable 5x7 glyphs, authored here as binary rows.
font={
 'A':['01110','10001','10001','11111','10001','10001','10001'],
 'B':['11110','10001','10001','11110','10001','10001','11110'],
 'D':['11110','10001','10001','10001','10001','10001','11110'],
 'E':['11111','10000','10000','11110','10000','10000','11111'],
 'F':['11111','10000','10000','11110','10000','10000','10000'],
 'G':['01111','10000','10000','10111','10001','10001','01110'],
 'H':['10001','10001','10001','11111','10001','10001','10001'],
 'I':['11111','00100','00100','00100','00100','00100','11111'],
 'L':['10000','10000','10000','10000','10000','10000','11111'],
 'M':['10001','11011','10101','10101','10001','10001','10001'],
 'N':['10001','11001','10101','10011','10001','10001','10001'],
 'O':['01110','10001','10001','10001','10001','10001','01110'],
 'P':['11110','10001','10001','11110','10000','10000','10000'],
 'Q':['01110','10001','10001','10001','10101','10010','01101'],
 'R':['11110','10001','10001','11110','10100','10010','10001'],
 'S':['01111','10000','10000','01110','00001','00001','11110'],
 'T':['11111','00100','00100','00100','00100','00100','00100'],
 'U':['10001','10001','10001','10001','10001','10001','01110'],
 'V':['10001','10001','10001','10001','10001','01010','00100'],
 'W':['10001','10001','10001','10101','10101','11011','10001'],
 'Y':['10001','10001','01010','00100','00100','00100','00100'],
 '-':['00000','00000','00000','11111','00000','00000','00000'],
}
chrrom=bytearray(8192)
# Tile 1 is the player's arrow; tile 2 is a moving frame beacon.
chrrom[16:24]=bytes([0x18,0x3c,0x7e,0xff,0x18,0x18,0x18,0x00])
chrrom[32:40]=bytes([0x3c,0x7e,0xff,0xff,0xff,0xff,0x7e,0x3c])
chars={c:i+3 for i,c in enumerate(font)}
for c,tile in chars.items():chrrom[tile*16:tile*16+8]=bytes([int(r,2)<<2 for r in font[c]]+[0])
nametable=bytearray(1024)
for row,text in [(5,'EMUUI QA'),(8,'ORIGINAL HOMEBREW'),(22,'D-PAD MOVES'),(24,'A CHANGES COLOR'),(26,'START RESETS')]:
    col=(32-len(text))//2
    nametable[row*32+col:row*32+col+len(text)]=bytes(chars.get(c,0) for c in text)

p=Asm();p.label('reset');p.emit(0x78,0xD8,0xA2,0xFF,0x9A,0xE8) # SEI CLD LDX#ff TXS INX
for reg in (0x2000,0x2001,0x4010):p.abs(0x8E,reg)
p.emit(0xA9,0x40);p.abs(0x8D,0x4017)
p.label('vblank1');p.abs(0x2C,0x2002);p.branch(0x10,'vblank1')
p.emit(0xA9,0,0xA2,0);p.label('clear')
for base in (0,0x100,0x300,0x400,0x500,0x600,0x700):p.abs(0x9D,base)
p.emit(0xE8);p.branch(0xD0,'clear')
p.emit(0xA9,0xFF,0xA2,0);p.label('oamclear');p.abs(0x9D,0x200);p.emit(0xE8);p.branch(0xD0,'oamclear')
p.label('vblank2');p.abs(0x2C,0x2002);p.branch(0x10,'vblank2')
# Initialize PPU palette.
p.emit(0xA9,0x3F);p.abs(0x8D,0x2006);p.emit(0xA9,0);p.abs(0x8D,0x2006);p.emit(0xA2,0)
p.label('palette_loop');p.abs(0xBD,'palette');p.abs(0x8D,0x2007);p.emit(0xE8,0xE0,32);p.branch(0xD0,'palette_loop')
# Copy four pages of nametable via zeropage pointer.
p.abs(0x2C,0x2002);p.emit(0xA9,0x20);p.abs(0x8D,0x2006);p.emit(0xA9,0);p.abs(0x8D,0x2006)
p.emit(0xA9,0);namelo=len(p.data)-1;p.emit(0x85,2,0xA9,0);namehi=len(p.data)-1;p.emit(0x85,3,0xA2,4,0xA0,0)
p.label('name_loop');p.emit(0xB1,2);p.abs(0x8D,0x2007);p.emit(0xC8);p.branch(0xD0,'name_loop');p.emit(0xE6,3,0xCA);p.branch(0xD0,'name_loop')
# Sprite data: Y,tile,attribute,X.
for address,value in [(0x200,120),(0x201,1),(0x202,0),(0x203,120),(0x204,154),(0x205,2),(0x206,1),(0x207,32)]:
    p.emit(0xA9,value);p.abs(0x8D,address)
p.emit(0xA9,0);p.abs(0x8D,0x2005);p.abs(0x8D,0x2005);p.emit(0xA9,0x80);p.abs(0x8D,0x2000);p.emit(0xA9,0x1E);p.abs(0x8D,0x2001)
p.label('main');p.abs(0x4C,'main')
p.label('nmi');p.emit(0x48,0x8A,0x48,0x98,0x48)
# Controller serial read: A bit7, B6, Select5, Start4, Up3, Down2, Left1, Right0.
p.emit(0xA9,1);p.abs(0x8D,0x4016);p.emit(0xA9,0);p.abs(0x8D,0x4016);p.emit(0x85,0,0xA2,8)
p.label('read_pad');p.abs(0xAD,0x4016);p.emit(0x4A,0x26,0,0xCA);p.branch(0xD0,'read_pad')
for mask,address,op,name in [(1,0x203,0xEE,'right'),(2,0x203,0xCE,'left'),(4,0x200,0xEE,'down'),(8,0x200,0xCE,'up')]:
    p.emit(0xA5,0,0x29,mask);p.branch(0xF0,'skip_'+name);p.abs(op,address);p.label('skip_'+name)
p.emit(0xA5,0,0x29,0x80);p.branch(0xF0,'normal_color');p.emit(0xA9,1);p.abs(0x4C,'set_color');p.label('normal_color');p.emit(0xA9,0);p.label('set_color');p.abs(0x8D,0x202)
p.emit(0xA5,0,0x29,0x10);p.branch(0xF0,'no_reset');p.emit(0xA9,120);p.abs(0x8D,0x200);p.abs(0x8D,0x203);p.label('no_reset')
p.abs(0xEE,0x207)
# DMA complete object page; fixed zero scroll.
p.emit(0xA9,0);p.abs(0x8D,0x2003);p.emit(0xA9,2);p.abs(0x8D,0x4014);p.emit(0xA9,0);p.abs(0x8D,0x2005);p.abs(0x8D,0x2005)
p.emit(0x68,0xA8,0x68,0xAA,0x68,0x40)
p.label('irq');p.emit(0x40)
p.label('palette');p.emit(*([0x0F,0x30,0x21,0x11]*4+[0x0F,0x2A,0x30,0x16]+[0x0F,0x16,0x30,0x21]*3))
p.label('nametable');p.emit(*nametable)
p.data[namelo]=p.labels['nametable']&255;p.data[namehi]=p.labels['nametable']>>8
prg=p.finish();assert len(prg)<0x3ffa
prg.extend(bytes(0x3ffa-len(prg)))
for vector in ('nmi','reset','irq'):prg.extend(struct.pack('<H',p.labels[vector]))
rom=b'NES\x1a'+bytes([1,1,0,0])+bytes(8)+prg+chrrom
out=Path(__file__).with_name('EmuUI_QA.nes');out.write_bytes(rom)
Path(__file__).with_name('manifest.json').write_text(json.dumps({'title':'EmuUI QA','license':'CC0-1.0','provenance':'Original test cartridge generated entirely by build_qa_rom.py; no commercial ROM, BIOS, firmware, or external game assets','size':len(rom),'sha256':hashlib.sha256(rom).hexdigest(),'mapper':0,'controls':'D-pad moves arrow; A changes color while held; Start centers arrow. The moving beacon proves frames are running.'},indent=2)+'\n')
print(f'{out}: {len(rom)} bytes sha256={hashlib.sha256(rom).hexdigest()}')
