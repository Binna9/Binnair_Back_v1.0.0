# Anomaly 실시간 API — 프론트엔드 가이드

백엔드는 Postgres에 시세/점수를 쌓지 않고, Writer가 Redis 스냅샷을 갱신합니다.  
프론트는 **기존 REST URL을 그대로** 쓰되, 아래만 맞추면 됩니다.

## 심볼 식별자

- `venueId` / `instrumentId`는 `web.market_symbols` 기준입니다.
- `instrumentId` = `symbol_id`
- 예: binance BTCUSDT → `venueId=1`, `instrumentId`는 DB PK

## 유지되는 API

| Method | Path | 비고 |
|--------|------|------|
| GET | `/anomaly/scores/{venueId}/{instrumentId}/series` | DTO 동일(+ optional `isTip`) |
| GET | `/anomaly/scores/top` | DTO 동일 |
| GET | `/anomaly/scores/top/vol` | DTO 동일 |
| GET | `/anomaly/scores/top/rng` | DTO 동일 |
| GET | `/anomaly/scores/top/ret` | DTO 동일 |
| GET | `/anomaly/scores/{venueId}/{instrumentId}/final` | DTO 동일 |
| GET | `/anomaly/filter/venues` | web.market_symbols |
| GET | `/anomaly/filter/instruments` | web.market_symbols |

## 제거할 호출

- `POST /anomaly/scores/detect` → **410 Gone**. 호출하지 마세요.  
  점수 갱신은 서버 Writer가 담당합니다.

## series 포인트 구성

| 종류 | `isTip` | `ts` | 밀도 |
|------|---------|------|------|
| 확정봉 | `false` | 5m 캔들 openTime | timeframe당 1점 |
| tip 샘플 | `true` | **샘플 시각**(Writer 시각, openTime 아님) | `interval-ms`마다 1점 |

- tip은 같은 5m openTime을 덮어쓰지 않고, Writer 주기(~1s)마다 **append**됩니다.
- tip 보관: 기본 **최근 1시간** (`anomaly.realtime.tip-retention=1h`).  
  interval 1s ≈ 3600점, 2s ≈ 1800점.
- `/series` 응답 = 확정봉 + tip 히스토리를 시간순 merge (같은 ts면 tip 우선).
- 1H 차트: `from`/`to`를 최근 1시간으로 주면 tip 궤적이 보입니다 (최대 ~1800~3600점).
- `isTip`은 optional. 없으면 확정봉으로 취급해도 됩니다.

## 점수 갱신 의미 (tip LIVE)

- timeframe 기본 **5m**.
- **미확정 tip**: 롤링 윈도우에 넣지 않고 임시 z/score/final 계산 → Redis final/top/series tip에 반영.
- **확정봉**: 그때만 롤링 윈도우 commit + series에 5m 1포인트.
- Top / Final / series tip scores는 봉 마감 전에도 1~2초 단위로 변할 수 있습니다.
- UI는 `LIVE · 2s` 표현과 맞습니다.

## 권장 폴링

| 화면 | 주기 | API |
|------|------|-----|
| 스캐너 Top | **1~3초** | `/top`, `/top/vol` 등 |
| 차트 series | **1~3초** (보고 있는 종목만) | `/series` |
| final 배지 | **1~3초** | `/final` |

Redis를 프론트에서 직접 접속하지 않습니다.

## 워밍업 / 지연 UX

- HTTP **503** + `error.anomaly.realtime.not_ready`  
  → Writer 워밍업 중. “시세 준비 중” 표시 후 재시도.

## 차트 `from` / `to`

- Writer는 확정봉 **최근 30일** + tip **최근 1시간**을 Redis에 올립니다.
- 1H 뷰: `from`/`to`를 최근 1시간으로 맞추면 tip 궤적 위주.
- 장기 뷰: tip은 최근 1시간만 있고, 그 이전은 5m 확정봉만 있습니다.

## Redis 직접 접근

금지. 인증·키 스키마·브라우저 제약상 Backend REST만 사용합니다.
