package dev.interview.dropbox;

import dev.interview.dropbox.model.Models.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
public class SyncController {
    private final SyncService service;
    public SyncController(SyncService service){ this.service=service; }

    @PostMapping("/uploads/plan") public Map<String,Object> plan(@RequestBody UploadPlanRequest body){ return service.plan(body.chunks()==null?List.of():body.chunks()); }
    @PutMapping(value="/chunks/{hash}",consumes=MediaType.ALL_VALUE) public ResponseEntity<Map<String,Object>> chunk(@PathVariable String hash,@RequestBody byte[] bytes)throws IOException{return ResponseEntity.status(201).body(service.putChunk(hash,bytes));}
    @PostMapping("/commits") public ResponseEntity<Map<String,Object>> commit(@RequestBody CommitRequest body,@RequestHeader(value="Idempotency-Key",required=false)String operation)throws IOException{return ResponseEntity.status(201).body(service.commit(body,operation));}
    @GetMapping("/files") public Map<String,Object> files(){return Map.of("files",service.files());}
    @GetMapping("/changes") public Map<String,Object> changes(@RequestParam(defaultValue="0")long cursor,@RequestParam(defaultValue="100")int limit){return service.changes(cursor,Math.max(1,Math.min(limit,1000)));}
    @GetMapping("/stats") public Map<String,Object> stats()throws IOException{return service.stats();}
    @GetMapping("/files/{id}/download") public ResponseEntity<byte[]> download(@PathVariable String id,@RequestParam(required=false)Long version)throws IOException{var d=service.download(id,version);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+d.name().replace("\"","")+"\"").header("X-File-Version",Long.toString(d.version())).contentType(MediaType.APPLICATION_OCTET_STREAM).body(d.bytes());}
    @PostMapping("/files/{id}/move") public Map<String,Object> move(@PathVariable String id,@RequestBody MoveRequest body,@RequestHeader(value="Idempotency-Key",required=false)String operation)throws IOException{return service.move(id,body,operation);}
    @DeleteMapping("/files/{id}") public Map<String,Object> delete(@PathVariable String id,@RequestParam long baseVersion,@RequestHeader(value="Idempotency-Key",required=false)String operation)throws IOException{return service.delete(id,baseVersion,operation);}
    @PostMapping("/admin/reset") public Map<String,Object> reset()throws IOException{service.reset();return Map.of("reset",true);}

    @ExceptionHandler(ApiException.class) ResponseEntity<Map<String,Object>> apiError(ApiException e){var body=new LinkedHashMap<String,Object>();body.put("error",e.code());body.put("message",e.getMessage());if(e.details()!=null)body.put("details",e.details());return ResponseEntity.status(e.status()).body(body);}
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> error(Exception e){return ResponseEntity.status(500).body(Map.of("error","INTERNAL_ERROR","message",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
}
