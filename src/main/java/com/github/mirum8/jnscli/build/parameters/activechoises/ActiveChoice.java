package com.github.mirum8.jnscli.build.parameters.activechoises;

import java.util.List;

public record ActiveChoice(
    String uid,
    List<String> referencedParameter,
    boolean legacy,
    String crumbs
) {
    public static ActiveChoice NOT_FOUND = new ActiveChoice(null, null, false, null);
}
