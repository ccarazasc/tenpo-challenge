-- API call history table
-- Stores every invocation recorded by ApiCallLoggingFilter.
-- 'response' column is TEXT to accommodate arbitrarily-sized JSON responses or error messages.

CREATE TABLE IF NOT EXISTS api_call_history (
    id         BIGSERIAL    PRIMARY KEY,
    endpoint   VARCHAR(512) NOT NULL,
    parameters TEXT,
    response   TEXT,
    success    BOOLEAN      NOT NULL DEFAULT TRUE,
    timestamp  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Descending index to speed up the paginated history query
CREATE INDEX IF NOT EXISTS idx_api_call_history_timestamp
    ON api_call_history (timestamp DESC);
