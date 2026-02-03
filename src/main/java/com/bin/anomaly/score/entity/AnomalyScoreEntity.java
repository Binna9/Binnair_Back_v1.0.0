package com.bin.anomaly.score.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(schema = "core", name = "anomaly_scores")
public class AnomalyScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id", nullable = false, updatable = false)
    private Long scoreId;

    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "instrument_id", nullable = false)
    private Long instrumentId;

    @Column(name = "timeframe", nullable = false, length = 10)
    private String timeframe;

    @Column(name = "ts", nullable = false)
    private OffsetDateTime ts;

    @Column(name = "score_version", nullable = false)
    private String scoreVersion;

    @Column(name = "window_days", nullable = false)
    private Integer windowDays;

    @Column(name = "z_ret")
    private Double zRet;

    @Column(name = "z_vol")
    private Double zVol;

    @Column(name = "z_rng")
    private Double zRng;

    @Column(name = "score")
    private Double score;

    @Column(name = "ingest_run_id")
    private UUID ingestRunId;

    @Column(name = "create_datetime", insertable = false, updatable = false)
    private OffsetDateTime createDatetime;
}

