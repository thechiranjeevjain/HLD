package dev.interview.dropbox.model;

import java.util.ArrayList;
import java.util.List;

public final class Models {
    private Models() {}
    public record ChunkRef(String hash, long size) {}
    public record UploadPlanRequest(List<ChunkRef> chunks) {}
    public record CommitRequest(String fileId, String name, String parentId, long baseVersion, String fileHash,
                                List<ChunkRef> chunks, String deviceId) {}
    public record MoveRequest(String name, String parentId, long baseVersion) {}
    public record Version(long version, long baseVersion, String fileHash, long size,
                          List<ChunkRef> chunks, String deviceId, String createdAt) {}
    public static class FileRecord {
        public String fileId;
        public String parentId;
        public String name;
        public long latestVersion;
        public boolean tombstone;
        public List<Version> versions = new ArrayList<>();
        public FileRecord() {}
        public FileRecord(String fileId, String parentId, String name) { this.fileId=fileId; this.parentId=parentId; this.name=name; }
    }
    public record ChangeEvent(long sequence, String type, String fileId, String name, String parentId,
                              long version, boolean tombstone, String serverTime) {}
    public static class State {
        public long nextSequence = 1;
        public java.util.LinkedHashMap<String, FileRecord> files = new java.util.LinkedHashMap<>();
        public List<ChangeEvent> events = new ArrayList<>();
        public java.util.HashMap<String, java.util.Map<String,Object>> operations = new java.util.HashMap<>();
    }
}
