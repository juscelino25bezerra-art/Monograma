#!/usr/bin/env python3
"""Imprime as capturas de build/screens no log, em JPEG reduzido e base64.

Os artefatos do Actions nem sempre podem ser baixados de onde a revisão acontece; o log pode.
Cada imagem vira linhas "SCREEN <nome> <pedaço>" entre "SCREEN-BEGIN <nome>" e "SCREEN-END <nome>".
Uso: python3 tools/screens/print_screens.py build/screens
"""
import base64
import io
import sys
from pathlib import Path

from PIL import Image

WIDTH = 540
CHUNK = 3000


def main(folder):
    paths = sorted(Path(folder).glob('*.png'))
    if not paths:
        print('nenhuma captura em', folder)
        return
    for path in paths:
        try:
            image = Image.open(path).convert('RGB')
        except Exception as error:  # captura vazia ou cortada
            print('SCREEN-SKIP', path.stem, error)
            continue
        if image.width > WIDTH:
            image = image.resize((WIDTH, round(image.height * WIDTH / image.width)))
        buffer = io.BytesIO()
        image.save(buffer, 'JPEG', quality=75)
        data = base64.b64encode(buffer.getvalue()).decode()
        print('SCREEN-BEGIN', path.stem)
        for i in range(0, len(data), CHUNK):
            print('SCREEN', path.stem, data[i:i + CHUNK])
        print('SCREEN-END', path.stem)


if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'build/screens')
