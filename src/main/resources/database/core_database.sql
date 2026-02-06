-- ============================================================================
-- MULTI-ASSET / MULTI-VENUE CRYPTO+STOCK+FX ML PIPELINE SCHEMA (ONE FILE)
-- PostgreSQL 13+ 권장 (pgcrypto, TIMESTAMPTZ 사용)
--
-- 핵심 설계
-- - instruments: 예측 대상(상품/티커의 “의미”)
-- - venues: 데이터 소스(거래소/브로커/데이터벤더)
-- - venue_symbols: venue별 실제 코드 매핑
-- - candles/features/labels: (venue_id, instrument_id, timeframe, ts) 기반 유니크
-- - features_vector: 학습 로딩 성능 최적(ARRAY), JSONB는 확장/디버깅용
-- - pipeline_runs: 운영 로그(재현성/디버깅)
-- - scalers: 스케일러 재현(체크포인트 외부)
--
-- 주의
-- - 이 스키마는 “멀티 벤더/멀티 자산군”을 전제로 하므로 symbol 문자열을 PK로 쓰지 않음
-- - 시간은 TIMESTAMPTZ(UTC 저장) 전제
-- ============================================================================

-- 0) Schema
CREATE SCHEMA IF NOT EXISTS core;
COMMENT ON SCHEMA core IS '멀티 자산/멀티 소스 ML 파이프라인 스키마';

-- 1) Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto schema core;

-- ============================================================================
-- 2) ENUM / DOMAIN
-- ============================================================================

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'label_3way' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname='core')) THEN
    CREATE DOMAIN core.label_3way AS INTEGER CHECK (VALUE IN (-1,0,1));
  END IF;
END $$;
COMMENT ON DOMAIN core.label_3way IS '-1 하락 / 0 보합 / 1 상승';

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'label_5class' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname='core')) THEN
    CREATE DOMAIN core.label_5class AS INTEGER CHECK (VALUE BETWEEN 0 AND 4);
  END IF;
END $$;
COMMENT ON DOMAIN core.label_5class IS '5분류 클래스(0~4)';

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'asset_class' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname='core')) THEN
    CREATE TYPE core.asset_class AS ENUM ('crypto','equity','fx','index','commodity','rates','etf');
  END IF;
END $$;
COMMENT ON TYPE core.asset_class IS '자산군 구분';

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'venue_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname='core')) THEN
    CREATE TYPE core.venue_type AS ENUM ('exchange','broker','data_vendor');
  END IF;
END $$;
COMMENT ON TYPE core.venue_type IS '데이터 소스 유형';

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pipeline_step' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname='core')) THEN
    CREATE TYPE core.pipeline_step AS ENUM ('collect','feature','label','train','inference');
  END IF;
END $$;
COMMENT ON TYPE core.pipeline_step IS '파이프라인 단계';

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'pipeline_status' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname='core')) THEN
    CREATE TYPE core.pipeline_status AS ENUM ('success','failed','partial','skipped');
  END IF;
END $$;
COMMENT ON TYPE core.pipeline_status IS '파이프라인 상태';

-- ============================================================================
-- 3) METADATA TABLES
-- ============================================================================

