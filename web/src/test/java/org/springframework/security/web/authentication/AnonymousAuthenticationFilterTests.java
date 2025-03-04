/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.web.authentication;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link AnonymousAuthenticationFilter}.
 *
 * @author Ben Alex
 * @author Eddú Meléndez
 */
public class AnonymousAuthenticationFilterTests {

	private void executeFilterInContainerSimulator(FilterConfig filterConfig, Filter filter, ServletRequest request,
			ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		filter.doFilter(request, response, filterChain);
	}

	@BeforeEach
	@AfterEach
	public void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	public void testDetectsMissingKey() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AnonymousAuthenticationFilter(null));
	}

	@Test
	public void testDetectsUserAttribute() {
		assertThatIllegalArgumentException().isThrownBy(() -> new AnonymousAuthenticationFilter("qwerty", null, null));
	}

	@Test
	public void testOperationWhenAuthenticationExistsInContextHolder() throws Exception {
		// Put an Authentication object into the SecurityContextHolder
		Authentication originalAuth = new TestingAuthenticationToken("user", "password", "ROLE_A");
		SecurityContextHolderStrategy strategy = mock(SecurityContextHolderStrategy.class);
		given(strategy.getContext()).willReturn(new SecurityContextImpl(originalAuth));
		AnonymousAuthenticationFilter filter = new AnonymousAuthenticationFilter("qwerty", "anonymousUsername",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		filter.setSecurityContextHolderStrategy(strategy);
		// Test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("x");
		executeFilterInContainerSimulator(mock(FilterConfig.class), filter, request, new MockHttpServletResponse(),
				new MockFilterChain(true));
		// Ensure filter didn't change our original object
		verify(strategy).getContext();
		verify(strategy, never()).setContext(any());
	}

	@Test
	public void testOperationWhenNoAuthenticationInSecurityContextHolder() throws Exception {
		AnonymousAuthenticationFilter filter = new AnonymousAuthenticationFilter("qwerty", "anonymousUsername",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
		filter.afterPropertiesSet();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("x");
		executeFilterInContainerSimulator(mock(FilterConfig.class), filter, request, new MockHttpServletResponse(),
				new MockFilterChain(true));
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		assertThat(auth.getPrincipal()).isEqualTo("anonymousUsername");
		assertThat(AuthorityUtils.authorityListToSet(auth.getAuthorities())).contains("ROLE_ANONYMOUS");
		SecurityContextHolder.getContext().setAuthentication(null); // so anonymous fires
																	// again
	}

	private class MockFilterChain implements FilterChain {

		private boolean expectToProceed;

		MockFilterChain(boolean expectToProceed) {
			this.expectToProceed = expectToProceed;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response) {
			if (!this.expectToProceed) {
				fail("Did not expect filter chain to proceed");
			}
		}

	}

}
