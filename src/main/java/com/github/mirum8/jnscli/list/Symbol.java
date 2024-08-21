package com.github.mirum8.jnscli.list;

sealed interface Symbol permits Symbol.Double, Symbol.Single {
    String value();

    record Double(String value) implements Symbol {
    }

    record Single(String value) implements Symbol {
    }

}