-- 3-1) venues
CREATE TABLE IF NOT EXISTS core.venues (
  venue_id        BIGSERIAL PRIMARY KEY,
  venue_code      TEXT NOT NULL UNIQUE,
  venue_type      core.venue_type NOT NULL,
  timezone        TEXT NOT NULL DEFAULT 'UTC',
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
  create_datetime TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE core.venues IS '데이터 소스(거래소/브로커/데이터벤더) 마스터';
COMMENT ON COLUMN core.venues.venue_id IS 'PK';
COMMENT ON COLUMN core.venues.venue_code IS '소스 코드(binance, polygon, kis, vendor_x 등)';
COMMENT ON COLUMN core.venues.venue_type IS 'exchange/broker/data_vendor';
COMMENT ON COLUMN core.venues.timezone IS '원천 데이터 기준 타임존 메타(기본 UTC)';
COMMENT ON COLUMN core.venues.is_active IS '활성 여부';
COMMENT ON COLUMN core.venues.metadata IS '벤더별 추가 메타(JSON)';
COMMENT ON COLUMN core.venues.create_datetime IS '생성 시각';

-- 3-2) instruments
CREATE TABLE IF NOT EXISTS core.instruments (
  instrument_id    BIGSERIAL PRIMARY KEY,
  symbol           TEXT NOT NULL,
  asset_class      core.asset_class NOT NULL,
  base_asset       TEXT,
  quote_asset      TEXT,
  currency         TEXT,
  country          TEXT,
  mic              TEXT,
  session_calendar TEXT,
  is_active        BOOLEAN NOT NULL DEFAULT TRUE,
  metadata         JSONB NOT NULL DEFAULT '{}'::jsonb,
  create_datetime  TIMESTAMPTZ NOT NULL DEFAULT now(),
  modify_datetime  TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE core.instruments IS '예측 대상(상품/티커 의미) 마스터';
COMMENT ON COLUMN core.instruments.instrument_id IS 'PK';
COMMENT ON COLUMN core.instruments.symbol IS '대표 표기(예: BTC/USDT, AAPL, 005930.KS, USDKRW 등)';
COMMENT ON COLUMN core.instruments.asset_class IS '자산군';
COMMENT ON COLUMN core.instruments.base_asset IS '기초자산(crypto/fx)';
COMMENT ON COLUMN core.instruments.quote_asset IS '상대자산(crypto/fx)';
COMMENT ON COLUMN core.instruments.currency IS '거래통화(equity/etf 등)';
COMMENT ON COLUMN core.instruments.country IS '국가 코드(예: US, KR)';
COMMENT ON COLUMN core.instruments.mic IS '거래소 MIC(선택)';
COMMENT ON COLUMN core.instruments.session_calendar IS '세션/휴장 캘린더 키(선택, 주식에 중요)';
COMMENT ON COLUMN core.instruments.is_active IS '활성 여부';
COMMENT ON COLUMN core.instruments.metadata IS '추가 메타(JSON)';
COMMENT ON COLUMN core.instruments.create_datetime IS '생성 시각';
COMMENT ON COLUMN core.instruments.modify_datetime IS '수정 시각';

-- 3-3) venue_symbols
CREATE TABLE IF NOT EXISTS core.venue_symbols (
  venue_id        BIGINT NOT NULL REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
  instrument_id   BIGINT NOT NULL REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
  venue_symbol    TEXT NOT NULL,
  market_type     TEXT,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  metadata        JSONB NOT NULL DEFAULT '{}'::jsonb,
  create_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),
  modify_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (venue_id, instrument_id),
  UNIQUE (venue_id, venue_symbol)
);

COMMENT ON TABLE core.venue_symbols IS 'venue별 실제 심볼/티커 매핑(수집 입력은 보통 venue_symbol)';
COMMENT ON COLUMN core.venue_symbols.venue_id IS 'FK -> venues';
COMMENT ON COLUMN core.venue_symbols.instrument_id IS 'FK -> instruments';
COMMENT ON COLUMN core.venue_symbols.venue_symbol IS '벤더/API에서 쓰는 실제 코드(예: CCXT symbol, Polygon ticker, 국내코드 등)';
COMMENT ON COLUMN core.venue_symbols.market_type IS '시장 타입(spot/futures/stock/fx/index 등)';
COMMENT ON COLUMN core.venue_symbols.is_active IS '활성 여부';
COMMENT ON COLUMN core.venue_symbols.metadata IS '벤더별 추가 메타(JSON)';
COMMENT ON COLUMN core.venue_symbols.create_datetime IS '생성 시각';
COMMENT ON COLUMN core.venue_symbols.modify_datetime IS '수정 시각';

-- 3-4) timeframes
CREATE TABLE IF NOT EXISTS core.timeframes (
  timeframe       VARCHAR(10) PRIMARY KEY,
  seconds         INTEGER NOT NULL CHECK (seconds > 0),
  create_datetime TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE core.timeframes IS '타임프레임 정의(봉 길이)';
COMMENT ON COLUMN core.timeframes.timeframe IS '타임프레임 코드(1m,5m,1h 등)';
COMMENT ON COLUMN core.timeframes.seconds IS '봉 길이(초)';
COMMENT ON COLUMN core.timeframes.create_datetime IS '생성 시각';

INSERT INTO core.timeframes(timeframe, seconds) VALUES
('1m', 60), ('3m', 180), ('5m', 300), ('15m', 900), ('30m', 1800),
('1h', 3600), ('4h', 14400), ('1d', 86400)
ON CONFLICT (timeframe) DO NOTHING;

-- ============================================================================
-- 4) CANDLES (RAW)
-- ============================================================================

