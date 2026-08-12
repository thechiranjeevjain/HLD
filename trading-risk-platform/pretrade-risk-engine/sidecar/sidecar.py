import json, os, urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/runtime": self.send_error(404); return
        request=urllib.request.Request(os.getenv("ENGINE_URL","http://engine:8090")+"/api/internal/runtime")
        with urllib.request.urlopen(request,timeout=2) as response: body=response.read()
        self.send_response(200);self.send_header("Content-Type","application/json");self.end_headers();self.wfile.write(body)
HTTPServer(("0.0.0.0",8091),Handler).serve_forever()

