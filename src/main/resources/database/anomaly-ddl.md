## : ANOMALY TABLE DDL

### anomaly_scores
``` sql
CREATE TABLE IF NOT EXISTS core.anomaly_scores (
score_id       BIGSERIAL PRIMARY KEY,
venue_id       BIGINT NOT NULL REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
instrument_id  BIGINT NOT NULL REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
timeframe      VARCHAR(10) NOT NULL REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,
ts             TIMESTAMPTZ NOT NULL,

score_version  TEXT NOT NULL,              -- "z_v1" 같이 계산 정의 버전
window_days    INTEGER NOT NULL,           -- baseline window (예: 90)

z_ret          DOUBLE PRECISION,
z_vol          DOUBLE PRECISION,
z_rng          DOUBLE PRECISION,
score          DOUBLE PRECISION,           -- 종합 스코어 (예: max(|z|))

ingest_run_id  UUID,                       -- pipeline_runs 연결(선택)
create_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),

UNIQUE (venue_id, instrument_id, timeframe, ts, score_version)
);

CREATE INDEX IF NOT EXISTS idx_anomaly_scores_lookup
ON core.anomaly_scores (venue_id, instrument_id, timeframe, ts DESC, score_version);
```

