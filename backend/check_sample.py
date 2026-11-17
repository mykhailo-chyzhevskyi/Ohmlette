"""Real HTTP integration check using the user-supplied PDF (download separately)."""
import base64
import json
import threading
import urllib.request
import urllib.error
from pathlib import Path
from http.server import ThreadingHTTPServer
from server import Handler

server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
base = f"http://127.0.0.1:{server.server_port}"
try:
    assert json.load(urllib.request.urlopen(base + "/health"))["status"] == "ok"
    payload = {"partNumber": "BZX84C2V4LT1G", "pdfBase64": base64.b64encode(Path("sample.pdf").read_bytes()).decode()}
    request = urllib.request.Request(base + "/v1/cards", data=json.dumps(payload).encode(), headers={"Content-Type": "application/json"})
    card = json.load(urllib.request.urlopen(request))
    assert card["classification"]["type"] == "diode", card
    assert card["partFound"] and any(item["page"] == 3 for item in card["evidence"]), card
    document = urllib.request.urlopen(base + "/documents/" + card["documentId"] + ".pdf").read()
    assert document == Path("sample.pdf").read_bytes()
    absent = {**payload, "partNumber": "NOT-A-REAL-PART-123"}
    missing = json.load(urllib.request.urlopen(urllib.request.Request(base + "/v1/cards", data=json.dumps(absent).encode(), headers={"Content-Type": "application/json"})))
    assert missing["partFound"] is False and missing["evidence"] == []
    invalid = urllib.request.Request(base + "/v1/cards", data=b'{}', headers={"Content-Type": "application/json"})
    try:
        urllib.request.urlopen(invalid)
        raise AssertionError("Expected HTTP 400")
    except urllib.error.HTTPError as error:
        assert error.code == 400
    Path("sample-card.json").write_text(json.dumps(card, ensure_ascii=False, indent=2), encoding="utf-8")
    print("HTTP integration OK: upload, classification, page evidence, PDF retrieval, invalid input")
finally:
    server.shutdown()
    server.server_close()
