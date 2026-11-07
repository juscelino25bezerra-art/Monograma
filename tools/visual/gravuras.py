#!/usr/bin/env python3
# Converte as gravuras de teixo da proposta (SVG do YewPlay em Compose, assets/yewplay/gravuras)
# em VectorDrawables. Uso: python3 tools/visual/gravuras.py <pasta dos SVG> <res/drawable> [pasta de prévia]
# Precisa de skia-pathops (pip install skia-pathops).
import re, pathops
import sys
G=sys.argv[1].rstrip('/')+'/'
S=sys.argv[3].rstrip('/')+'/' if len(sys.argv)>3 else None
names={'welcome':'yew_gravura_boas_vindas','subs':'yew_gravura_inscricoes','downloads':'yew_gravura_downloads','about':'yew_gravura_sobre'}
colors={'#F3EEE9':'@color/mobile_color_on_surface','#E0364F':'#FFE0364F','#9E2236':'#FF9E2236'}
LIMIT=14000; OVER=0.6
def parse(d):
    p=pathops.Path(); toks=re.findall(r'[MCLZ]|-?\d+\.?\d*',d); i=0; cmd=None
    while i<len(toks):
        t=toks[i]
        if t in 'MCLZ': cmd=t; i+=1
        if cmd=='Z': p.close(); continue
        n={'M':2,'L':2,'C':6}[cmd]; v=list(map(float,toks[i:i+n])); i+=n
        if cmd=='M': p.moveTo(*v); cmd='L'
        elif cmd=='L': p.lineTo(*v)
        else: p.cubicTo(*v)
    return p
f=lambda x:('%.1f'%x).rstrip('0').rstrip('.').replace('-0','-0') if abs(x)>=0.05 else '0'
def emit(p):
    out=[]
    for verb,pts in p.segments:
        if verb=='moveTo': out.append('M'+','.join(f(c) for c in pts[0]))
        elif verb=='lineTo': out.append('L'+','.join(f(c) for c in pts[0]))
        elif verb=='curveTo': out.append('C'+' '.join(','.join(f(c) for c in q) for q in pts))
        elif verb=='qCurveTo': out.append('Q'+' '.join(','.join(f(c) for c in q) for q in pts))
        elif verb=='closePath': out.append('Z')
    return ''.join(out)
def rect(x0,y0,x1,y1):
    r=pathops.Path(); r.moveTo(x0,y0); r.lineTo(x1,y0); r.lineTo(x1,y1); r.lineTo(x0,y1); r.close(); return r
def split(p,box,depth=0):
    s=emit(p)
    if len(s)<=LIMIT or depth>8: return [s] if s else []
    x0,y0,x1,y1=box
    if x1-x0>=y1-y0: m=(x0+x1)/2; boxes=[(x0,y0,m+OVER,y1),(m-OVER,y0,x1,y1)]
    else: m=(y0+y1)/2; boxes=[(x0,y0,x1,m+OVER),(x0,m-OVER,x1,y1)]
    res=[]
    for b in boxes:
        q=pathops.op(p,rect(*b),pathops.PathOp.INTERSECTION)
        res+=split(q,b,depth+1)
    return res
for k,n in names.items():
    s=open(G+f'yewplay_gravura_{k}.svg').read()
    w,h=map(float,re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"',s).groups())
    out=['<?xml version="1.0" encoding="utf-8"?>',
    f'<!-- YEWPLAY(gravura): "{k}" da proposta (p. 9 e 17), o mesmo traçado do YewPlay em Compose',
    '     (assets/yewplay/gravuras). Traço em marfim no escuro e tinta no claro; os frutos guardam o rubi',
    '     e a sombra rubi escura; o furo do fruto é vazado e mostra a cor da página. O traço vem cortado',
    '     em ladrilhos que se sobrepõem 0,6 (o aapt2 não aceita um path com mais de 32 mil caracteres);',
    '     gerado por tools/visual/gravuras.py. -->',
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
    f'    android:width="{f(w)}dp"',f'    android:height="{f(h)}dp"',f'    android:viewportWidth="{f(w)}"',f'    android:viewportHeight="{f(h)}">']
    prev=[]
    for fill,d in re.findall(r'<path fill="(#[0-9A-Fa-f]+)" d="([^"]+)"',s):
        p=parse(d)
        for c in split(p,(0,0,w,h)):
            out.append(f'    <path\n        android:fillColor="{colors[fill.upper()]}"\n        android:pathData="{c}" />')
            prev.append(f'<path fill="{fill}" d="{c}"/>')
    out.append('</vector>')
    open(sys.argv[2].rstrip('/')+f'/{n}.xml','w').write('\n'.join(out)+'\n')
    if S: open(S+n+'.svg','w').write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {f(w)} {f(h)}">'+''.join(prev)+'</svg>')
    print(n,len(prev),max(len(x) for x in prev))
