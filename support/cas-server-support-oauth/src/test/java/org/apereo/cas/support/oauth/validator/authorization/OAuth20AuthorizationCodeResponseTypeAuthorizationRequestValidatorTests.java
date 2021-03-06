package org.apereo.cas.support.oauth.validator.authorization;

import org.apereo.cas.authentication.principal.WebApplicationServiceFactory;
import org.apereo.cas.services.RegisteredServiceAccessStrategyAuditableEnforcer;
import org.apereo.cas.services.ServicesManager;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.OAuth20GrantTypes;
import org.apereo.cas.support.oauth.OAuth20ResponseTypes;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.apereo.cas.util.CollectionUtils;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.JEEContext;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collection;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link OAuth20AuthorizationCodeResponseTypeAuthorizationRequestValidatorTests}.
 *
 * @author Misagh Moayyed
 * @since 5.3.0
 */
@Tag("OAuth")
public class OAuth20AuthorizationCodeResponseTypeAuthorizationRequestValidatorTests {
    @Test
    public void verifyValidator() {
        val serviceManager = mock(ServicesManager.class);

        val service = new OAuthRegisteredService();
        service.setName("OAuth");
        service.setClientId("client");
        service.setClientSecret("secret");
        service.setServiceId("https://callback.example.org");

        when(serviceManager.getAllServices()).thenReturn((Collection) CollectionUtils.toCollection(service));
        val v = new OAuth20AuthorizationCodeResponseTypeAuthorizationRequestValidator(serviceManager, new WebApplicationServiceFactory(),
            new RegisteredServiceAccessStrategyAuditableEnforcer());

        val request = new MockHttpServletRequest();
        val response = new MockHttpServletResponse();

        assertFalse(v.validate(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.GRANT_TYPE, OAuth20GrantTypes.AUTHORIZATION_CODE.getType());
        assertFalse(v.validate(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.CLIENT_ID, "client");
        assertFalse(v.validate(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.REDIRECT_URI, service.getServiceId());
        assertFalse(v.validate(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.RESPONSE_TYPE, "unknown");
        assertFalse(v.validate(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.RESPONSE_TYPE, OAuth20ResponseTypes.CODE.getType());
        service.setSupportedResponseTypes(new LinkedHashSet<>());
        assertTrue(v.validate(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.REQUEST, "authn-request");
        assertFalse(v.validate(new JEEContext(request, response)));
        request.removeParameter(OAuth20Constants.REQUEST);

        service.setSupportedResponseTypes(CollectionUtils.wrapHashSet(OAuth20ResponseTypes.CODE.getType()));
        assertTrue(v.validate(new JEEContext(request, response)));

        service.setSupportedResponseTypes(CollectionUtils.wrapHashSet(OAuth20ResponseTypes.TOKEN.getType()));
        assertFalse(v.validate(new JEEContext(request, response)));

        assertTrue(v.supports(new JEEContext(request, response)));

        request.setParameter(OAuth20Constants.REDIRECT_URI, "unknown-uri");
        assertFalse(v.validate(new JEEContext(request, response)));
        request.setParameter(OAuth20Constants.REDIRECT_URI, service.getServiceId());

        service.getAccessStrategy().setServiceAccessAllowed(false);
        assertFalse(v.validate(new JEEContext(request, response)));

        assertEquals(Ordered.LOWEST_PRECEDENCE, v.getOrder());
        assertNotNull(v.getRegisteredServiceAccessStrategyEnforcer());
        assertEquals(OAuth20ResponseTypes.CODE, v.getResponseType());
        assertNotNull(v.getServicesManager());
        assertNotNull(v.getWebApplicationServiceServiceFactory());
    }
}
