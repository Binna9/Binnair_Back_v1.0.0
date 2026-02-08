## : ANOMALY TABLE DDL

### anomaly_scores
``` sql
-- ============================================================
-- core.anomaly_scores
-- 5분봉(및 기타 timeframe) 기준 이상점수(z-score 기반) 저장 테이블
-- - 입력: core.candles(확정봉)에서 파생(ret/vol/range)
-- - 출력: ts별 z-score + 종합 score를 버전(score_version) 단위로 저장
-- - 특징:
--   * score_version으로 계산 정의(변환/윈도우/집계 방식) 버전 관리
--   * UNIQUE로 멱등(재계산/재실행 시 upsert 가능)
--   * ts는 TIMESTAMPTZ(UTC aware)로 candles와 동일 기준 유지
-- ============================================================

CREATE TABLE IF NOT EXISTS core.anomaly_scores (
    score_id        BIGSERIAL PRIMARY KEY,  -- surrogate key (내부 식별자)
    venue_id        BIGINT NOT NULL
        REFERENCES core.venues(venue_id) ON UPDATE CASCADE,  -- 거래소/venue 식별자
    instrument_id   BIGINT NOT NULL
        REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,  -- 자산/종목 식별자
    timeframe       VARCHAR(10) NOT NULL
        REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,  -- '5m' 등 캔들 주기
    ts              TIMESTAMPTZ NOT NULL,   -- 점수 기준 시각(캔들 ts와 동일, UTC aware)
    score_version   TEXT NOT NULL,          -- 점수 계산 정의 버전 (예: 'z_v1', 'ewmz_v1')
    window_days     INTEGER NOT NULL,       -- baseline window 기간(일) (예: 90)
    z_ret           DOUBLE PRECISION,       -- 수익률(ret) z-score (예: log return 표준화)
    z_vol           DOUBLE PRECISION,       -- 거래량(volume/logvol) z-score
    z_rng           DOUBLE PRECISION,       -- 변동폭(range) z-score (예: (high-low)/close)
    score           DOUBLE PRECISION,       -- 종합 이상 점수 (예: max(|z_ret|,|z_vol|,|z_rng|))
    ingest_run_id   UUID,                   -- 파이프라인 run_id 연결(선택)
                                            -- NOTE: 실제로는 detect_run_id가 의미상 더 맞을 수 있음
    create_datetime TIMESTAMPTZ NOT NULL DEFAULT now(), -- 레코드 생성 시각(UTC)
    UNIQUE (venue_id, instrument_id, timeframe, ts, score_version, window_days) -- 멱등 저장 키
);

COMMENT ON TABLE core.anomaly_scores IS
'캔들(ts) 기준 이상점수 저장 테이블. ret/vol/range 파생값을 baseline(window_days) 대비 z-score로 표준화하고 종합 score를 score_version 단위로 저장한다. UNIQUE 키로 재실행/재계산 시 upsert 가능한 멱등 구조.';
COMMENT ON COLUMN core.anomaly_scores.score_id IS
'이상점수 레코드 내부 식별자(BIGSERIAL).';
COMMENT ON COLUMN core.anomaly_scores.venue_id IS
'거래소/venue 식별자(core.venues FK).';
COMMENT ON COLUMN core.anomaly_scores.instrument_id IS
'자산/종목 식별자(core.instruments FK).';
COMMENT ON COLUMN core.anomaly_scores.timeframe IS
'캔들 주기(예: 5m, 1h). core.timeframes FK.';
COMMENT ON COLUMN core.anomaly_scores.ts IS
'점수 기준 시각(캔들 ts와 동일). TIMESTAMPTZ(UTC aware).';
COMMENT ON COLUMN core.anomaly_scores.score_version IS
'점수 계산 정의 버전. feature 변환/윈도우/집계 방식 변경 시 버전으로 구분(예: z_v1).';
COMMENT ON COLUMN core.anomaly_scores.window_days IS
'baseline 윈도우 기간(일). 예: 최근 90일 분포를 기준으로 z-score 산출.';
COMMENT ON COLUMN core.anomaly_scores.z_ret IS
'수익률(ret) z-score. 일반적으로 log(close_t/close_{t-1})를 표준화한 값.';
COMMENT ON COLUMN core.anomaly_scores.z_vol IS
'거래량(volume) z-score. heavy-tail 완화를 위해 log1p(volume) 등을 표준화한 값으로 쓰는 것을 권장.';
COMMENT ON COLUMN core.anomaly_scores.z_rng IS
'변동폭(range) z-score. 예: (high-low)/close 또는 log(high/low) 등을 표준화한 값.';
COMMENT ON COLUMN core.anomaly_scores.score IS
'종합 이상 점수. 예: max(|z_ret|, |z_vol|, |z_rng|) 또는 가중합 등(버전 정의에 따름).';
COMMENT ON COLUMN core.anomaly_scores.ingest_run_id IS
'파이프라인 run_id 연결용 UUID(선택). 현재 컬럼명은 ingest_run_id이나, 이상탐지 단계 run_id를 넣는 용도로도 사용 가능(운영 정책으로 고정 필요).';
COMMENT ON COLUMN core.anomaly_scores.create_datetime IS
'레코드 생성 시각(기본 now()).';
-- 조회 패턴: 자산별 최근 점수 + 버전 필터 (ts DESC) 최적화
CREATE INDEX IF NOT EXISTS idx_anomaly_scores_lookup
ON core.anomaly_scores (venue_id, instrument_id, timeframe, ts DESC, score_version);
COMMENT ON INDEX core.idx_anomaly_scores_lookup IS
'자산(venue/instrument/timeframe)별 최근 점수 조회(ts DESC) 및 score_version 필터 최적화 인덱스.';
CREATE INDEX IF NOT EXISTS idx_anomaly_scores_final
ON core.anomaly_scores (venue_id, instrument_id, timeframe, score_version, window_days, ts DESC);


```

