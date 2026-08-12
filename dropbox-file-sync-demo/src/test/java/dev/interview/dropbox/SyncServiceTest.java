package dev.interview.dropbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.interview.dropbox.model.Models.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SyncServiceTest {
    @TempDir Path temp;
    SyncService store;
    @BeforeEach void setup() throws Exception { store=new SyncService(new ObjectMapper().findAndRegisterModules(),temp.toString()); }

    private java.util.Map<String,Object> upload(byte[] bytes,String name,long base,String fileId,String device,String operation)throws Exception{
        String hash=SyncService.sha256(bytes); ChunkRef chunk=new ChunkRef(hash,bytes.length);
        if(!((List<?>)store.plan(List.of(chunk)).get("missingChunks")).isEmpty())store.putChunk(hash,bytes);
        return store.commit(new CommitRequest(fileId,name,"root",base,hash,List.of(chunk),device),operation);
    }
    @Test void chunksArePlannedAndDeduplicated()throws Exception{byte[] b="same".getBytes();String h=SyncService.sha256(b);ChunkRef c=new ChunkRef(h,b.length);assertEquals(1,((List<?>)store.plan(List.of(c)).get("missingChunks")).size());assertEquals(false,store.putChunk(h,b).get("deduplicated"));assertTrue(((List<?>)store.plan(List.of(c)).get("missingChunks")).isEmpty());assertEquals(true,store.putChunk(h,b).get("deduplicated"));}
    @Test void createUpdateAndOldVersionDownload()throws Exception{var first=upload("v1".getBytes(),"a.txt",0,null,"laptop","create");String id=(String)first.get("fileId");var second=upload("v2".getBytes(),"a.txt",1,id,"laptop","update");assertEquals(2L,second.get("version"));assertArrayEquals("v2".getBytes(),store.download(id,null).bytes());assertArrayEquals("v1".getBytes(),store.download(id,1L).bytes());}
    @Test void staleUpdatePreservesConflictCopy()throws Exception{var first=upload("v1".getBytes(),"notes.txt",0,null,"laptop","create");String id=(String)first.get("fileId");upload("laptop".getBytes(),"notes.txt",1,id,"laptop","update");var conflict=upload("phone".getBytes(),"notes.txt",1,id,"phone","conflict");assertEquals(true,conflict.get("conflict"));assertNotEquals(id,conflict.get("fileId"));assertTrue(((String)conflict.get("name")).contains("conflict from phone"));assertEquals(2,store.files().size());}
    @Test void retriesAreIdempotent()throws Exception{var first=upload("data".getBytes(),"a",0,null,"laptop","same");var second=upload("data".getBytes(),"a",0,null,"laptop","same");assertEquals(first.get("fileId"),second.get("fileId"));assertEquals(true,second.get("idempotentReplay"));assertEquals(1,store.stats().get("versions"));}
    @Test void missingChunksCannotCommit(){byte[] b="missing".getBytes();String h=SyncService.sha256(b);ApiException e=assertThrows(ApiException.class,()->store.commit(new CommitRequest(null,"x",null,0,h,List.of(new ChunkRef(h,b.length)),"d"),"op"));assertEquals("MISSING_CHUNK",e.code());}
    @Test void renameDeleteAndPaginate()throws Exception{var first=upload("data".getBytes(),"a",0,null,"d","create");String id=(String)first.get("fileId");store.move(id,new MoveRequest("b","archive",1),"move");store.delete(id,1,"delete");var page=store.changes(0,2);assertEquals(true,page.get("hasMore"));assertEquals(2,((List<?>)page.get("events")).size());assertEquals(true,store.files().get(0).get("tombstone"));}
    @Test void staleDeleteRejected()throws Exception{var first=upload("v1".getBytes(),"a",0,null,"d","create");String id=(String)first.get("fileId");upload("v2".getBytes(),"a",1,id,"d","update");assertEquals("STALE_VERSION",assertThrows(ApiException.class,()->store.delete(id,1,"delete")).code());}
    @Test void stateSurvivesRestart()throws Exception{var first=upload("durable".getBytes(),"a",0,null,"d","create");var restarted=new SyncService(new ObjectMapper().findAndRegisterModules(),temp.toString());assertArrayEquals("durable".getBytes(),restarted.download((String)first.get("fileId"),null).bytes());}
}