CREATE TABLE IF NOT EXISTS core.candles (
  candle_id       BIGSERIAL PRIMARY KEY,
  venue_id        BIGINT NOT NULL REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
  instrument_id   BIGINT NOT NULL REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
  timeframe       VARCHAR(10) NOT NULL REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,
  ts              TIMESTAMPTZ NOT NULL,
  open            NUMERIC(20, 8) NOT NULL,
  high            NUMERIC(20, 8) NOT NULL,
  low             NUMERIC(20, 8) NOT NULL,
  close           NUMERIC(20, 8) NOT NULL,
  volume          NUMERIC(30, 8) NOT NULL,
  is_final        BOOLEAN NOT NULL DEFAULT TRUE,
  source          TEXT,
  ingest_run_id   UUID,
  create_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),
  modify_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT candles_ohlc_logic
    CHECK (high >= low AND high >= open AND high >= close AND low <= open AND low <= close),
  CONSTRAINT candles_volume_nonneg CHECK (volume >= 0),
  CONSTRAINT candles_close_positive CHECK (close > 0),

  UNIQUE (venue_id, instrument_id, timeframe, ts)
);

COMMENT ON TABLE core.candles IS '원본 캔들(OHLCV) 저장소: 전처리/스케일 값 저장 금지';
COMMENT ON COLUMN core.candles.candle_id IS 'PK';
COMMENT ON COLUMN core.candles.venue_id IS '데이터 소스 FK';
COMMENT ON COLUMN core.candles.instrument_id IS '예측 대상 FK';
COMMENT ON COLUMN core.candles.timeframe IS '타임프레임 FK';
COMMENT ON COLUMN core.candles.ts IS '캔들 시작 시각(UTC 저장 권장)';
COMMENT ON COLUMN core.candles.open IS '시가(원본)';
COMMENT ON COLUMN core.candles.high IS '고가(원본)';
COMMENT ON COLUMN core.candles.low IS '저가(원본)';
COMMENT ON COLUMN core.candles.close IS '종가(원본)';
COMMENT ON COLUMN core.candles.volume IS '거래량(원본)';
COMMENT ON COLUMN core.candles.is_final IS '확정 여부(정정/실시간 임시 캔들 대응)';
COMMENT ON COLUMN core.candles.source IS '수집 모듈/소스 식별(예: ccxt/polygon/vendor)';
COMMENT ON COLUMN core.candles.ingest_run_id IS '수집 run_id(추적용, 선택)';
COMMENT ON COLUMN core.candles.create_datetime IS '생성 시각';
COMMENT ON COLUMN core.candles.modify_datetime IS '수정 시각';

COMMENT ON CONSTRAINT candles_ohlc_logic ON core.candles IS 'OHLC 논리 제약';
COMMENT ON CONSTRAINT candles_volume_nonneg ON core.candles IS 'volume >= 0';
COMMENT ON CONSTRAINT candles_close_positive ON core.candles IS 'close > 0';

-- ============================================================================
-- 5) FEATURES
-- ============================================================================

CREATE TABLE IF NOT EXISTS core.features (
  feature_id       BIGSERIAL PRIMARY KEY,
  venue_id         BIGINT NOT NULL REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
  instrument_id    BIGINT NOT NULL REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
  timeframe        VARCHAR(10) NOT NULL REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,
  ts               TIMESTAMPTZ NOT NULL,

  feature_set_version TEXT NOT NULL,

  -- 학습 로딩 성능 핵심: 고정 순서 벡터
  features_vector  DOUBLE PRECISION[],

  -- 확장/디버깅용 JSONB
  indicators       JSONB NOT NULL DEFAULT '{}'::jsonb,
  features         JSONB NOT NULL DEFAULT '{}'::jsonb,

  is_scaled        BOOLEAN NOT NULL DEFAULT FALSE,
  scaler_version   TEXT,

  ingest_run_id    UUID,

  create_datetime  TIMESTAMPTZ NOT NULL DEFAULT now(),
  modify_datetime  TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE (venue_id, instrument_id, timeframe, ts, feature_set_version),

  CONSTRAINT features_scaled_requires_scaler
    CHECK (
      (is_scaled = FALSE AND scaler_version IS NULL)
      OR
      (is_scaled = TRUE  AND scaler_version IS NOT NULL)
    )
);

