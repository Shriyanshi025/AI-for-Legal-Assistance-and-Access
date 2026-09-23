package com.legalassist.service.chunking;

import com.legalassist.entity.DocumentPage;
import com.legalassist.exception.ChunkingException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TextChunkingServiceImpl implements TextChunkingService {

    @Override
    public List<RawChunk> createChunks(List<DocumentPage> pages, int maxChunkSize, int overlapSize) {
        if (maxChunkSize <= 0) {
            throw new ChunkingException("maxChunkSize must be greater than 0");
        }
        if (overlapSize < 0) {
            throw new ChunkingException("overlapSize must be non-negative");
        }
        if (overlapSize >= maxChunkSize) {
            throw new ChunkingException("overlapSize must be strictly smaller than maxChunkSize");
        }
        if (pages == null || pages.isEmpty()) {
            return Collections.emptyList();
        }

        List<RawChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (DocumentPage page : pages) {
            String rawContent = page.getContent();
            if (rawContent == null || rawContent.isBlank()) {
                continue;
            }

            String text = rawContent.replace("\r\n", "\n").trim();
            if (text.isBlank()) {
                continue;
            }

            int len = text.length();
            if (len <= maxChunkSize) {
                chunks.add(new RawChunk(chunkIndex++, page.getPageNumber(), text, null, null));
                continue;
            }

            int start = 0;
            while (start < len) {
                int targetEnd = Math.min(start + maxChunkSize, len);
                int end = targetEnd;

                if (targetEnd < len) {
                    int minBoundarySearch = start + overlapSize + 1;
                    int bestBoundary = findBestBoundary(text, minBoundarySearch, targetEnd);
                    if (bestBoundary > minBoundarySearch) {
                        end = bestBoundary;
                    }
                }

                String chunkContent = text.substring(start, end).trim();
                if (!chunkContent.isBlank()) {
                    chunks.add(new RawChunk(chunkIndex++, page.getPageNumber(), chunkContent, null, null));
                }

                if (end >= len) {
                    break;
                }

                int nextStart = end - overlapSize;
                if (nextStart <= start) {
                    nextStart = start + 1;
                }
                start = nextStart;
            }
        }

        return chunks;
    }

    private int findBestBoundary(String text, int minPos, int maxPos) {
        if (minPos >= maxPos) {
            return -1;
        }

        String searchSub = text.substring(minPos, maxPos);

        // 1. Paragraph boundary (\n\n)
        int idx = searchSub.lastIndexOf("\n\n");
        if (idx != -1) {
            return minPos + idx + 2;
        }

        // 2. Line boundary (\n)
        idx = searchSub.lastIndexOf("\n");
        if (idx != -1) {
            return minPos + idx + 1;
        }

        // 3. Sentence boundary (. )
        idx = searchSub.lastIndexOf(". ");
        if (idx != -1) {
            return minPos + idx + 2;
        }

        // 4. Word boundary ( )
        idx = searchSub.lastIndexOf(" ");
        if (idx != -1) {
            return minPos + idx + 1;
        }

        return -1;
    }
}
