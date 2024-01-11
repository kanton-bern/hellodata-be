/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.sidecars.airflow.service.resource;

import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.Pipeline;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineInstance;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineInstanceState;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.pipeline.PipelineResource;
import ch.bedag.dap.hellodata.sidecars.airflow.client.AirflowClient;
import ch.bedag.dap.hellodata.sidecars.airflow.client.dag.AirflowDag;
import ch.bedag.dap.hellodata.sidecars.airflow.client.dag.AirflowDagsResponse;
import io.kubernetes.client.openapi.models.V1Pod;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_PIPELINE_RESOURCES;

@Log4j2
@Service
public class AirflowPipelineResourceProviderService {

    private final ObjectProvider<DiscoveryClient> discoveryClientObjectProvider;
    private final ObjectProvider<PodUtils<V1Pod>> podUtilsObjectProvider;
    private final AirflowClient apiClient;
    private final NatsSenderService natsSenderService;
    private final String instanceName;

    public AirflowPipelineResourceProviderService(ObjectProvider<DiscoveryClient> discoveryClientObjectProvider, ObjectProvider<PodUtils<V1Pod>> podUtilsObjectProvider,
                                                  AirflowClient apiClient, NatsSenderService natsSenderService, @Value("${hello-data.instance.name}") String instanceName) {
        this.discoveryClientObjectProvider = discoveryClientObjectProvider;
        this.podUtilsObjectProvider = podUtilsObjectProvider;
        this.apiClient = apiClient;
        this.natsSenderService = natsSenderService;
        this.instanceName = instanceName;
    }

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-seconds:300}", timeUnit = TimeUnit.SECONDS)
    public void publishPipelines() throws URISyntaxException, IOException {
        log.info("--> publishPipelines()");
        AirflowDagsResponse dags = apiClient.dags();
        var pipelines = new ArrayList<Pipeline>();
        for (AirflowDag dag : dags.getDags()) {
            var pipeline = Pipeline.builder()
                                   .id(dag.getId())
                                   .active(dag.isActive())
                                   .paused(dag.isPaused())
                                   .description(dag.getDescription())
                                   .tags(dag.getTags().stream().map(AirflowDag.Tag::getName).toList())
                                   .fileLocation(dag.getFileLocation())
                                   .build();
            var airflowDagRunsResponse = apiClient.dagRuns(dag.getId());
            if (!airflowDagRunsResponse.getDagRuns().isEmpty()) {
                var lastRun = airflowDagRunsResponse.getDagRuns().get(0);
                pipeline.setLastInstance(PipelineInstance.builder()
                                                         .id(lastRun.getId())
                                                         .dagId(lastRun.getDagId())
                                                         .endDate(lastRun.getEndDate())
                                                         .startDate(lastRun.getStartDate())
                                                         .state(PipelineInstanceState.fromValue(lastRun.getState().getValue()))
                                                         .build());
            } else {
                pipeline.setLastInstance(new PipelineInstance());
            }
            pipelines.add(pipeline);
        }

        DiscoveryClient discoveryClient = this.discoveryClientObjectProvider.getIfAvailable();
        if (discoveryClient != null) {
            discoveryClient.description();
            discoveryClient.getServices();
        }
        PodUtils<V1Pod> podUtils = podUtilsObjectProvider.getIfAvailable();
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            PipelineResource pipelineResource = new PipelineResource(instanceName, current.getMetadata().getNamespace(), pipelines);
            natsSenderService.publishMessageToJetStream(PUBLISH_PIPELINE_RESOURCES, pipelineResource);
        } else {
            //dummy info for tests
            PipelineResource pipelineResource = new PipelineResource(instanceName, "namespace", pipelines);
            natsSenderService.publishMessageToJetStream(PUBLISH_PIPELINE_RESOURCES, pipelineResource);
        }
    }
}
