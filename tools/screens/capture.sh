#!/usr/bin/env bash
# Capturas das telas do Monograma num emulador (workflow monograma-telas.yml).
# Navega pelo texto dos botões (uiautomator), não por coordenadas, e salva PNGs em build/screens.
# Nunca falha o job por uma tela que não abriu: a captura que faltar já diz o que houve.
set -u
PKG=io.github.juscelino25bezerra.monograma.debug  # o build de debug tem o sufixo .debug
DIR=smarttubetv/build/outputs/renamed_apks/stmobileDebug
APK=$(ls "$DIR"/*universal*.apk 2>/dev/null | head -1)
[ -z "$APK" ] && APK=$(ls "$DIR"/*.apk | head -1)
OUT=build/screens
mkdir -p "$OUT"

shot() { sleep "${2:-3}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "captura: $1"; report; }

# Toca no elemento cujo texto é $1. $2=last escolhe o mais baixo da tela (rótulos da barra).
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" "${2:-first}" <<'PY'
import re, sys
text, which = sys.argv[1], sys.argv[2]
xml = open('/tmp/ui.xml', encoding='utf-8').read()
hits = []
for node in re.findall(r'<node [^>]*>', xml):
    if 'text="%s"' % text in node:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
        if b:
            x1, y1, x2, y2 = map(int, b.groups())
            hits.append(((x1 + x2) // 2, (y1 + y2) // 2))
if hits:
    x, y = max(hits, key=lambda h: h[1]) if which == 'last' else hits[0]
    print(x, y)
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; echo "toque: $1 ($xy)"; else echo "não achei: $1"; fi
}

launch() {
  adb shell am force-stop $PKG
  # Pelo ícone que estiver ativo (Grafite ou Coral), como o launcher faria.
  adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
}

# O que está na frente, e qualquer falha do app desde a última chamada.
report() {
  echo "frente: $(adb shell dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity' | tr -s ' ')"
  adb logcat -d -b crash | tail -60
  adb logcat -d -s AndroidRuntime:E | tail -60
  adb logcat -c
}

adb wait-for-device
# Português do Brasil no aparelho todo (o app segue o idioma do sistema por padrão).
adb root >/dev/null 2>&1; sleep 3; adb wait-for-device
adb shell "setprop persist.sys.locale pt-BR; setprop ctl.restart zygote"
sleep 20; adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
sleep 10
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard >/dev/null 2>&1

adb install -r -g "$APK"
adb logcat -c
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

# --- Tema escuro ---
adb shell cmd uimode night yes
launch
shot 01_inicio 30
tap_text "Downloads" last; shot 02_downloads 5
tap_text "Você" last; shot 03_voce 5
tap_text "Configurações"; shot 04_configuracoes 5
tap_text "Aparência e idioma"; shot 05_aparencia 4
adb shell input swipe 540 1900 540 700 900; shot 05b_aparencia_baixo 3
adb shell input keyevent KEYCODE_BACK; sleep 2
adb shell input swipe 540 1900 540 600 900; sleep 2
shot 04b_configuracoes_baixo 2
tap_text "Avançado"; shot 06_avancado 4
adb shell input swipe 540 1900 540 700 900; shot 06b_avancado_baixo 3
adb shell input keyevent KEYCODE_BACK; sleep 1
adb shell input keyevent KEYCODE_BACK; sleep 2
tap_text "Início" last; sleep 2
adb shell am start -a android.intent.action.VIEW -d "https://www.youtube.com/watch?v=jNQXAC9IVRw" $PKG >/dev/null 2>&1
shot 07_video 30
adb shell input swipe 540 1800 540 900 900; shot 07b_video_baixo 3

# --- Ícone Coral: o app continua aberto e a tela inicial mostra o ícone novo ---
launch; sleep 15
tap_text "Você" last; sleep 3
tap_text "Configurações"; sleep 3
tap_text "Aparência e idioma"; sleep 3
tap_text "Coral"; shot 11_icone_coral 8
adb shell input keyevent KEYCODE_HOME; sleep 6
adb shell input swipe 540 1900 540 500 600; shot 12_launcher_coral 4
echo "aliases: $(adb shell dumpsys package $PKG | grep -E -A1 'enabledComponents|disabledComponents' | tr -s ' ' | tr '\n' ' ')"
adb shell input keyevent KEYCODE_HOME; sleep 2

# --- Tema claro ---
adb shell cmd uimode night no
launch
shot 08_inicio_claro 25
tap_text "Você" last; shot 09_voce_claro 5
tap_text "Configurações"; shot 10_configuracoes_claro 5
exit 0
