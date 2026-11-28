package org.example.documind.dto.document;

import org.springframework.core.io.Resource;

/** An uploaded file ready to be sent to the browser, with the name and type it was uploaded with. */
public record StoredFile(Resource resource, String filename, String contentType, long size) {}
