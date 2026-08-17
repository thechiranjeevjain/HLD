package com.example.documents.storage;

public interface ObjectStoragePort {

    SignedUploadIntent createUploadIntent(String key, String contentType, long contentLengthBytes);
}
