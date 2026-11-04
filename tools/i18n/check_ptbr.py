#!/usr/bin/env python3
"""Confere a tradução pt-BR: nenhuma chave traduzível faltando e placeholders iguais.

Uso: python3 tools/i18n/check_ptbr.py   (na raiz do repositório)
Sai com código 1 se encontrar problema.
"""
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

PAIRS = [
    ("smarttubetv/src/stmobile/res/values/strings_mobile.xml",
     "smarttubetv/src/stmobile/res/values-pt-rBR/strings_mobile.xml"),
    ("smarttubetv/src/stmobile/res/values/strings_settings.xml",
     "smarttubetv/src/stmobile/res/values-pt-rBR/strings_settings.xml"),
    ("smarttubetv/src/stmobile/res/values/strings_settings_search.xml",
     "smarttubetv/src/stmobile/res/values-pt-rBR/strings_settings_search.xml"),
    ("common/src/main/res/values/strings.xml",
     "common/src/main/res/values-pt-rBR/strings.xml"),
]

# %1$s, %d, %.1f, %% ... (Android/Java format specifiers)
PH = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")
# Android resource refs / tags that must survive translation
REF = re.compile(r"@(?:string|plurals)/[\w.]+|</?[a-zA-Z]+[^>]*>")


def text(el):
    return "".join(el.itertext()) if el is not None else ""


def entries(path):
    root = ET.parse(path).getroot()
    out = {}
    for el in root:
        if el.tag not in ("string", "plurals", "string-array"):
            continue
        if el.get("translatable") == "false":
            continue
        name = el.get("name")
        if el.tag == "string":
            out[(el.tag, name)] = [text(el)]
        elif el.tag == "plurals":
            out[(el.tag, name)] = {i.get("quantity"): text(i) for i in el}
        else:
            out[(el.tag, name)] = [text(i) for i in el]
    return out


def placeholders(s):
    found = PH.findall(s)
    # Positional args may be reordered; plain ones keep order.
    return sorted(found)


def check_pair(src, dst):
    problems = []
    en, pt = entries(src), entries(dst)
    for key, val in en.items():
        if key not in pt:
            problems.append(f"FALTA   {key[0]} {key[1]}")
            continue
        tval = pt[key]
        if key[0] == "plurals":
            # pt-BR needs one/other; compare against English "other"
            ref = val.get("other", "")
            for q in ("one", "other"):
                if q not in tval:
                    problems.append(f"PLURAL  {key[1]} sem quantity={q}")
            ref_ph = set(placeholders(ref))
            for q, s in tval.items():
                if not set(placeholders(s)) <= ref_ph | set(placeholders(val.get(q, ref))):
                    problems.append(f"PLACEH  {key[1]}[{q}] {placeholders(s)} != {sorted(ref_ph)}")
            continue
        if len(val) != len(tval):
            problems.append(f"ARRAY   {key[1]} {len(tval)} itens, esperado {len(val)}")
            continue
        for a, b in zip(val, tval):
            if placeholders(a) != placeholders(b):
                problems.append(f"PLACEH  {key[1]}: {placeholders(b)} != {placeholders(a)}")
            if sorted(REF.findall(a)) != sorted(REF.findall(b)):
                problems.append(f"MARCAÇÃO {key[1]}: {REF.findall(b)} != {REF.findall(a)}")
            if re.search(r"(?<!\\)'", b) and not (b.startswith('"') and b.endswith('"')):
                problems.append(f"APÓSTROFO sem escape em {key[1]}")
    for key in pt:
        if key not in en:
            problems.append(f"SOBRA   {key[0]} {key[1]} (não existe em values/)")
    return problems


def raw_apostrophes(path):
    """ElementTree já decodifica; confere o texto cru por ' sem barra."""
    bad = []
    for n, line in enumerate(Path(path).read_text(encoding="utf-8").splitlines(), 1):
        if "<!--" in line:
            continue
        m = re.search(r">([^<]*)<", line)
        if m and re.search(r"(?<!\\)'", m.group(1)) and not m.group(1).startswith('"'):
            bad.append(f"APÓSTROFO sem escape {path}:{n}")
        if m and re.search(r'(?<!\\)"', m.group(1)) and not (m.group(1).startswith('"') and m.group(1).endswith('"')):
            bad.append(f"ASPAS sem escape (o Android as remove) {path}:{n}")
    return bad


def main():
    total = 0
    for src, dst in PAIRS:
        probs = check_pair(src, dst) + raw_apostrophes(dst)
        print(f"{dst}: {'ok' if not probs else str(len(probs)) + ' problema(s)'}")
        for p in probs:
            print("   ", p)
        total += len(probs)
    sys.exit(1 if total else 0)


if __name__ == "__main__":
    main()
