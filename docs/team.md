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