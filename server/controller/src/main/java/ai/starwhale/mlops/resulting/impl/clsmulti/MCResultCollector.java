/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.resulting.impl.clsmulti;

import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.resulting.Indicator;
import ai.starwhale.mlops.resulting.ResultCollector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * result collector for Multiclass classification
 */
@Slf4j
public class MCResultCollector implements ResultCollector {

    final StoragePathCoordinator storagePathCoordinator;
    final String storagePath;

    /**
     * dump to or reload from storage
     */
    final String jobUUID;

    /**
     * key: label-prediction
     * value: label& prediction items
     */
    final Map<String, List<MCIndicator>> rawResultHolder;

    final List<Indicator> results;

    final StorageAccessService storageAccessService;

    final ObjectMapper objectMapper;

    volatile boolean newItemIn;

    final MCConfusionMetrics mcConfusionMetrics;

    final CohenKappa cohenKappa;

    final MBCConfusionMetrics mbcConfusionMetrics;

    /**
     * a little bit heavy operation because of load
     * @param jobUUID the result id
     * @throws IOException storage exception
     */
    public MCResultCollector(String jobUUID,StoragePathCoordinator storagePathCoordinator,StorageAccessService storageAccessService,ObjectMapper objectMapper) throws IOException {
        this.jobUUID = jobUUID;
        this.storageAccessService = storageAccessService;
        this.storagePathCoordinator = storagePathCoordinator;
        this.objectMapper = objectMapper;
        rawResultHolder = new ConcurrentHashMap<>();
        storagePath = storagePath();
        load();
        this.results = new LinkedList<>();
        this.mcConfusionMetrics = new MCConfusionMetrics(this.rawResultHolder);
        this.cohenKappa = new CohenKappa(mcConfusionMetrics);
        this.mbcConfusionMetrics = new MBCConfusionMetrics(mcConfusionMetrics);
        this.results.add(mcConfusionMetrics);
        this.results.add(cohenKappa);
        this.results.add(mbcConfusionMetrics);
        newItemIn = false;
    }

    @Override
    public String getIdentity() {
        return this.jobUUID;
    }

    @Override
    public void feed(InputStream labelIS, InputStream inferenceResultIS) throws IOException {
        final Queue<String> labels = readLinesFromIS(labelIS);
        final Queue<String> inferenceResults = readLinesFromIS(inferenceResultIS);
        String label;
        String inferenceResult;
        do {
            label = labels.poll();
            inferenceResult = inferenceResults.poll();
            if (null == label) {
                break;
            }
            feed(new MCIndicator(label, inferenceResult));
        } while (true);
    }

    public void feed(MCIndicator mcIndicator) {
        rawResultHolder.computeIfAbsent(mcIndicator.getName(),
            k -> Collections.synchronizedList(new LinkedList<>()))
            .add(mcIndicator);
        mcConfusionMetrics.feed(mcIndicator);
        cohenKappa.feed(mcIndicator);
        mbcConfusionMetrics.feed(mcIndicator);
        newItemIn = true;
    }

    @Override
    public void feed(InputStream inferenceResultIS) throws IOException {
        final Queue<String> jsonLines = readLinesFromIS(inferenceResultIS);
        jsonLines.parallelStream().map(jsonline -> {
            InferenceResult inferenceResult;
            try {
                inferenceResult = objectMapper
                    .readValue(jsonline, InferenceResult.class);
            } catch (JsonProcessingException e) {
                log.error("unknown format of json line {}", jsonline);
                return null;
            }
            return inferenceResult;
        }).filter(Objects::nonNull)
            .map(InferenceResult::toMCIndicators)
            .flatMap(Collection::parallelStream)
            .forEach(this::feed);
    }

    Queue<String> readLinesFromIS(InputStream is) throws IOException {
        Queue<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        while (reader.ready()) {
            lines.offer(reader.readLine());
        }
        return lines;
    }

    /**
     * a little bit heavy operation when calculate is called
     */
    @Override
    public List<Indicator> collect() {
        if (newItemIn) {
            synchronized (this.results){
                if (newItemIn){
                    calculate();
                }
            }

        }
        return results;
    }

    private void calculate() {
        this.cohenKappa.calculate();
        this.mbcConfusionMetrics.calculate();
    }

    @Override
    public void load() throws IOException {
        final InputStream inputStream = storageAccessService.get(this.storagePath);
        if(null == inputStream){
            return;
        }
        TypeReference<HashMap<String, List<MCIndicator>>> typeRef
            = new TypeReference<HashMap<String, List<MCIndicator>>>() {};
        final HashMap<String, List<MCIndicator>> stringListHashMap = objectMapper
            .readValue(inputStream, typeRef);
        stringListHashMap.forEach((key,value)-> rawResultHolder.put(key,Collections.synchronizedList(value)));
    }

    @Override
    public void dump() throws IOException {
        storageAccessService.put(this.storagePath,objectMapper.writeValueAsBytes( rawResultHolder));
    }

    private String storagePath() {
        return storagePathCoordinator.resultMetricsPath(jobUUID);
    }
}
