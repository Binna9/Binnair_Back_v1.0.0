package com.bin.anomaly.score.repository;

import com.bin.anomaly.score.config.AnomalyScoreProperties;
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
    private final AnomalyScoreProperties props;

    /**
     * DDL의 UNIQUE(venue_id,instrument_id,timeframe,ts,score_version) 기반 멱등 저장.
     */
    @Transactional
    public void upsert(long venueId,
                       long instrumentId,
                       OffsetDateTime ts,
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
                ON CONFLICT (venue_id, instrument_id, timeframe, ts, score_version)
                DO UPDATE SET
                  window_days = EXCLUDED.window_days,
                  z_ret = EXCLUDED.z_ret,
                  z_vol = EXCLUDED.z_vol,
                  z_rng = EXCLUDED.z_rng,
                  score = EXCLUDED.score,
                  ingest_run_id = EXCLUDED.ingest_run_id
                """;

        em.createNativeQuery(sql)
                .setParameter("venueId", venueId)
                .setParameter("instrumentId", instrumentId)
                .setParameter("timeframe", props.getTimeframe())
                .setParameter("ts", Timestamp.from(ts.toInstant()))
                .setParameter("scoreVersion", props.getScoreVersion())
                .setParameter("windowDays", props.getWindowDays())
                .setParameter("zRet", zRet)
                .setParameter("zVol", zVol)
                .setParameter("zRng", zRng)
                .setParameter("score", score)
                .setParameter("runId", detectRunId)
                .executeUpdate();
    }
}

