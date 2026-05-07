# Java Brewers Capstone Team
==============================

| Lead               | Lead role | Owns |
|--------------------|---|---|
| Martin Melekus     | Resource Server | TransactionService, RS integration tests, Kafka verification, reading + explaining JwtAuthConverter |
| Manish Patel       | Frontend           | apiFetch, useMe, page components, smoke tests |
| Krupali Desai      | Quality / Security | Hardening sweep, SAST, demo script |

## Task 2.1
1. who calls the API? the BFF
2. What URL is hit? The API endpoint is `/api/v1/accounts` (the `/api` prefix is important — it triggers the proxy controller, which adds the session and CSRF token and forwards to the resource server).
3. How does the request carry the session and CSRF token? The session and CSRF token are carried in cookies.
4. Where does /api proxy to? The `/api` endpoint proxies to the bff server running on port 8080: http://localhost:8080
5. How does the BFF forward? The BFF requests from the resource server /api/v1 and passes the response back to the frontend through a mock controller.
6. How does the bearer token get attached? WebClient forwards the bearer token to the resource server using an OIDC filter that extracts it from the session and adds it to the Authorization header of outgoing requests.
7. Who handles it on the RS? The resource server handles it in the `SecurityConfig` filter chain, where the JWT is validated and converted to a local user with authorities.
8. What does ownership look like? The account owner is the calling user--accounts are filtered by the calling user's ID.
9. Final stop? The AccountRepository defines only a findByOwnerId method.

## Task 2.2
1. JWT issuer + audience validated: The `SecurityConfig` on the resource server validates the JWT's issuer and audience claims against the mock-auth server's configuration.
2. Stateless session policy on the resource server: The `SecurityConfig` sets the session management policy to stateless, meaning the resource server does not maintain any session state and relies entirely on the JWT for authentication.
3. URL-level hasRole("ADMIN") rule: @PreAuthorize annotation on listAll method in the UserController.
4. @PreAuthrize method-level rule: Is on the listAll method in the UserController.
5. Ownership check that returns 404 (not 403): AccountController getOne method lists 404, not 403 if user tries to list accounts he doesn't own.
6. CSRF token cookie configuration on the BFF:  Is configured in the BFF's CookieOAuth2AuthorizationRequestRepository, which then sets the CSRF token in a cookie.  It is set to HttpOnly true to prevent XSRF attacks.
7. CSRF eager-load filter on the BFF (already wired): Is configured in the SecurityConfig of the BFF.  Eager loading makes sure the cookie is loaded. 
