package com.bin.anomaly.score.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AnomalyScoreUpsertDao {

    @PersistenceContext
    private final EntityManager em;

    /**
     * @param venueId 거래소 ID
     * @param instrumentId 종목 ID
     * @param timeframe 캔들 주기 (예: "5m", "1h")
     * @param ts 점수 기준 시각
     * @param scoreVersion 점수 버전 (예: "z_v1")
     * @param windowDays baseline window 일수
     * @param zRet 수익률 z-score
     * @param zVol 거래량 z-score
     * @param zRng 변동폭 z-score
     * @param score 종합 이상 점수
     * @param detectRunId pipeline run ID (ingest_run_id 컬럼에 저장)
     */
    @Transactional
    public void upsert(long venueId,
                       long instrumentId,
                       String timeframe,
                       OffsetDateTime ts,
                       String scoreVersion,
                       int windowDays,
                       Double zRet,
                       Double zVol,
                       Double zRng,
                       Double score,
                       UUID detectRunId) {

        String sql = """
                INSERT INTO core.anomaly_scores (
                  venue_id, instrument_id, timeframe, ts,
                  score_version, window_days,
                  z_ret, z_vol, z_rng, score,
                  ingest_run_id
                )
                VALUES (
                  :venueId, :instrumentId, :timeframe, :ts,
                  :scoreVersion, :windowDays,
                  :zRet, :zVol, :zRng, :score,
                  :runId
                )
                ON CONFLICT (venue_id, instrument_id, timeframe, ts, score_version, window_days)
                DO UPDATE SET
                  z_ret = EXCLUDED.z_ret,
                  z_vol = EXCLUDED.z_vol,
                  z_rng = EXCLUDED.z_rng,
                  score = EXCLUDED.score,
                  ingest_run_id = EXCLUDED.ingest_run_id
                """;

        em.createNativeQuery(sql)
                .setParameter("venueId", venueId)
                .setParameter("instrumentId", instrumentId)
                .setParameter("timeframe", timeframe)
                .setParameter("ts", Timestamp.from(ts.toInstant()))
                .setParameter("scoreVersion", scoreVersion)
                .setParameter("windowDays", windowDays)
                .setParameter("zRet", zRet)
                .setParameter("zVol", zVol)
                .setParameter("zRng", zRng)
                .setParameter("score", score)
                .setParameter("runId", detectRunId)
                .executeUpdate();
    }
}