COMMENT ON TABLE core.features IS '피처 저장소(전처리 결과). feature_set_version으로 재현성 고정';
COMMENT ON COLUMN core.features.feature_id IS 'PK';
COMMENT ON COLUMN core.features.venue_id IS '데이터 소스 FK';
COMMENT ON COLUMN core.features.instrument_id IS '예측 대상 FK';
COMMENT ON COLUMN core.features.timeframe IS '타임프레임 FK';
COMMENT ON COLUMN core.features.ts IS '피처 시각(캔들 ts 기준)';
COMMENT ON COLUMN core.features.feature_set_version IS '피처 정의 버전';
COMMENT ON COLUMN core.features.features_vector IS '학습 입력용 고정 벡터(권장: 로더가 바로 텐서로 매핑)';
COMMENT ON COLUMN core.features.indicators IS '지표 JSON(확장/디버깅)';
COMMENT ON COLUMN core.features.features IS '추가 피처 JSON(확장/디버깅)';
COMMENT ON COLUMN core.features.is_scaled IS '스케일 적용 여부';
COMMENT ON COLUMN core.features.scaler_version IS '스케일러 버전(스케일 적용 시 필수)';
COMMENT ON COLUMN core.features.ingest_run_id IS '생성 run_id(추적용, 선택)';
COMMENT ON COLUMN core.features.create_datetime IS '생성 시각';
COMMENT ON COLUMN core.features.modify_datetime IS '수정 시각';

COMMENT ON CONSTRAINT features_scaled_requires_scaler ON core.features IS '스케일 적용 시 scaler_version 필수';

-- ============================================================================
-- 6) LABELS
-- ============================================================================

CREATE TABLE IF NOT EXISTS core.labels (
  label_id        BIGSERIAL PRIMARY KEY,
  venue_id        BIGINT NOT NULL REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
  instrument_id   BIGINT NOT NULL REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
  timeframe       VARCHAR(10) NOT NULL REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,
  ts              TIMESTAMPTZ NOT NULL,

  label_set_version TEXT NOT NULL,
  horizon         INTEGER NOT NULL CHECK (horizon > 0),

  future_close     NUMERIC(20, 8),
  future_return    DOUBLE PRECISION,
  future_volatility DOUBLE PRECISION,

  label           core.label_3way,
  label_class     core.label_5class,

  ingest_run_id   UUID,

  create_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),
  modify_datetime TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE (venue_id, instrument_id, timeframe, ts, label_set_version, horizon),

  CONSTRAINT labels_label_requires_class
    CHECK (label IS NULL OR label_class IS NOT NULL)
);

COMMENT ON TABLE core.labels IS '라벨/타겟 저장소. label_set_version + horizon으로 재현성 고정';
COMMENT ON COLUMN core.labels.label_id IS 'PK';
COMMENT ON COLUMN core.labels.venue_id IS '데이터 소스 FK';
COMMENT ON COLUMN core.labels.instrument_id IS '예측 대상 FK';
COMMENT ON COLUMN core.labels.timeframe IS '타임프레임 FK';
COMMENT ON COLUMN core.labels.ts IS '라벨 기준 시각(캔들 ts 기준)';
COMMENT ON COLUMN core.labels.label_set_version IS '라벨 정의 버전';
COMMENT ON COLUMN core.labels.horizon IS '예측 지평(봉 수)';
COMMENT ON COLUMN core.labels.future_close IS 'horizon 이후 종가';
COMMENT ON COLUMN core.labels.future_return IS 'horizon 이후 수익률';
COMMENT ON COLUMN core.labels.future_volatility IS 'horizon 이후 변동성(선택)';
COMMENT ON COLUMN core.labels.label IS '3분류 라벨(-1/0/1)';
COMMENT ON COLUMN core.labels.label_class IS '5분류 라벨(0~4)';
COMMENT ON COLUMN core.labels.ingest_run_id IS '생성 run_id(추적용, 선택)';
COMMENT ON COLUMN core.labels.create_datetime IS '생성 시각';
COMMENT ON COLUMN core.labels.modify_datetime IS '수정 시각';

