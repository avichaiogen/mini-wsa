package com.akamai.miniwsa.samples;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

// PageRequest.getOffset() = page * size, so non-multiple offsets are silently rounded down.
// This implementation passes the raw offset directly so the SQL OFFSET matches the caller.
record OffsetPageRequest(int limit, int offset) implements Pageable {

    @Override public int getPageNumber()               { return 0; }
    @Override public int getPageSize()                 { return limit; }
    @Override public long getOffset()                  { return offset; }
    @Override public Sort getSort()                    { return Sort.unsorted(); }
    @Override public Pageable next()                   { return new OffsetPageRequest(limit, offset + limit); }
    @Override public Pageable previousOrFirst()        { return new OffsetPageRequest(limit, Math.max(0, offset - limit)); }
    @Override public Pageable first()                  { return new OffsetPageRequest(limit, 0); }
    @Override public Pageable withPage(int pageNumber) { return new OffsetPageRequest(limit, pageNumber * limit); }
    @Override public boolean hasPrevious()             { return offset > 0; }
}
