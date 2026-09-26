"""Monta todas as saídas do Revelador a partir de src/.
- index.html              -> artifact da versão PC (sem esqueleto)
- ios/iphone.html         -> artifact da versão iPhone (sem esqueleto)
- dist/pc.html            -> PC autônomo (abre direto no navegador)
- dist/pwa/               -> app instalável do iPhone (GitHub Pages)
"""
import os, shutil, json, sys
VERSION = sys.argv[1] if len(sys.argv) > 1 else "1.1"
R = os.path.dirname(os.path.abspath(__file__))
rd = lambda p: open(os.path.join(R, p), encoding="utf-8").read()

def inject(s):
    for k, f in [("PRESETS", "presets.js"), ("ENGINE", "engine.js"), ("SAMPLE", "sample.js"),
                 ("SHARPEN", "sharpen.js"), ("FIXUI", "fix-ui.js")]:
        s = s.replace(f"/*{k}*/", rd("src/" + f))
    return s

def standalone(body, extra_head="", html_class=""):
    title, rest = body.split("\n", 1)
    i = rest.index("</style>") + len("</style>")
    cls = f' class="{html_class}"' if html_class else ""
    return (f'<!doctype html>\n<html lang="pt-BR"{cls}>\n<head>\n<meta charset="utf-8">\n'
            '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n'
            f'<meta name="generator" content="Revelador v{VERSION}">\n{title}\n{extra_head}'
            + rest[:i] + "\n</head>\n<body>\n" + rest[i:] + "\n</body>\n</html>\n")

pc = inject(rd("src/pc.html")).replace("v1.1 ·", f"v{VERSION} ·")
ios = inject(rd("src/iphone.html"))
for chk, s in [("pc", pc), ("ios", ios)]:
    assert "/*" + "SHARPEN*/" not in s and "/*FIXUI*/" not in s, chk
open(os.path.join(R, "index.html"), "w", encoding="utf-8").write(pc)
open(os.path.join(R, "ios/iphone.html"), "w", encoding="utf-8").write(ios)

os.makedirs(os.path.join(R, "dist/pwa"), exist_ok=True)
open(os.path.join(R, "dist/pc.html"), "w", encoding="utf-8").write(standalone(pc))
head = ('<meta name="apple-mobile-web-app-capable" content="yes">\n<meta name="mobile-web-app-capable" content="yes">\n'
        '<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">\n'
        '<meta name="apple-mobile-web-app-title" content="Revelador">\n<meta name="theme-color" content="#141517">\n'
        '<link rel="apple-touch-icon" href="icon-180.png">\n<link rel="manifest" href="manifest.webmanifest">\n')
pwa = standalone(ios, head, "standalone standalone-build").replace("<title>Revelador iPhone</title>", "<title>Revelador</title>")
open(os.path.join(R, "dist/pwa/index.html"), "w", encoding="utf-8").write(pwa)
for f in ["icon-180.png", "icon-192.png", "icon-512.png", "manifest.webmanifest"]:
    shutil.copy(os.path.join(R, "ios/pwa", f), os.path.join(R, "dist/pwa", f))
open(os.path.join(R, "dist/pwa/sw.js"), "w").write(f'''const V="revelador-v{VERSION}";
const FILES=["./","index.html","pc.html","manifest.webmanifest","icon-180.png","icon-192.png","icon-512.png"];
self.addEventListener("install",e=>{{e.waitUntil(caches.open(V).then(c=>c.addAll(FILES)));self.skipWaiting();}});
self.addEventListener("activate",e=>{{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==V).map(k=>caches.delete(k)))));self.clients.claim();}});
self.addEventListener("fetch",e=>{{if(e.request.method!=="GET")return;
  e.respondWith(fetch(e.request).then(r=>{{const cp=r.clone();caches.open(V).then(c=>c.put(e.request,cp));return r;}}).catch(()=>caches.match(e.request,{{ignoreSearch:true}})));}});
''')
shutil.copy(os.path.join(R, "dist/pc.html"), os.path.join(R, "dist/pwa/pc.html"))
print("build ok", VERSION, len(pc), len(ios))