COMMENT ON CONSTRAINT labels_label_requires_class ON core.labels IS 'label 존재 시 label_class도 존재';

-- ============================================================================
-- 7) SCALER ARTIFACTS
-- ============================================================================

CREATE TABLE IF NOT EXISTS core.scalers (
  scaler_id        BIGSERIAL PRIMARY KEY,
  venue_id         BIGINT NOT NULL REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
  instrument_id    BIGINT NOT NULL REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
  timeframe        VARCHAR(10) NOT NULL REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,

  scaler_version       TEXT NOT NULL,
  feature_set_version  TEXT NOT NULL,

  scaler_json       JSONB NOT NULL,
  feature_columns   JSONB NOT NULL,
  data_range        JSONB,
  notes             TEXT,

  create_datetime   TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE (venue_id, instrument_id, timeframe, scaler_version, feature_set_version)
);

COMMENT ON TABLE core.scalers IS '스케일러 아티팩트(체크포인트 없이도 스케일링 재현)';
COMMENT ON COLUMN core.scalers.scaler_id IS 'PK';
COMMENT ON COLUMN core.scalers.venue_id IS '데이터 소스 FK';
COMMENT ON COLUMN core.scalers.instrument_id IS '예측 대상 FK';
COMMENT ON COLUMN core.scalers.timeframe IS '타임프레임 FK';
COMMENT ON COLUMN core.scalers.scaler_version IS '스케일러 버전 식별자';
COMMENT ON COLUMN core.scalers.feature_set_version IS '대응 피처 버전';
COMMENT ON COLUMN core.scalers.scaler_json IS '스케일러 파라미터(JSON: mean/scale/var 등)';
COMMENT ON COLUMN core.scalers.feature_columns IS '벡터 컬럼 순서/정의(JSON)';
COMMENT ON COLUMN core.scalers.data_range IS '스케일러 학습 데이터 범위(JSON)';
COMMENT ON COLUMN core.scalers.notes IS '비고';
COMMENT ON COLUMN core.scalers.create_datetime IS '생성 시각';

-- ============================================================================
-- 8) PIPELINE RUNS
-- ============================================================================

CREATE TABLE IF NOT EXISTS core.pipeline_runs (
  run_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  step   core.pipeline_step NOT NULL,
  status core.pipeline_status NOT NULL DEFAULT 'success',

  venue_id      BIGINT REFERENCES core.venues(venue_id) ON UPDATE CASCADE,
  instrument_id BIGINT REFERENCES core.instruments(instrument_id) ON UPDATE CASCADE,
  timeframe     VARCHAR(10) REFERENCES core.timeframes(timeframe) ON UPDATE CASCADE,

  feature_set_version TEXT,
  label_set_version   TEXT,
  scaler_version      TEXT,
  horizon             INTEGER,

  start_ts TIMESTAMPTZ,
  end_ts   TIMESTAMPTZ,

  input_count  INTEGER DEFAULT 0,
  output_count INTEGER DEFAULT 0,

  error_message TEXT,
  extra JSONB NOT NULL DEFAULT '{}'::jsonb,

  create_datetime TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE core.pipeline_runs IS '파이프라인 실행 로그(운영/재현성/디버깅)';
COMMENT ON COLUMN core.pipeline_runs.run_id IS 'PK(UUID)';
COMMENT ON COLUMN core.pipeline_runs.step IS 'collect/feature/label/train/inference';
COMMENT ON COLUMN core.pipeline_runs.status IS 'success/failed/partial/skipped';
COMMENT ON COLUMN core.pipeline_runs.venue_id IS '연관 데이터 소스';
COMMENT ON COLUMN core.pipeline_runs.instrument_id IS '연관 예측 대상';
COMMENT ON COLUMN core.pipeline_runs.timeframe IS '연관 타임프레임';
COMMENT ON COLUMN core.pipeline_runs.feature_set_version IS '사용 피처 버전';
COMMENT ON COLUMN core.pipeline_runs.label_set_version IS '사용 라벨 버전';
COMMENT ON COLUMN core.pipeline_runs.scaler_version IS '사용 스케일러 버전';
COMMENT ON COLUMN core.pipeline_runs.horizon IS '사용 지평';
COMMENT ON COLUMN core.pipeline_runs.start_ts IS '처리 시작 시각';
COMMENT ON COLUMN core.pipeline_runs.end_ts IS '처리 종료 시각';
COMMENT ON COLUMN core.pipeline_runs.input_count IS '입력 레코드 수';
COMMENT ON COLUMN core.pipeline_runs.output_count IS '출력 레코드 수';
COMMENT ON COLUMN core.pipeline_runs.error_message IS '실패 시 에러 메시지';
COMMENT ON COLUMN core.pipeline_runs.extra IS '추가 메타(JSON)';
COMMENT ON COLUMN core.pipeline_runs.create_datetime IS '로그 시각';

-- ============================================================================
-- 9) INDEXES (운영/학습 로딩 최적화)
-- ============================================================================

