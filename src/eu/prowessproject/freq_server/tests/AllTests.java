/**
 * Copyright (c) 2014, Pablo Lamela Seijas
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Created: 2014-05-25
 */
package eu.prowessproject.freq_server.tests;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXB;

import org.junit.After;
import org.junit.Test;

import eu.prowessproject.freq_server.messages.constants.Constants;
import eu.prowessproject.freq_server.messages.response.FreqServerResponse;

public class AllTests {

	private static final String POST_METHOD_TOKEN = "POST";
	private static final String BASE_URL = "http://localhost:8080/freq_server/";
	public static final URL startServer;
	public static final URL stopServer;
	public static final URL allocateFrequency;
	public static final URL deallocateFrequency;
	
	static {
		try {
			startServer = new URL(BASE_URL + "StartServer");
			stopServer = new URL(BASE_URL + "StopServer");
			allocateFrequency = new URL(BASE_URL + "AllocateFrequency");
			deallocateFrequency = new URL(BASE_URL + "DeallocateFrequency");
		} catch (MalformedURLException e) {
			throw new RuntimeException("Wrong URL!", e);
		}		
	}

	@Test
	public void startStop_test() {
		FreqServerResponse startResponse = makeRequest(startServer);
		assertNormalResponse(startResponse);
		FreqServerResponse stopResponse = makeRequest(stopServer);
		assertNormalResponse(stopResponse);
	}

	@Test
	public void startStartStopStop_test() {
		FreqServerResponse startResponse = makeRequest(startServer);
		assertNormalResponse(startResponse);
		FreqServerResponse secondStartResponse = makeRequest(startServer);
		assertErrorResponse(secondStartResponse, Constants.ALREADY_STARTED);
		FreqServerResponse stopResponse = makeRequest(stopServer);
		assertNormalResponse(stopResponse);
		FreqServerResponse secondStopResponse = makeRequest(stopServer);
		assertErrorResponse(secondStopResponse, Constants.NOT_RUNNING);
	}
	
	@Test
	public void startAllocateDeallocateStop_test() {
		FreqServerResponse startResponse = makeRequest(startServer);
		assertNormalResponse(startResponse);
		FreqServerResponse allocateResponse = makeRequest(allocateFrequency);
		assertResultResponse(allocateResponse);
		Integer frequency = allocateResponse.getResult().getFrequencyAllocated();
		FreqServerResponse deallocateResponse = makeRequest(deallocateFrequency, frequency.toString());
		assertNormalResponse(deallocateResponse);
		FreqServerResponse stopResponse = makeRequest(stopServer);
		assertNormalResponse(stopResponse);
	}
	
	@Test
	public void badDeallocateStop_test() {
		FreqServerResponse badDeallocateResponse1 = makeRequest(deallocateFrequency, "2");
		assertErrorResponse(badDeallocateResponse1, Constants.NOT_RUNNING);
		FreqServerResponse startResponse = makeRequest(startServer);
		assertNormalResponse(startResponse);
		FreqServerResponse badDeallocateResponse2 = makeRequest(deallocateFrequency, "two");
		assertErrorResponse(badDeallocateResponse2, Constants.WRONG_REQUEST);
		FreqServerResponse allocateResponse = makeRequest(allocateFrequency);
		assertResultResponse(allocateResponse);
		Integer frequency = allocateResponse.getResult().getFrequencyAllocated();
		FreqServerResponse badDeallocateResponse3 = makeRequest(deallocateFrequency, "2");
		assertErrorResponse(badDeallocateResponse3, Constants.NOT_ALLOCATED);
		FreqServerResponse deallocateResponse = makeRequest(deallocateFrequency, frequency.toString());
		assertNormalResponse(deallocateResponse);
		FreqServerResponse badDeallocateResponse4 = makeRequest(deallocateFrequency, frequency.toString());
		assertErrorResponse(badDeallocateResponse4, Constants.NOT_ALLOCATED);
		FreqServerResponse stopResponse = makeRequest(stopServer);
		assertNormalResponse(stopResponse);
	}

	@After
	public void cleanUp() {
		makeRequest(stopServer);
	}

	private void assertNormalResponse(FreqServerResponse response) {
		assertEquals(Constants.OK, response.getState());
		assertTrue(response.getError().isEmpty());
		assertNull(response.getResult());
	}

	private void assertResultResponse(FreqServerResponse response) {
		assertEquals(Constants.OK, response.getState());
		assertTrue(response.getError().isEmpty());
		assertNotNull(response.getResult());
	}

	private void assertErrorResponse(FreqServerResponse response, String errorType) {
		assertEquals(Constants.ERROR, response.getState());
		assertEquals(1, response.getError().size());
		assertEquals(errorType, response.getError().get(0).getErrorType());
		assertNull(response.getResult());
	}

	private static FreqServerResponse makeRequest(URL url) {
		return makeRequest(url, null);
	}

	private static FreqServerResponse makeRequest(URL url, String param) {
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(POST_METHOD_TOKEN);
			if (param != null) {
				conn.setFixedLengthStreamingMode(param.length());
				conn.setDoOutput(true);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
				bw.write(param);
				bw.flush();
				bw.close();
			}
			conn.connect();
			FreqServerResponse response = JAXB.unmarshal(conn.getInputStream(), FreqServerResponse.class);
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				return response;
			} else {
				throw (new RuntimeException("Wrong response code!"));
			}
		} catch (IOException e) {
			throw (new RuntimeException("IOException when making request!", e));
		}
	}
}
