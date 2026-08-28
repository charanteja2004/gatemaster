package com.gatemaster.server

import java.time.Duration

/**
 * Everything the server needs from its environment, resolved once at startup.
 *
 * Reading configuration eagerly means a misconfigured deployment fails on the
 * first line of main() with a message naming the variable, rather than on the
 * first request that happens to need it.
 */
data class Config(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String?,
    val databasePassword: String?,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val accessTokenLifetime: Duration,
    val refreshTokenLifetime: Duration,
    val bcryptCost: Int,
    val allowedOrigins: List<String>,
) {
    companion object {
        /** Below this the hash is cheap enough to be worth brute-forcing. */
        const val MIN_BCRYPT_COST = 10

        /**
         * A JWT secret shorter than this is not worth the HMAC around it. 32
         * bytes matches the output width of SHA-256.
         */
        const val MIN_SECRET_LENGTH = 32

        fun fromEnv(env: (String) -> String? = System::getenv): Config {
            fun required(name: String): String = env(name)
                ?: error("$name is not set. See server/README.md for the full list.")

            val secret = required("GATEMASTER_JWT_SECRET")
            require(secret.length >= MIN_SECRET_LENGTH) {
                "GATEMASTER_JWT_SECRET must be at least $MIN_SECRET_LENGTH characters; " +
                    "generate one with: openssl rand -base64 48"
            }

            val cost = env("GATEMASTER_BCRYPT_COST")?.toIntOrNull() ?: 12
            require(cost >= MIN_BCRYPT_COST) {
                "GATEMASTER_BCRYPT_COST must be at least $MIN_BCRYPT_COST, was $cost"
            }

            return Config(
                port = env("PORT")?.toIntOrNull() ?: 8080,
                databaseUrl = required("GATEMASTER_DATABASE_URL"),
                databaseUser = env("GATEMASTER_DATABASE_USER"),
                databasePassword = env("GATEMASTER_DATABASE_PASSWORD"),
                jwtSecret = secret,
                jwtIssuer = env("GATEMASTER_JWT_ISSUER") ?: "gatemaster",
                jwtAudience = env("GATEMASTER_JWT_AUDIENCE") ?: "gatemaster-app",
                // Short, because an access token cannot be revoked: the window
                // in which a stolen one is useful is exactly this long.
                accessTokenLifetime = Duration.ofMinutes(
                    env("GATEMASTER_ACCESS_TOKEN_MINUTES")?.toLongOrNull() ?: 15,
                ),
                // Long, because it can be revoked, and because asking a student
                // to sign in every fortnight is how an app gets uninstalled.
                refreshTokenLifetime = Duration.ofDays(
                    env("GATEMASTER_REFRESH_TOKEN_DAYS")?.toLongOrNull() ?: 60,
                ),
                bcryptCost = cost,
                allowedOrigins = env("GATEMASTER_ALLOWED_ORIGINS")
                    ?.split(',')
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    .orEmpty(),
            )
        }
    }
}