-- instruments
CREATE INDEX IF NOT EXISTS idx_instruments_asset_symbol
  ON core.instruments(asset_class, symbol);
COMMENT ON INDEX core.idx_instruments_asset_symbol IS '자산군+심볼 검색';

CREATE INDEX IF NOT EXISTS idx_instruments_symbol
  ON core.instruments(symbol);
COMMENT ON INDEX core.idx_instruments_symbol IS '심볼 단독 검색';

CREATE UNIQUE INDEX IF NOT EXISTS uq_instruments_symbol_asset
ON core.instruments(symbol, asset_class);

-- venues
CREATE INDEX IF NOT EXISTS idx_venues_type_active
  ON core.venues(venue_type, is_active);
COMMENT ON INDEX core.idx_venues_type_active IS '소스 유형/활성 필터';

-- venue_symbols
CREATE INDEX IF NOT EXISTS idx_venue_symbols_venue_symbol
  ON core.venue_symbols(venue_id, venue_symbol);
COMMENT ON INDEX core.idx_venue_symbols_venue_symbol IS 'venue 내 심볼 조회(수집 resolve)';

CREATE INDEX IF NOT EXISTS idx_venue_symbols_instrument
  ON core.venue_symbols(instrument_id, venue_id);
COMMENT ON INDEX core.idx_venue_symbols_instrument IS 'instrument의 venue 목록 조회';

CREATE INDEX IF NOT EXISTS gin_venue_symbols_metadata
  ON core.venue_symbols USING GIN (metadata);
COMMENT ON INDEX core.gin_venue_symbols_metadata IS '매핑 메타 필터';

-- candles
CREATE INDEX IF NOT EXISTS idx_candles_vid_iid_tf_ts_desc
  ON core.candles(venue_id, instrument_id, timeframe, ts DESC);
COMMENT ON INDEX core.idx_candles_vid_iid_tf_ts_desc IS '학습/전처리 로딩 핵심 경로';

CREATE INDEX IF NOT EXISTS idx_candles_iid_tf_ts_desc
  ON core.candles(instrument_id, timeframe, ts DESC);
COMMENT ON INDEX core.idx_candles_iid_tf_ts_desc IS 'instrument 기준 로딩';

CREATE INDEX IF NOT EXISTS brin_candles_ts
  ON core.candles USING BRIN (ts);
COMMENT ON INDEX core.brin_candles_ts IS '대용량 기간 스캔(BRIN)';

CREATE INDEX IF NOT EXISTS idx_candles_ingest_run
  ON core.candles(ingest_run_id);
COMMENT ON INDEX core.idx_candles_ingest_run IS 'run_id 추적';

-- features
CREATE INDEX IF NOT EXISTS idx_features_vid_iid_tf_ts_desc_ver
  ON core.features(venue_id, instrument_id, timeframe, ts DESC, feature_set_version);
COMMENT ON INDEX core.idx_features_vid_iid_tf_ts_desc_ver IS '학습 로딩 핵심 경로(버전 포함)';

CREATE INDEX IF NOT EXISTS idx_features_iid_tf_ts_desc_ver
  ON core.features(instrument_id, timeframe, ts DESC, feature_set_version);
