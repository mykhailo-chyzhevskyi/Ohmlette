"""Local HTTP backend for laboratory 2. No cloud AI or GPU required."""
import argparse
import base64
import hashlib
import io
import ipaddress
import json
import re
import socket
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

from pypdf import PdfReader
from classifier import Classifier

MAX_PDF = 15 * 1024 * 1024
STORE = Path(__file__).parent / "storage"
MODEL = Classifier()

def validate_url(url):
    parsed = urlsplit(url)
    if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password or parsed.port not in (None, 443):
        raise ValueError("Потрібне публічне HTTPS-посилання на PDF")
    addresses = socket.getaddrinfo(parsed.hostname, 443, type=socket.SOCK_STREAM)
    if not addresses or any(not ipaddress.ip_address(a[4][0]).is_global for a in addresses):
        raise ValueError("Посилання на локальні адреси заборонені")

class PublicRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        validate_url(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)

def download(url):
    validate_url(url)
    opener = urllib.request.build_opener(PublicRedirect())
    with opener.open(urllib.request.Request(url, headers={"User-Agent": "Ohmlette-Lab2/0.1"}), timeout=25) as response:
        data = response.read(MAX_PDF + 1)
    return data

def make_card(data, part):
    if not part or len(part) > 100 or not re.fullmatch(r"[A-Za-z0-9_. /+()-]+", part):
        raise ValueError("Вкажіть артикул латиницею (до 100 символів)")
    if len(data) > MAX_PDF or not data.lstrip().startswith(b"%PDF-"):
        raise ValueError("Потрібен PDF розміром до 15 МіБ")
    reader = PdfReader(io.BytesIO(data))
    if reader.is_encrypted:
        raise ValueError("Захищені паролем PDF поки не підтримуються")
    if not 1 <= len(reader.pages) <= 100:
        raise ValueError("Підтримуються PDF до 100 сторінок")
    pages = [(i+1, p.extract_text() or "") for i, p in enumerate(reader.pages)]
    if sum(len(t.strip()) for _, t in pages) < 100:
        raise ValueError("Не знайдено текстового шару. OCR у цій версії немає")
    # First page normally describes the device; subsequent table terms can bias classification.
    classification = MODEL.predict(pages[0][1][:12000])
    needle = re.sub(r"[^A-Z0-9]", "", part.upper())
    matches = []
    for page, text in pages:
        lines = text.splitlines()
        for idx, line in enumerate(lines):
            compact = re.sub(r"[^A-Z0-9]", "", line.upper())
            if needle in compact:
                matches.append({"page": page, "quote": "\n".join(lines[max(0,idx-1):idx+3])[:1800], "status": "unverified"})
                break
    doc_id = hashlib.sha256(data).hexdigest()
    STORE.mkdir(exist_ok=True)
    (STORE / f"{doc_id}.pdf").write_bytes(data)
    return {
        "schemaVersion": 1, "partNumber": part, "classification": classification,
        "documentId": doc_id, "pageCount": len(pages), "evidence": matches[:12],
        "partFound": bool(matches), "status": "unverified",
        "summary": "Чернетка картки. Тип визначено локальним ML-класифікатором. Знайдені фрагменти потрібно перевірити в PDF.",
        "parameters": [],
        "limitations": ["Min/typ/max та умови ще не розібрано: показано оригінальні фрагменти, а не готові числові параметри.", "Модель навчена на малому наборі прикладів і може помилитися."]
    }

class Handler(BaseHTTPRequestHandler):
    def send_json(self, status, payload):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path == "/health":
            return self.send_json(200, {"status": "ok", "ai": "multinomial-naive-bayes-v1"})
        match = re.fullmatch(r"/documents/([a-f0-9]{64})\.pdf", self.path.split("?")[0])
        if match:
            file = STORE / f"{match[1]}.pdf"
            if file.exists():
                data = file.read_bytes()
                self.send_response(200)
                self.send_header("Content-Type", "application/pdf")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
                return
        self.send_json(404, {"error": "Не знайдено"})

    def do_POST(self):
        if self.path != "/v1/cards":
            return self.send_json(404, {"error": "Не знайдено"})
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 < length <= 22 * 1024 * 1024:
                raise ValueError("Некоректний розмір запиту")
            payload = json.loads(self.rfile.read(length))
            if not isinstance(payload, dict):
                raise ValueError("Очікується JSON-об'єкт")
            part = payload.get("partNumber", "").strip()
            url, content = payload.get("url"), payload.get("pdfBase64")
            if bool(url) == bool(content):
                raise ValueError("Оберіть один спосіб: посилання або файл")
            data = download(url) if url else base64.b64decode(content, validate=True)
            self.send_json(200, make_card(data, part))
        except (ValueError, TypeError, AttributeError) as error:
            self.send_json(400, {"error": str(error)})
        except Exception:
            self.send_json(422, {"error": "Не вдалося завантажити або прочитати PDF. Спробуйте вибрати збережений файл."})

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", default=8000, type=int)
    args = parser.parse_args()
    print(f"Ohmlette backend: http://{args.host}:{args.port}", flush=True)
    ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()
