package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.shell.ShellPrinter;

/**
 * Replaces {@link ShellPrinter} in MCP mode so accidental CLI-style writes never reach
 * stdout — stdout is reserved for JSON-RPC frames.
 */
class NoOpShellPrinter extends ShellPrinter {

    NoOpShellPrinter() {
        super(null);
    }

    @Override
    public void println() {
        // intentional no-op: stdout is owned by the MCP transport
    }

    @Override
    public void println(String message) {
        // intentional no-op: stdout is owned by the MCP transport
    }

    @Override
    public void print(String message) {
        // intentional no-op: stdout is owned by the MCP transport
    }
}