COMMENT ON INDEX core.idx_features_iid_tf_ts_desc_ver IS 'instrument 기준 피처 로딩';

CREATE INDEX IF NOT EXISTS brin_features_ts
  ON core.features USING BRIN (ts);
COMMENT ON INDEX core.brin_features_ts IS '대용량 기간 스캔(BRIN)';

-- JSONB 인덱스는 쓰기 비용이 있으니 필요하면 유지(운영 정책에 따라 제거 가능)
CREATE INDEX IF NOT EXISTS gin_features_indicators
  ON core.features USING GIN (indicators);
COMMENT ON INDEX core.gin_features_indicators IS '지표 JSON 필터/검색';

CREATE INDEX IF NOT EXISTS gin_features_features
  ON core.features USING GIN (features);
COMMENT ON INDEX core.gin_features_features IS '피처 JSON 필터/검색';

CREATE INDEX IF NOT EXISTS idx_features_ingest_run
  ON core.features(ingest_run_id);
COMMENT ON INDEX core.idx_features_ingest_run IS 'run_id 추적';

-- labels
CREATE INDEX IF NOT EXISTS idx_labels_vid_iid_tf_ts_desc_ver_h
  ON core.labels(venue_id, instrument_id, timeframe, ts DESC, label_set_version, horizon);
COMMENT ON INDEX core.idx_labels_vid_iid_tf_ts_desc_ver_h IS '학습 로딩 핵심(버전/지평 포함)';

CREATE INDEX IF NOT EXISTS idx_labels_iid_tf_ts_desc_ver_h
  ON core.labels(instrument_id, timeframe, ts DESC, label_set_version, horizon);
COMMENT ON INDEX core.idx_labels_iid_tf_ts_desc_ver_h IS 'instrument 기준 라벨 로딩';

CREATE INDEX IF NOT EXISTS brin_labels_ts
  ON core.labels USING BRIN (ts);
COMMENT ON INDEX core.brin_labels_ts IS '대용량 기간 스캔(BRIN)';

CREATE INDEX IF NOT EXISTS idx_labels_ingest_run
  ON core.labels(ingest_run_id);
COMMENT ON INDEX core.idx_labels_ingest_run IS 'run_id 추적';

-- scaler
CREATE INDEX IF NOT EXISTS idx_scaler_vid_iid_tf_version_desc
  ON core.scalers(venue_id, instrument_id, timeframe, scaler_version DESC);
COMMENT ON INDEX core.idx_scaler_vid_iid_tf_version_desc IS '최신 스케일러 탐색';

-- pipeline_runs
CREATE INDEX IF NOT EXISTS idx_pipeline_runs_step_time_desc
  ON core.pipeline_runs(step, create_datetime DESC);
COMMENT ON INDEX core.idx_pipeline_runs_step_time_desc IS '단계별 최근 실행 조회';

CREATE INDEX IF NOT EXISTS idx_pipeline_runs_vid_iid_tf_time_desc
  ON core.pipeline_runs(venue_id, instrument_id, timeframe, create_datetime DESC);
COMMENT ON INDEX core.idx_pipeline_runs_vid_iid_tf_time_desc IS '대상별 최근 실행 조회';

CREATE INDEX IF NOT EXISTS gin_pipeline_runs_extra
  ON core.pipeline_runs USING GIN (extra);
COMMENT ON INDEX core.gin_pipeline_runs_extra IS 'extra JSON 필터/검색';

-- ============================================================================
-- 10) TRIGGERS (modify_datetime 자동 갱신)
-- ============================================================================

CREATE OR REPLACE FUNCTION core.touch_modify_datetime()
RETURNS TRIGGER AS $$
BEGIN
  NEW.modify_datetime = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;
COMMENT ON FUNCTION core.touch_modify_datetime() IS 'UPDATE 시 modify_datetime 자동 갱신';

-- instruments
DROP TRIGGER IF EXISTS trg_touch_instruments ON core.instruments;
CREATE TRIGGER trg_touch_instruments
BEFORE UPDATE ON core.instruments
FOR EACH ROW EXECUTE FUNCTION core.touch_modify_datetime();
COMMENT ON TRIGGER trg_touch_instruments ON core.instruments IS 'instruments 수정 시 modify_datetime 갱신';

