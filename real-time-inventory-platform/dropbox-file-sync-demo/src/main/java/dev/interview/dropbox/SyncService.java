package dev.interview.dropbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.interview.dropbox.model.Models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class SyncService {
    private final ObjectMapper json;
    private final Path root;
    private final Path blobs;
    private final Path metadata;
    private State state;

    public SyncService(ObjectMapper json, @Value("${sync.data-dir:data}") String dataDir) throws IOException {
        this.json=json; this.root=Path.of(dataDir); this.blobs=root.resolve("blobs"); this.metadata=root.resolve("metadata.json");
        Files.createDirectories(blobs);
        this.state=Files.exists(metadata) ? json.readValue(metadata.toFile(), State.class) : new State();
    }

    public synchronized Map<String,Object> plan(List<ChunkRef> chunks) {
        var missing = chunks.stream().filter(c -> !Files.exists(blobs.resolve(c.hash())))
                .map(c -> Map.<String,Object>of("hash",c.hash(),"size",c.size(),"uploadUrl","/api/chunks/"+c.hash())).toList();
        return Map.of("uploadSessionId",UUID.randomUUID().toString(),"missingChunks",missing);
    }

    public synchronized Map<String,Object> putChunk(String expected, byte[] bytes) throws IOException {
        String actual=sha256(bytes);
        if (!actual.equals(expected)) throw new ApiException(HttpStatus.BAD_REQUEST,"HASH_MISMATCH","Chunk bytes do not match declared SHA-256",Map.of("actual",actual));
        Path target=blobs.resolve(expected); boolean deduplicated=Files.exists(target);
        if (!deduplicated) { Path temp=Files.createTempFile(root,"chunk-",".tmp"); Files.write(temp,bytes); moveAtomic(temp,target); }
        return Map.of("hash",expected,"size",bytes.length,"deduplicated",deduplicated);
    }

    public synchronized Map<String,Object> commit(CommitRequest request, String operationId) throws IOException {
        requireOperation(operationId);
        if (state.operations.containsKey(operationId)) return replay(operationId);
        if (request.name()==null || request.fileHash()==null || request.chunks()==null) throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","name, fileHash and chunks are required");
        verifyManifest(request.chunks(),request.fileHash());
        String fileId=request.fileId()==null ? UUID.randomUUID().toString() : request.fileId();
        FileRecord record=state.files.get(fileId); long latest=record==null?0:record.latestVersion;
        boolean conflict=record!=null && request.baseVersion()!=latest;
        String name=request.name();
        if (conflict) { name=conflictName(name,request.deviceId()); fileId=UUID.randomUUID().toString(); record=null; latest=0; }
        if (record==null) record=new FileRecord(fileId,defaultParent(request.parentId()),name);
        long version=latest+1;
        record.name=name; record.parentId=defaultParent(request.parentId()==null?record.parentId:request.parentId()); record.latestVersion=version; record.tombstone=false;
        long size=request.chunks().stream().mapToLong(ChunkRef::size).sum();
        record.versions.add(new Version(version,request.baseVersion(),request.fileHash(),size,List.copyOf(request.chunks()),defaultDevice(request.deviceId()),Instant.now().toString()));
        state.files.put(fileId,record);
        ChangeEvent event=event(conflict?"CONFLICT":version==1?"CREATE":"UPDATE",record);
        Map<String,Object> response=new LinkedHashMap<>();
        response.put("fileId",fileId); response.put("version",version); response.put("cursor",event.sequence()); response.put("conflict",conflict); response.put("name",name); response.put("idempotentReplay",false);
        state.operations.put(operationId,response); persist(); return response;
    }

    public synchronized Map<String,Object> move(String fileId, MoveRequest request, String operationId) throws IOException {
        requireOperation(operationId); if(state.operations.containsKey(operationId)) return replay(operationId);
        FileRecord record=live(fileId); checkVersion(record,request.baseVersion(),"Rename");
        record.name=request.name(); record.parentId=defaultParent(request.parentId()); ChangeEvent event=event("RENAME",record);
        Map<String,Object> response=linked("fileId",fileId,"name",record.name,"parentId",record.parentId,"cursor",event.sequence(),"idempotentReplay",false);
        state.operations.put(operationId,response); persist(); return response;
    }

    public synchronized Map<String,Object> delete(String fileId, long baseVersion, String operationId) throws IOException {
        requireOperation(operationId); if(state.operations.containsKey(operationId)) return replay(operationId);
        FileRecord record=live(fileId); checkVersion(record,baseVersion,"Delete"); record.tombstone=true; ChangeEvent event=event("DELETE",record);
        Map<String,Object> response=linked("fileId",fileId,"tombstone",true,"cursor",event.sequence(),"idempotentReplay",false);
        state.operations.put(operationId,response); persist(); return response;
    }

    public synchronized List<Map<String,Object>> files() {
        return state.files.values().stream().map(f->{ Version v=f.versions.get(f.versions.size()-1); return linked("fileId",f.fileId,"parentId",f.parentId,"name",f.name,"latestVersion",f.latestVersion,"tombstone",f.tombstone,"size",v.size(),"fileHash",v.fileHash(),"versionCount",f.versions.size()); }).toList();
    }

    public synchronized Map<String,Object> changes(long cursor, int limit) {
        List<ChangeEvent> events=state.events.stream().filter(e->e.sequence()>cursor).limit(limit).toList();
        long next=events.isEmpty()?cursor:events.get(events.size()-1).sequence();
        return Map.of("events",events,"cursor",next,"hasMore",state.events.stream().anyMatch(e->e.sequence()>next));
    }

    public synchronized Download download(String fileId, Long requestedVersion) throws IOException {
        FileRecord record=state.files.get(fileId); if(record==null) throw new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","File does not exist");
        Version version=requestedVersion==null?record.versions.get(record.versions.size()-1):record.versions.stream().filter(v->v.version()==requestedVersion).findFirst().orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"VERSION_NOT_FOUND","Version does not exist"));
        var output=new java.io.ByteArrayOutputStream(); for(ChunkRef chunk:version.chunks()) output.write(Files.readAllBytes(blobs.resolve(chunk.hash())));
        byte[] data=output.toByteArray(); if(!sha256(data).equals(version.fileHash())) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,"INTEGRITY_FAILURE","Stored file failed integrity verification");
        return new Download(data,record.name,version.version());
    }

    public synchronized Map<String,Object> stats() throws IOException {
        long logical=state.files.values().stream().flatMap(f->f.versions.stream()).mapToLong(Version::size).sum();
        long physical; try(var paths=Files.list(blobs)){ physical=paths.filter(Files::isRegularFile).mapToLong(p->{try{return Files.size(p);}catch(IOException e){throw new RuntimeException(e);}}).sum(); }
        return linked("files",state.files.size(),"versions",state.files.values().stream().mapToInt(f->f.versions.size()).sum(),"events",state.events.size(),"logicalBytes",logical,"physicalBytes",physical,"dedupeSavedBytes",Math.max(0,logical-physical));
    }

    public synchronized void reset() throws IOException {
        state=new State(); persist(); try(var paths=Files.list(blobs)){ for(Path path:paths.toList()) Files.deleteIfExists(path); }
    }

    private void verifyManifest(List<ChunkRef> chunks,String fileHash) throws IOException {
        var output=new java.io.ByteArrayOutputStream();
        for(ChunkRef chunk:chunks){ Path p=blobs.resolve(chunk.hash()); if(!Files.exists(p)) throw new ApiException(HttpStatus.CONFLICT,"MISSING_CHUNK","Commit references a chunk not uploaded",Map.of("hash",chunk.hash())); byte[] data=Files.readAllBytes(p); if(data.length!=chunk.size()||!sha256(data).equals(chunk.hash())) throw new ApiException(HttpStatus.CONFLICT,"INVALID_CHUNK","Stored chunk failed verification",Map.of("hash",chunk.hash())); output.write(data); }
        if(!sha256(output.toByteArray()).equals(fileHash)) throw new ApiException(HttpStatus.BAD_REQUEST,"FILE_HASH_MISMATCH","Manifest does not produce declared file hash");
    }
    private ChangeEvent event(String type,FileRecord record){ var event=new ChangeEvent(state.nextSequence++,type,record.fileId,record.name,record.parentId,record.latestVersion,record.tombstone,Instant.now().toString()); state.events.add(event); return event; }
    private FileRecord live(String id){ FileRecord r=state.files.get(id); if(r==null||r.tombstone) throw new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND","Live file does not exist"); return r; }
    private void checkVersion(FileRecord r,long base,String operation){ if(base!=r.latestVersion) throw new ApiException(HttpStatus.CONFLICT,"STALE_VERSION",operation+" was based on a stale version",Map.of("latestVersion",r.latestVersion)); }
    private void requireOperation(String id){ if(id==null||id.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST,"IDEMPOTENCY_KEY_REQUIRED","Mutation requires an Idempotency-Key header"); }
    private Map<String,Object> replay(String id){ var result=new LinkedHashMap<>(state.operations.get(id)); result.put("idempotentReplay",true); return result; }
    private void persist() throws IOException { Files.createDirectories(root); Path temp=Files.createTempFile(root,"metadata-",".tmp"); json.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(),state); moveAtomic(temp,metadata); }
    private static void moveAtomic(Path from,Path to)throws IOException{ try{Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);} }
    private static String conflictName(String name,String device){ int dot=name.lastIndexOf('.'); String suffix=dot>0?name.substring(dot):""; String stem=dot>0?name.substring(0,dot):name; return stem+" (conflict from "+defaultDevice(device)+")"+suffix; }
    private static String defaultParent(String value){ return value==null||value.isBlank()?"root":value; }
    private static String defaultDevice(String value){ return value==null||value.isBlank()?"device":value; }
    public static String sha256(byte[] bytes){ try{ return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }catch(Exception e){ throw new IllegalStateException(e); } }
    private static Map<String,Object> linked(Object... values){ var m=new LinkedHashMap<String,Object>(); for(int i=0;i<values.length;i+=2)m.put((String)values[i],values[i+1]); return m; }
    public record Download(byte[] bytes,String name,long version) {}
}
