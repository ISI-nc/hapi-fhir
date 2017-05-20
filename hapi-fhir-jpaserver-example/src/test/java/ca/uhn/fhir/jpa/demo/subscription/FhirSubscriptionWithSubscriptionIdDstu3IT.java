/*
 *  Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  @author Jeff Chung
 */
package ca.uhn.fhir.jpa.demo.subscription;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.EncodingEnum;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Subscription;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import java.net.URI;

/**
 * Adds a FHIR subscription with criteria through the rest interface. Then creates a websocket with the id of the
 * subscription
 * <p>
 * Note: This test only returns a ping with the subscription id, Check FhirSubscriptionWithSubscriptionIdDstu3IT for
 * a test that returns the xml of the observation
 * <p>
 * To execute the following test, execute it the following way:
 * 0. execute 'clean' test
 * 1. Execute the 'createPatient' test
 * 2. Update the patient id static variable
 * 3. Execute the 'createSubscription' test
 * 4. Update the subscription id static variable
 * 5. Execute the 'attachWebSocket' test
 * 6. Execute the 'sendObservation' test
 * 7. Look in the 'attachWebSocket' terminal execution and wait for your ping with the subscription id
 */
@Ignore
public class FhirSubscriptionWithSubscriptionIdDstu3IT {

    private static final Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirSubscriptionWithSubscriptionIdDstu3IT.class);

    public static final String WEBSOCKET_LISTENER_URL = "ws://localhost:9093/websocket/dstu3";

    public static final String PATIENT_ID = "5102";
    public static final String SUBSCRIPTION_ID = "5103";

    private IGenericClient client = FhirServiceUtil.getFhirDstu3Client();

    @Test
    public void clean() {
        RemoveDstu3TestIT.deleteResources(Subscription.class, null, client);
        RemoveDstu3TestIT.deleteResources(Observation.class, null, client);
    }

    @Test
    public void createPatient() throws Exception {
        IGenericClient client = FhirServiceUtil.getFhirDstu3Client();
        Patient patient = FhirDstu3Util.getPatient();
        MethodOutcome methodOutcome = client.create().resource(patient).execute();
        String id = methodOutcome.getId().getIdPart();
        patient.setId(id);
        System.out.println("Patient id generated by server is: " + id);
    }

    @Test
    public void createSubscription() {
        IGenericClient client = FhirServiceUtil.getFhirDstu3Client();

        Subscription subscription = new Subscription();
        subscription.setReason("Monitor new neonatal function (note, age will be determined by the monitor)");
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
//        subscription.setCriteria("Observation?subject=Patient/" + PATIENT_ID);
        subscription.setCriteria("Observation?code=SNOMED-CT|82313006");

        Subscription.SubscriptionChannelComponent channel = new Subscription.SubscriptionChannelComponent();
        channel.setType(Subscription.SubscriptionChannelType.WEBSOCKET);
        channel.setPayload("application/json");
        subscription.setChannel(channel);

        MethodOutcome methodOutcome = client.create().resource(subscription).execute();
        String id = methodOutcome.getId().getIdPart();

        System.out.println("Subscription id generated by server is: " + id);
    }

    @Ignore
    @Test
    public void attachWebSocket() throws Exception {
        WebSocketClient webSocketClient = new WebSocketClient();
        SocketImplementation socket = new SocketImplementation(SUBSCRIPTION_ID, EncodingEnum.JSON);

        try {
            webSocketClient.start();
            URI echoUri = new URI(WEBSOCKET_LISTENER_URL);
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            ourLog.info("Connecting to : {}", echoUri);
            webSocketClient.connect(socket, echoUri, request);

            while (true) {
                Thread.sleep(500L);
            }

        } finally {
            try {
                ourLog.info("Shutting down websocket client");
                webSocketClient.stop();
            } catch (Exception e) {
                ourLog.error("Failure", e);
            }
        }
    }

    @Test
    public void createObservation() throws Exception {
        Observation observation = new Observation();
        CodeableConcept codeableConcept = new CodeableConcept();
        observation.setCode(codeableConcept);
        Coding coding = codeableConcept.addCoding();
        coding.setCode("82313006");
        coding.setSystem("SNOMED-CT");
        Reference reference = new Reference();
        reference.setReference("Patient/" + PATIENT_ID);
        observation.setSubject(reference);
        observation.setStatus(Observation.ObservationStatus.FINAL);

        IGenericClient client = FhirServiceUtil.getFhirDstu3Client();

        MethodOutcome methodOutcome2 = client.create().resource(observation).execute();
        String observationId = methodOutcome2.getId().getIdPart();
        observation.setId(observationId);

        System.out.println("Observation id generated by server is: " + observationId);
    }

    @Test
    public void createObservationThatDoesNotMatch() throws Exception {
        Observation observation = new Observation();
        IdDt idDt = new IdDt();
        idDt.setValue("Patient/" + PATIENT_ID);
        CodeableConcept codeableConcept = new CodeableConcept();
        observation.setCode(codeableConcept);
        Coding coding = codeableConcept.addCoding();
        coding.setCode("8231");
        coding.setSystem("SNOMED-CT");
        observation.setStatus(Observation.ObservationStatus.FINAL);

        IGenericClient client = FhirServiceUtil.getFhirDstu3Client();

        MethodOutcome methodOutcome2 = client.create().resource(observation).execute();
        String observationId = methodOutcome2.getId().getIdPart();
        observation.setId(observationId);

        System.out.println("Observation id generated by server is: " + observationId);

    }
}