-- venue_symbols
DROP TRIGGER IF EXISTS trg_touch_venue_symbols ON core.venue_symbols;
CREATE TRIGGER trg_touch_venue_symbols
BEFORE UPDATE ON core.venue_symbols
FOR EACH ROW EXECUTE FUNCTION core.touch_modify_datetime();
COMMENT ON TRIGGER trg_touch_venue_symbols ON core.venue_symbols IS 'venue_symbols 수정 시 modify_datetime 갱신';

-- candles
DROP TRIGGER IF EXISTS trg_touch_candles ON core.candles;
CREATE TRIGGER trg_touch_candles
BEFORE UPDATE ON core.candles
FOR EACH ROW EXECUTE FUNCTION core.touch_modify_datetime();
COMMENT ON TRIGGER trg_touch_candles ON core.candles IS 'candles 수정 시 modify_datetime 갱신';

-- features
DROP TRIGGER IF EXISTS trg_touch_features ON core.features;
CREATE TRIGGER trg_touch_features
BEFORE UPDATE ON core.features
FOR EACH ROW EXECUTE FUNCTION core.touch_modify_datetime();
COMMENT ON TRIGGER trg_touch_features ON core.features IS 'features 수정 시 modify_datetime 갱신';

-- labels
DROP TRIGGER IF EXISTS trg_touch_labels ON core.labels;
CREATE TRIGGER trg_touch_labels
BEFORE UPDATE ON core.labels
FOR EACH ROW EXECUTE FUNCTION core.touch_modify_datetime();
COMMENT ON TRIGGER trg_touch_labels ON core.labels IS 'labels 수정 시 modify_datetime 갱신';

-- ============================================================================
-- 11) VIEWS (운영 편의)
-- ============================================================================

CREATE OR REPLACE VIEW core.latest_candles AS
SELECT DISTINCT ON (venue_id, instrument_id, timeframe)
  candle_id, venue_id, instrument_id, timeframe, ts,
  open, high, low, close, volume,
  is_final, source, ingest_run_id,
  create_datetime, modify_datetime
FROM core.candles
ORDER BY venue_id, instrument_id, timeframe, ts DESC;

COMMENT ON VIEW core.latest_candles IS 'venue/instrument/timeframe별 최신 캔들';

CREATE OR REPLACE VIEW core.latest_features AS
SELECT DISTINCT ON (venue_id, instrument_id, timeframe, feature_set_version)
  feature_id, venue_id, instrument_id, timeframe, feature_set_version, ts,
  features_vector, indicators, features,
  is_scaled, scaler_version, ingest_run_id,
  create_datetime, modify_datetime
FROM core.features
ORDER BY venue_id, instrument_id, timeframe, feature_set_version, ts DESC;

COMMENT ON VIEW core.latest_features IS 'venue/instrument/tf/feature_version별 최신 피처';

CREATE OR REPLACE VIEW core.train_dataset AS
SELECT
  f.venue_id,
  f.instrument_id,
  f.timeframe,
  f.ts,
  f.feature_set_version,
  f.is_scaled,
  f.scaler_version,
  f.features_vector,
  f.indicators,
  f.features,
  l.label_set_version,
  l.horizon,
  l.future_close,
  l.future_return,
  l.future_volatility,
  l.label,
  l.label_class
FROM core.features f
JOIN core.labels l
  ON l.venue_id = f.venue_id
 AND l.instrument_id = f.instrument_id
 AND l.timeframe = f.timeframe
 AND l.ts = f.ts;

COMMENT ON VIEW core.train_dataset IS '학습 로딩용 조인 뷰(반드시 버전/지평 WHERE로 필터해서 사용)';

-- 데이터셋 인덱스 추가 
CREATE INDEX IF NOT EXISTS ix_features_lookup
ON core.features (venue_id, instrument_id, timeframe, feature_set_version, ts DESC)
WHERE is_scaled = false;

CREATE INDEX IF NOT EXISTS ix_features_fit_unscaled
ON core.features (venue_id, instrument_id, timeframe, feature_set_version, ts)
WHERE is_scaled = false;

CREATE INDEX IF NOT EXISTS ix_labels_target
ON core.labels (venue_id, instrument_id, timeframe, label_set_version, horizon, ts);