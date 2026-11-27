#!/usr/bin/env python3
import hashlib, re, struct, sys, zipfile, zlib
from pathlib import Path

src, dst = map(Path, sys.argv[1:3])
OLD, NEW = "com.miui.calculator", "com.mathengine"
ANDROID_NS = "http://schemas.android.com/apk/res/android"

def enc16(n):
    return struct.pack("<H", n) if n < 0x8000 else struct.pack("<HH", 0x8000 | (n >> 16), n & 0xffff)

def dec16(b, p):
    n = struct.unpack_from("<H", b, p)[0]; p += 2
    if n & 0x8000:
        n = ((n & 0x7fff) << 16) | struct.unpack_from("<H", b, p)[0]; p += 2
    return n, p

def parse_pool(b):
    t, hs, size = struct.unpack_from("<HHI", b, 8)
    if t != 1 or hs != 28: raise ValueError("Unexpected AXML string pool")
    count, styles, flags, start, style_start = struct.unpack_from("<IIIII", b, 16)
    if styles: raise ValueError("Unexpected styled manifest string pool")
    offs = struct.unpack_from("<" + "I"*count, b, 36)
    vals = []
    for off in offs:
        p = 8 + start + off
        if flags & 0x100: raise ValueError("Unexpected UTF-8 string pool")
        n, p = dec16(b, p)
        vals.append(b[p:p+n*2].decode("utf-16le"))
    return size, flags, vals

