<div align="center">
    <img src="icon.png" width=200 height=200>
    <h1>JnsCLI</h1>
</div>
JnsCLI is a command-line interface for Jenkins, the popular CI/CD automation server. This tool allows you to
interact with your Jenkins server directly from the command line, making it easier to manage jobs, builds, and server
configurations without the need for the web interface. It also supports AI-powered error analysis to help you quickly identify and fix build errors.

## Table of Contents

- [Quick Start (Installation from Binary)](#quick-start-installation-from-binary)
- [Manual Compilation](#manual-compilation)
- [Features](#features)
- [Usage](#usage)
- [Commands](#commands)
    - [List Jenkins Jobs](#list-jenkins-jobs)
    - [Run a Job](#run-a-job)
    - [Abort](#abort)
    - [Connect](#connect)
    - [Alias](#alias)
    - [Info](#info)
  - [Error](#error)
  - [AI Commands](#ai-commands)
- [Contributing](#contributing)
- [License](#license)

## Quick Start (Installation from Binary)

Download the binary for your platform:

```shell
# for x86-64
wget -O jns-0.1.0-amd64 https://github.com/mirum8/jnscli/releases/download/v0.1.0/jns-0.1.0-amd64 \
&& chmod +x jns-0.1.0-amd64 && mkdir -p $HOME/.local/bin/ \
&& mv jns-0.1.0-amd64 $HOME/.local/bin/jns

# for ARM (Apple M1)
wget -O jns-0.1.0-arm64 https://github.com/mirum8/jnscli/releases/download/v0.1.0/jns-0.1.0-arm64 \
&& chmod +x jns-0.1.0-arm64 && mkdir -p $HOME/.local/bin/ \
&& mv jns-0.1.0-arm64 $HOME/.local/bin/jns
```

Add the binary to your PATH:

```shell
# for bash
echo "export PATH=\$PATH:$HOME/.local/bin/" >> $HOME/.bashrc && source $HOME/.bashrc
# for zsh
echo "export PATH=\$PATH:$HOME/.local/bin/" >> $HOME/.zshrc && source $HOME/.zshrc
```

Connect to your Jenkins server. You will be prompted to enter the server URL, username, and token:

```shell
jns connect
```

List Jenkins jobs:

```shell
jns list
```

Run a job:

```shell
jns build <jobName>
```

## Manual Compilation

### Prerequisites

- GraalVM JDK 21

### Steps

```shell
git clone https://github.com/mirum8/jnscli.git \
&& cd jnscli \
&& ./mvnw clean native:compile -Pnative \
&& mkdir -p $HOME/.local/bin/ \
&& mv target/jns $HOME/.local/bin/jns
```

Add the binary to your PATH:

```shell
# for bash
echo "export PATH=\$PATH:$HOME/.local/bin/" >> $HOME/.bashrc && source $HOME/.bashrc
# for zsh
echo "export PATH=\$PATH:$HOME/.local/bin/" >> $HOME/.zshrc && source $HOME/.zshrc
```

## Features

- Get a list of jobs
- Build a job with parameters
- Get information about a job and its builds
- Abort a running job
- Manage job aliases
- Retrieve and analyze build errors
- AI-powered error analysis

## Usage

Start the CLI application and use the available commands to interact with your Jenkins server.

## Commands

### List Jenkins Jobs

List all jobs on the Jenkins server:

```shell
jns list
```

![List jobs](casts/list.gif)

List jobs in a specific folder:

```shell
jns list <folder>
```

### Run a Job

Run a job on the Jenkins server:

```shell
jns build <jobId|jobName> [-p, --params <key=value>...] [-q, --quiet] [-l, --log] [--ai]
```

![Build job](casts/build.gif)

You can also use an ID number (prefixed by '%') from the 'list' output to start a job:

![Build job by ID](casts/buildById.gif)

If the job already started, you can abort the previous build and start a new one (or cancel the new build):

![Abort and build](casts/abortAndBuild.gif)

Options:

- `-q, --quiet`: Run the job in quiet mode, suppressing the progress bar.
- `-l, --log`: Display the build log during running job.
- `-p, --params <key=value>`: Specify build parameters. If you don't specify required parameters, you will be prompted
  to enter them. To pass multiple parameters, use this flag multiple times. For example:

  ```shell
  jns build <jobId> \
    -p key1=value1  \
    -p key2=value2
  ```
- `--ai`: Analyze errors using AI if the build fails.

### Abort

Abort a running job:

```shell
jns abort <jobId> [--b, --buildNumber]
```

### Connect

Connect to the Jenkins server:

```shell
jns connect
```

### Alias

Manage job aliases:

```shell
jns alias add <aliasName> <jobIdOrUrl> # Add an alias
jns alias rm <aliasName> # Remove an alias
jns alias ls # List all aliases
```

### Info

Get information about a job:

```shell
jns info <jobId> [options]
```

Options:

- `-b, --buildNumber`: Specify build number
- `-s, --includeSuccess`: Include successful builds
- `-f, --includeFailed`: Include failed builds
- `-r, --includeRunning`: Include running builds
- `-l, --limit`: Limit the number of builds (default: 3)
- `-m, --my-builds`: Show builds run by the current user

### Error

Get error information for the last or a specific build:

```shell
jns error <jobId> [Options]
```

Options:

- `-b, --buildNumber`: Specify build number
- `-m, --myBuild`: Show error for the last build run by the current user
- `--ai`: Analyze errors with AI

If parameters are not specified, the command will return the error information for the last failed build within the last
5 builds.

### AI Commands

AI-powered error analysis is available for Jenkins builds. To use this feature, you need to configure the AI settings (
Ollama and OpenAI are supported as AI services):
```shell
jns ai configure
```

After configuring the AI settings, you can test the AI service availability:

```shell
jns ai test
```

Now you can use `--ai` parameter in the 'build' and 'error' commands to analyze build errors with AI.
![Analyze error with AI](casts/getErrorWithAI.gif)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [MIT License](LICENSE).
