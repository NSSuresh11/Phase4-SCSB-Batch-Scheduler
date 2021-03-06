package org.recap.batch.service;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.recap.BaseTestCase;
import org.recap.ScsbCommonConstants;
import org.recap.ScsbConstants;
import org.recap.model.batch.SolrIndexRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.recap.spring.SwaggerAPIProvider;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by akulak on 10/5/17.
 */
public class DailyReconcilationServiceUT extends BaseTestCase {

    @Value("${scsb.circ.url}")
    String solrCircUrl;

    @Mock
    RestTemplate restTemplate;

    @Mock
    private DailyReconcilationService dailyReconcilationService;

    @Mock
    CommonService commonService;

    @Test
    public void testDailyReconcilationService() throws Exception{
        String jobName  = "DailyReconcilation";
        Date lastExecutedTime = new Date();
        HttpHeaders headers = new HttpHeaders();
        headers.set(ScsbCommonConstants.API_KEY, SwaggerAPIProvider.getInstance().getSwaggerApiKey());
        HttpEntity<SolrIndexRequest> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<String> responseEntity = new ResponseEntity<>(ScsbConstants.SUCCESS, HttpStatus.OK);
        ReflectionTestUtils.setField(dailyReconcilationService,"commonService",commonService);
        Mockito.when(dailyReconcilationService.commonService.getRestTemplate()).thenReturn(restTemplate);
        Mockito.when(dailyReconcilationService.commonService.getRestTemplate().exchange(solrCircUrl + ScsbConstants.DAILY_RECONCILIATION_URL, HttpMethod.POST, httpEntity, String.class)).thenReturn(responseEntity);
        Mockito.when(dailyReconcilationService.commonService.executeService(solrCircUrl,  ScsbConstants.DAILY_RECONCILIATION_URL, HttpMethod.POST)).thenReturn(responseEntity.getBody());
        Mockito.when(dailyReconcilationService.dailyReconcilation(solrCircUrl)).thenCallRealMethod();
        String status = dailyReconcilationService.dailyReconcilation(solrCircUrl);
        assertNotNull(status);
        assertEquals(ScsbConstants.SUCCESS, status);
    }
}