def make_pool(flags, vals):
    data, offs = bytearray(), []
    for s in vals:
        offs.append(len(data)); raw = s.encode("utf-16le")
        data += enc16(len(raw)//2) + raw + b"\0\0"
    start = 28 + 4*len(vals)
    chunk = bytearray(struct.pack("<HHI", 1, 28, 0))
    chunk += struct.pack("<IIIII", len(vals), 0, flags, start, 0)
    chunk += struct.pack("<" + "I"*len(offs), *offs) + data
    chunk += b"\0" * ((-len(chunk)) % 4)
    struct.pack_into("<I", chunk, 4, len(chunk))
    return bytes(chunk)

def get_attr_values(xml, strings):
    pos, result = 8, []
    while pos < len(xml):
        typ, hs, size = struct.unpack_from("<HHI", xml, pos)
        if size < hs or pos + size > len(xml): raise ValueError("Invalid AXML chunk")
        if typ == 0x0102:
            ext = pos + 16
            ns, name, attr_start, attr_size, count = struct.unpack_from("<IIHHH", xml, ext)
            tag = strings[name]
            attrs = {}
            base = ext + attr_start
            for i in range(count):
                a = base + i*attr_size
                ans, aname, raw = struct.unpack_from("<III", xml, a)
                vs, res0, dtype, value = struct.unpack_from("<HBBI", xml, a+12)
                attrs[strings[aname]] = strings[value] if dtype == 3 else value
            result.append((tag, attrs))
        pos += size
    return result

def patch_manifest(xml):
    if struct.unpack_from("<H", xml, 0)[0] != 3: raise ValueError("Not binary AndroidManifest.xml")
    old_size, flags, vals = parse_pool(xml)
    repl = {
        OLD: NEW,
        OLD + ".permission.SHOW_FLOAT_WINDOW": NEW + ".permission.SHOW_FLOAT_WINDOW",
        OLD + ".fileprovider": NEW + ".fileprovider",
        OLD + ".widget.provider": NEW + ".widget.provider",
        OLD + ".androidx-startup": NEW + ".androidx-startup",
    }
    counts = {k: vals.count(k) for k in repl}
    if any(v == 0 for v in counts.values()): raise ValueError(f"Missing expected manifest values: {counts}")
    vals = [repl.get(s, s) for s in vals]
    pool = make_pool(flags, vals)
    out = bytearray(xml[:8] + pool + xml[8+old_size:])
    struct.pack_into("<I", out, 4, len(out))
    items = get_attr_values(bytes(out), vals)
    manifest = next(a for t,a in items if t == "manifest")
    authorities = [a["authorities"] for t,a in items if t == "provider" and "authorities" in a]
    if manifest.get("package") != NEW: raise ValueError("Manifest package mismatch")
    expected = {NEW+".fileprovider", NEW+".widget.provider", NEW+".androidx-startup"}
    if not expected.issubset(set(authorities)): raise ValueError("Provider authority patch incomplete")
    return bytes(out), counts

def uleb(n):
    out=bytearray()
    while True:
        x=n & 127; n >>= 7
        out.append(x | (128 if n else 0))
        if not n: return bytes(out)

def read_uleb(b,p):
    n=shift=0
    while True:
        x=b[p]; p+=1; n |= (x&127)<<shift
        if x < 128: return n,p
        shift += 7

def patch_dex(dex):
    if not dex.startswith(b"dex\n"): raise ValueError("Not DEX")
    count, ids_off = struct.unpack_from("<II", dex, 56)
    offsets = [struct.unpack_from("<I", dex, ids_off+4*i)[0] for i in range(count)]
    ordered = sorted(set(offsets)); out=bytearray(dex); hits=0
    target = "content://" + OLD + ".widget.provider"
    replacement = ("content://" + NEW + ".widget.provider").encode()
    for off in offsets:
        units, p = read_uleb(dex, off)
        end = dex.find(b"\0", p)
        if end < 0: raise ValueError("Bad DEX string")
        if dex[p:end].decode("utf-8") == target:
            nexts = [x for x in ordered if x > off]
            cap = (min(nexts) if nexts else len(dex)) - off
            item = uleb(len(replacement)) + replacement + b"\0"
            if len(item) > cap: raise ValueError("DEX replacement exceeds string slot")
            out[off:off+cap] = item + b"\0"*(cap-len(item))
            hits += 1
    if hits != 1: raise ValueError(f"Expected 1 widget provider DEX string, found {hits}")
    out[12:32] = hashlib.sha1(out[32:]).digest()
    struct.pack_into("<I", out, 8, zlib.adler32(out[12:]) & 0xffffffff)
    return bytes(out)

with zipfile.ZipFile(src) as zin:
    manifest, replacements = patch_manifest(zin.read("AndroidManifest.xml"))
    dex_names = [n for n in zin.namelist() if re.fullmatch(r"classes(?:\d+)?\.dex", n)]
    patched_dex, hits = {}, 0
    for name in dex_names:
        data = zin.read(name)
        new = patch_dex(data)
        if new != data: patched_dex[name] = new; hits += 1
    if hits != 1: raise ValueError("Expected exactly one patched DEX file")
    with zipfile.ZipFile(dst, "w", allowZip64=True) as zout:
        for info in zin.infolist():
            name = info.filename
            if name.upper() == "META-INF/MANIFEST.MF" or (name.startswith("META-INF/") and re.fullmatch(r"META-INF/[^/]+\.(SF|RSA|DSA|EC)", name, re.I)):
                continue
            data = manifest if name == "AndroidManifest.xml" else patched_dex.get(name)
            if data is None: data = zin.read(name)
            zout.writestr(info, data)
with zipfile.ZipFile(dst) as z:
    bad = z.testzip()
    if bad: raise ValueError("ZIP CRC failure: " + bad)
print("PACKAGE=com.mathengine")
print("VERSION_EXPECTED=17.4.1 / 317000041")
print("MANIFEST_STRING_REPLACEMENTS=" + repr(replacements))
print("DEX_PROVIDER_STRING_PATCHED=1")
print("ZIP_ENTRIES=" + str(len(zipfile.ZipFile(dst).namelist())))
print("OUTPUT_BYTES=" + str(dst.stat().st_size))
print("OUTPUT_SHA256=" + hashlib.sha256(dst.read_bytes()).hexdigest())
