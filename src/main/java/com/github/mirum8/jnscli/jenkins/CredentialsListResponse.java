package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record CredentialsListResponse(
    List<Credential> credentials
) {
}
