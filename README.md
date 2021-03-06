# Overview

Projux is a set of BASH scripts that provide command line support for
managing one or more project working directories and related settings.
Projects can be managed both locally and remotely (via TMUX).

Built-in support is provided for connecting to remote workspaces, switching
between workspaces (automatically setting and clearing project environment
variables), and managing sessions. A framework is also provided for
implementing project specific functions for formatting, linting, building,
testing, etc based on a standard set of commands common to all projects.


# Installation

Installation is straightforward:

1. Clone the repository to a local machine (e.g. Mac laptop, etc):

        cd ~
        git clone git://github.com/mdreves/projux.git .projux

2. Add default project settings to `~/.bashrc` and source `.projuxrc`

        if [ -f .projux/.projuxrc ]; then
          # default settings
          ...

          . .projux/.projuxrc
        fi

    Note that the default settings must be provided before the `.projuxrc`
    file is sourced. See `.projux/.sample-bashrc` for an example setup.

3. Add per project settings to `~/.projects`

    See `.projux/.sample-projects` for an example setup.


4. Setup remote hosts (if needed)

    This can either be done using `projux sync` or manually.  In either case,
    make sure to set either the `DEFAULT_PROJECT_HOST` var in `~/.bashrc` (if
    hosting all projects on a single server) or per project `PROJECT_HOST` vars
    in `~/.projects` (if different projects have different servers).

    a) Manually

    To setup manually, copy the `~/.projux` dir and `~/.projects` file to the
    remote host(s) and setup the remote `~/.bashrc` file as per step 2 and 4.

    b) Using sync

    If all the projects are hosted on a single development server, then in
    addition to the `DEFAULT_PROJECT_HOST` variable set in `~/.bashrc`, add
    a `DEFAULT_PROJECT_SYNC_LIST` variable pointing to a file that includes
    at least the `.projux` dir and the `.projects` file. If you are like me
    and have a shared `~/.bashrc` file used between a Mac laptop and a linux
    desktop server, then you can also add `.bashrc`. If not, you will need to
    manually setup the remote `.bashrc` as per step 2 and 4. If different
    projects are hosted on different servers, then you will need to setup
    project specific `PROJECT_SYNC_LIST` variables within the '.projects'
    file (similar to the `PROJECT_HOST` variables). There are examples for
    settings in `.projux/.sample-bashrc`, `.projux/.sample-projects`, and
    `.projux/.sample-projectsync`.

    Now, synchronize the local and remote host(s):

    If using `DEFAULT_PROJECT_HOST` and `DEFAULT_PROJECT_SYNC_LIST` settings:

        $ projux sync

    If separate `PROJECT_HOST` and `PROJECT_SYNC_LIST` settings:

        $ projux sync <project_name>


# Example

The best way to understand how projux works is to go through a detailed
example. Let's assume we have a two projects called `foo` and `bar`. Let's
further assume that `foo` is a Scala based project while `bar` is a mix of
C++ and Go. We will also assume that all our projects are hosted on a shared
remote server called `devhost` that is used for development. The first thing
we need to do is setup `~/.bashrc` to work with our remote server and our
development languages.  Here is an example of the settings we might add to
`~/.bashrc`:

        # devhost is our shared development server
        export DEFAULT_PROJECT_HOST=devhost

        # Setup TMUX windows for vim (bash), building (bash), and a repl (scala)
        export DEFAULT_PROJECT_WINDOWS="\
          vim::bash \        # vim window runs in bash shell
          build::bash \      # build window runs in a bash shell
          repl::scala"       # repl window runs scala interpreter

        # By default don't include any files in dirs with names tmp or backup
        export DEFAULT_PROJECT_EXCLUDE_FILTER="*tmp/*:*backup/*"

        # Format implementations for C++ and Go files
        export DEFAULT_PROJECT_FORMAT_CMDS="\
          c:cc:cpp:h::clang-format -i <flags> <targets> \
          go::goimports -w <flags> <targets>"

        # Lint implementation for Go files (using both govet and golint)
        export DEFAULT_PROJECT_LINT_CMDS="\
          go::golint <flags> <targets>; govet <flags> <targets>"

        # Build implementation for Scala, C++, and Go files
        export DEFAULT_PROJECT_BUILD_CMDS="\
          scala::scalac <flags> <targets> \
          cc::bazel build <args> \
          go::bazel build <args>"

We could also add support for test, coverage, clean, package, etc, but we
will keep the example simple for now.

Now we need to add our project specific settings to `~/.projects`. Environment
variables and aliases are associated with a given project based on their
position within the file.  All variables and aliases from the start of a
`PROJECT_NAME` entry up to the next `PROJECT_NAME` entry will be associated
with the same project. These variables/alaises are set whenever the
`projux <project_name>` command is used.

        # foo project
        PROJECT_NAME=foo
        PROJECT_INCLUDE_FILTER=*.scala
        PROJECT_DIR=${HOME}/Workspace/${PROJECT_NAME}
        PROJECT_SRC_DIR=${PROJECT_DIR}/src
        PROJECT_TEST_DIR=${PROJECT_DIR}/test
        PROJECT_TARGET_DIR=${PROJECT_DIR}/target
        PROJECT_BIN_DIR=${PROJECT_TARGET_DIR}/bin
        PROJECT_PKGS="com/xxx/foo com/xxx/common"
        PROJECT_RUN_CMDS="\
            client::client -log_dir=log -server=localhost:4321 \
            server::server -port=4321"
        alias start="startup_script"
        alias stop="stop_script"

        # bar project
        PROJECT_NAME=bar
        PROJECT_INCLUDE_FILTER=*.{cc,h}:*.go
        PROJECT_DIR=${HOME}/Workspace/${PROJECT_NAME}
        PROJECT_SRC_DIR=${PROJECT_DIR}
        PROJECT_TEST_DIR=${PROJECT_SRC_DIR}
        PROJECT_TEST_SUFFIXES=cc:go::_test
        PROJECT_TARGET_DIR=${PROJECT_DIR}/bazel-out
        PROJECT_BIN_DIR=${PROJECT_TARGET_DIR}/bazel-bin/${PROJECT_NAME}
        PROJECT_GEN_DIR=${PROJECT_DIR}/bazel-genfiles
        PROJECT_PKGS="xxx/bar"
        PROJECT_RUN_CMDS="bar -echo_flag="hello"

At this point we will assume that the projects have been syncd between the
local and remove hosts using `projux sync`, etc as described in the
installation steps. We can now attach to a project in a remote workspace using:

        $ projux attach foo

The first time this command is run from the local host it will create all the
TMUX windows listed in `DEFAULT_PROJECT_WINDOWS` and will then attach to the
first window listed (`vim` in our example). Subsequent calls will then reattach
wherever you left off.

We could attach another terminal window by issuing the same command in a
different terminal, but doing this will cause all our actions in one window to
be mirrored in the other (including switching windows). This may be useful
in some cases (e.g. mirroring a session so one person can watch what another
person is doing), but in our case we would like to attach a second terminal to
a different window. Let's assume we are running on Mac and are using iTerm2 to
create a split view of two terminal windows side by side. In the first terminal
we would like to run vim under project `foo` and in the second terminal we would
like to run builds (also under project `foo`). To do this, we need to use TMUX
grouped sessions. The setup is a bit complicated, but projux let's us do this
by just adding a number (2 in this case) to the end of the command to have it
work in another session group. In the second terminal we will run:

        $ projux attach foo 2

This will load the same project environment variables as the first terminal,
but instead of attaching to the first window by default, it will attach to the
second TMUX window (`build` in our example). These terminals can now operate
independently. Using the `win` subcommand (or TMUX itself) you can switch the
second terminal to the first TMUX window (in which case you will get mirroring
between the terminals), but switching out of that window to another will only
effect the second terminal window and not the first (e.g. TMUX window switches
are now per terminal). If you looked at the tmux session itself, you would see
that it contains '_2' in the name (e.g.  foo_2). I recommend associating a
number with a terminal and operating on all projects within that terminal using
the same number.

Now lets switch projects. To change the first terminal window to load project
`bar` use the following command:

        $ projux attach bar

Notice that we used attach even though we are already attached to the remote
server, this is to keep things consistent and simple, all you need to remember
is to point a terminal to a projects sessions (new or already existing) just use
attach. We can switch the second terminal window to bar as well by using:

        $ projux attach bar 2

And you can go back and forth.

        $ projux attach foo

Once you are working with a given project, all the environment variables and
aliases will be set to that project. Each time you switch, you clear the
previous and load the new. You can read more in the docs below, but there are
various subcommands supported by project such as listing existing projects,
killing project sessions, etc.

In addition to project management related commands, projux provides support
for common development related operations. For example, to format all the files
(that have formatting implementations) use the following:

        $ projux format

Based on our settings, this command would only work in the `bar` project because
a format implementation was not provided for `foo`. A full list of commands is
provided in the documentation below, but it covers basic things such as lint,
build, test, etc. It also covers commands for running a built executable:

        $ projux run                     # runs 'client -log_dir=log -port=4321'
        $ projux run client -log_dir=/t  # runs 'client -log_dir=/t -port=4321'
        $ projux run server              # runs 'server -port=4321'

When you need to disconnect from the development server, just issue the
following command:

        $ projux detach

When you call attach next time it will pick back right were you left off.


# Conventions

In order to try to be consistent, many projux subcommands follow a set of
conventions.

## Keywords

By convention, keyword args are prefixed with `:` (e.g. `geterrors :build`,
`cd :src`, etc).

## Recursive Targets

The characters `...` following a directory target means the directory and all
sub-directores recursively. For example, `build a/...` means build the
`a/` directory and all its sub-directories.

## Common Flags

Many commands accept a `-p <project>[:<win>]` flag to specify the project and
optional win to run the command against. If used, this must come before other
cmd args. When used the current project will not be changed, instead the command
will be sent over TMUX to the default (first window) associated with the
project.


# Subcommands

## Project Settings Subcommands

    $ projux

      Show current project.

    $ projux ls

      Display list of known projects (from ~/.projects).

    $ projux settings [<project>]

      Show project env var settings.

      Examples:
        projux settings         :  Show current project settings
        projux settings foo     :  Show project foo's env var settings

    $ projux bin [<cmd>]

      Display the full path to a binary associated with the project. The
      binaries built by the projects are assumed to be stored in
      $PROJECT_TARGET_DIR. The $PROJECT_RUN_CMDS specify the name used
      within projux to refer to the binary.

      Examples:
        projux bin              :  Full path to 1st entry in $PROJECT_RUN_CMDS
        projux bin client       :  Full path to client:: from $PROJECT_RUN_CMDS

    $ projux flags [<cmd>]

      Display the full path and flags for a binary associated with the project.
      The binaries built by the projects are assumed to be stored in
      $PROJECT_TARGET_DIR. The $PROJECT_RUN_CMDS specify the name used
      within projux to refer to the binary and its flags.

      Examples:
        projux flags            :  Display 1st entry in $PROJECT_RUN_CMDS
        projux flags client     :  Diplay client:: from $PROJECT_RUN_CMDS


    $ projux var {get|set|update|clear} {:test [+/-]<targets>|:run [+/-]<flags>}

      Set/get/upate/clear variables related to $PROJECT_TEST_TARGETS and
      $PROJECT_RUN_CMDS.

      Examples for $PROJECT_TEST_TARGETS:
        PROJECT_TEST_TARGETS="a_test b_test"
        projux var get :test                  # echo $PROJECT_TEST_TARGETS
        projux var set :test c_test           # PROJECT_TEST_TARGETS=c_test
        projux var update :test +d -b_test    # PROJECT_TEST_TARGEST=a_test d
        projux var clear :test                # PROJECT_TEST_TARGETS=""

      Examples for $PROJECT_RUN_CMDS:
        PROJECT_RUN_CMDS="cl::client -f=a srv::server -p=6"
        projux var get :run                    # echo $PROJECT_RUN_CMDS
        projux var get :run cl                 # echo "client -f=a"
        projux var get :run cl -f=+b           # echo "client -f=a,b"
        projux var update :run cl::cl -g=+z,-y # Update cl:: to cl::client -g=x,z
        projux var set :run xxx:prog -a        # PROJECT_RUN_CMDS=xxx::prog -a
        projux var clear :run                  # PROJECT_RUN_CMDS=""


## Remote Project Management Subcommands

    $ projux sync [<project>]

      Sync local files with remote project host based on $PROJECT_SYNC_LIST and
      $PROJECT_SYNC_DESTS.

      Examples:
        projux sync             :  Sync current project
        projux sync foo         :  Sync local files with foo's project host

    $ projux ssh [<project>]

      SSH to remote project host (based on $PROJECT_HOST). If no project name is
      provided, then SSH to $DEFAULT_PROJECT_HOST. The -Y and -t flags will be
      passed to the SSH command.

      Examples:
        projux ssh              :  SSH to default remote host
        projux ssh foo          :  SSH to remote host for foo project

    $ projux sftp [<project>]

      SFTP to remote project host (based on $PROJECT_HOST). If no project name is
      provided, then SFTP to $DEFAULT_PROJECT_HOST.

      Examples:
        projux sftp             :  SFTP to default remote host
        projux sftp foo         :  SFTP to remote host for foo project


## Project Session Management Subcommands

    $ projux sessions

      Show all attached project sessions.

    $ projux attach <project> [<session>]

      If attached switch to <project>, if detached create new TMUX session. In
      either case, initialize project vars.

      Examples:
        projux attach foo       :  Attach/switch to foo project
        projux attach bar 2     :  Attach/switch to 2nd session for project bar
        projux attach foo 3     :  Attach/switch to 3rd session for project foo

    $ projux detach [<project>] [<session>]

      Detach from TMUX session.

      Examples:
        projux detach           :  Detach current client from cur project
        projux detach foo       :  Detach all clients attached to foo project
        projux detach foo 2     :  Detach clients attached to 2nd foo session

    $ projux kill [<project>] [<session>]

      Kill TMUX session.

      Examples:
        projux kill             :  Kill all of current project's TMUX sessions
        projux kill foo         :  Kill all of foo project's TMUX sessions
        projux kill foo 1       :  Kill main session of project foo
        projux kill foo 2       :  Kill 2nd session of project foo

    $ projux clear

      Clear env var settings for current project.

      Examples:
        projux clear

    $ projux reload

      Reload env var settings for current project.

      Examples:
        projux reload

    $ projux win [<win>|:default|:delete <win>|:new <win>]

      Project window management. If no args are passed, then list the current
      windows. If a window name is passed as an arguement, then switch to that
      window. If :default is passed print the default window (for session 1, the
      default is the first window, for session 2, it is the second, etc). If
      :new <win> or :delete <win> is used then create a new window or delete an
      existing window (by name).

      Examples:
        projux win              :  Display current windows
        projux win vim          :  Switch to vim window
        projux win :default     :  Print default window for this session
        projux win :new xx      :  Create a new window named 'xx'
        projux win :delete xx   :  Delete window named 'xx'

    $ projux pane [<pane>|:delete <pane>|:split|:vsplit]

      Project window pane management. If no args are passed, then list the
      current window panes. If a pane number (either current number or a global
      ID) is passed as an arguement, then switch to that pane. If :delete <pane>
      is used then delete an existing pane (by number or global ID). If :split
      or :vsplit is used then create a new pane by splitting horizontally or
      vertiacally.

      Examples:
        projux pane             :  Display current panes
        projux pane 2           :  Switch to pane 2
        projux pane %5          :  Switch to pane with global ID 5
        projux pane :split      :  Create pane by horizontal split
        projux pane :vsplit     :  Create pane by vertical split
        projux pane :delete 3   :  Delete pane 3


## Project File and Directory Management Subcommands

    $ projux cd {:bin|:gen [#]|:project|:src [#]|:test [#]}

      Change to project related directories.

      Examples:
        projux cd :bin          :  cd $PROJECT_BIN_DIR
        projux cd :src          :  cd $PROJECT_SRC_DIR/$PROJECT_PKGS[0]
        projux cd :src 2        :  cd $PROJECT_SRC_DIR/$PROJECT_PKGS[1]
        projux cd :test 2       :  cd $PROJECT_TEST_DIR/$PROJECT_PKGS[1]
        projux cd :project      :  cd $PROJECT_DIR
        projux cd :gen 3        :  cd $PROJECT_GEN_DIR/$PROJECT_PKGS[2]

    $ projux pushd {:bin|:gen [#]|:project|:src [#]|:test [#]}

      Push project related directories.

      Examples:
        projux pushd :bin       :  pushd $PROJECT_BIN_DIR
        projux pushd :src       :  pushd $PROJECT_SRC_DIR/$PROJECT_PKGS[0]
        projux pushd :src 2     :  pushd $PROJECT_SRC_DIR/$PROJECT_PKGS[1]
        projux pushd :test 2    :  pushd $PROJECT_TEST_DIR/$PROJECT_PKGS[1]
        projux pushd :project   :  pushd $PROJECT_DIR
        projux pushd :gen 3     :  pushd $PROJECT_GEN_DIR/$PROJECT_PKGS[2]


## Project Tools Subcommands

    $ projux format [-p <project>] <targets>

      Runs project specific format based on $PROJECT_FORMAT_CMDS.

      Examples:
        projux format           :  Format recursively from current dir
        projux format ...       :  As above
        projux format foo.cc    :  Format specific file
        projux format :project  :  Format all project files
        projux format -p foo    :  Format recursively in project foo

    $ projux lint [-p <project>] <targets>

      Runs project specific lint based on $PROJECT_LINT_CMDS.

      Examples:
        projux lint             :  Lint recursively from current dir
        projux lint ...         :  As above
        projux lint foo.py      :  Lint specific file.
        projux lint :project    :  Lint all project files
        projux lint -p foo      :  Lint recursively in project foo

    $ projux build [-p <project>] <targets>

      Runs project specific build based on $PROJECT_BUILD_CMDS.

      Examples:
        projux build Foo.scala  :  Build specific file.
        projux build            :  Build recursively from current dir
        projux build ...        :  As above
        projux build -p foo     :  Build recursively in project foo

    $ projux test [-p <project>] <targets>

      Runs project specific test based on $PROJECT_TEST_CMDS.

      Examples:
        projux test             :  Test recursively from current dir
        projux test ...         :  As above
        projux test my_test     :  Test specific file.
        projux test -p foo      :  Test recursively in project foo

    $ projux coverage [-p <project>] <targets>

      Runs project specific coverage based on $PROJECT_COVERAGE_CMDS.

      Examples:
        projux coverage         :  Coverage recursively from current dir
        projux coverage ...     :  As above
        projux coverage x_test  :  Coverage for a specific file.
        projux coverage -p foo  :  Coverage recursively in project foo

    $ projux run [-p <project>] [<label>] [<flags>]

      Runs project specific program based on $PROJECT_RUN_CMDS setting.
      Multiple executables can be tagged with 'key::' labels to distinguish
      them. The key label is then provided to the run command to select the
      appropriate program. If no label is provided then the first entry is used.

      Examples:
        PROJECT_RUN_CMDS="client::cl -f=a srv::server -port=6"
        projux run              :  $PROJECT_RUN_DIR/cl -f=a
        projux run cl           :  As above
        projux run cl -f=-a,+b  :  $PROJECT_RUN_DIR/cl -f=b
        projux run cl -f=c      :  $PROJECT_RUN_DIR/cl -f=c
        projux run -shared srv  :  $PROJECT_SHARE_DIR/srv -port=6

    $ projux clean [-p <project>]

      Cleans project temp files based on $PROJECT_CLEAN_CMDS.

      Examples:
        projux clean            :  Clean project files
        projux clean -p foo     :  Clean project foo

    $ projux package [-p <project>]

      Package project output files based on $PROJECT_PACKAGE_CMDS.

      Examples:
        projux package          :  Package project output
        projux package -p foo   :  Package project foo

    $ projux deploy [-p <project>]

      Deploy project based on $PROJECT_DEPLOY_CMDS.

      Examples:
        projux deploy           :  Deploy project
        projux deploy -p foo    :  Deploy project foo

    $ projux gendocs [-p <project>]

      Generate docs for project based on $PROJECT_GENDOCS_CMDS.

      Examples:
        projux gendocs          :  Generate docs for project
        projux gendocs -p foo   :  Generate docs for project foo

    $ projux search [-p <project>]

      Runs project specific search based on $PROJECT_SEARCH_CMDS.

      Examples:
        projux search xxx       :  Search for xxx in project code base
        projux search -p p xxx  :  Search for xxx in project p code base

    $ projux sanity [-p <project>]

      Runs project specific sanity. The default implementation runs format,
      lint, build, test, and coverage. A project specific implementation can
      be defined by setting $PROJECT_SANITY_FN.

      Examples:
        projux sanity           :  Run sanity for current project
        projux sanity -p foo    :  Run sanity on project foo

    $ projux share [-p <project>] <label>

      Copies named program file from $PROJECT_BIN_DIR to $PROJECT_SHARE_DIR.

      Examples:
        projx share cl          :  cp $PROJECT_BIN_DIR/client $PROJECT_SHARED_DIR

    $ projux backup [<project>]

      Backs up changed project files (not in .git repo) to $PROJECT_BACKUP_DIR.

      Examples:
        projux backup           :  Backup uncomitted .git files
        projux backup foo       :  Backup uncomitted project foo .git files

    $ projux vimserver

      Projux supports starting up VIM in server mode using the `projux vim`
      command.  When used, vim will be started with a server name that matches
      the current `$PROJECT_NAME` environment variable. Unfortunately, server
      mode in VIM is not like EMACS, you can't open multiple windows and share
      buffers. It is mainly used to send information from one TMUX window to
      another window running the VIM server (e.g. sending the quickfix output
      from a build in one window to VIM in running in another window in order
      to display the errors in Syntastic).

      Examples:
        projux vim              :  vim -servername $PROJECT_NAME -c ":Open :s"

    $ projux sbt

      Projux provides a plugin for use with Scala's SBT (Simple Build Tool).
      Although Scala must be installed to use sbt, the projux plugin is intended
      to be used by non-Scala projects. To use the plugin the following must be
      installed:

      1. Scala (2.10 or later)
      2. SBT (0.13)

      Once Scala is installed, the plugin itself does not need to be installed;
      instead, just run the `projux sbt` command to launch sbt with the plugin.
      Once launched, many of the same commands used from the command line
      (format, lint, build, ...) can be run from the SBT command line. The
      difference is that in SBT you don't need to specify the targets. All files
      associated with the project (based on the environment variable settings at
      the time of launching `projux sbt`) are used as targets. With each
      subsequent call to a given command, only those files that changed since
      the last run are used as targets.  You can also make use of SBT's watcher
      to automatically run a command whenever a project file is changed.

      Examples:
        projux sbt              :  Run sbt from ~/.projux/psbt
        >format                 :  Format files
        >~lint                  :  Watch for changes and auto-lint
        >build                  :  Build
        >~test                  :  Watch for changes and auto-test


## Project Output Subcommands

    $ projux errors {:build|:lint|:test|:coverage|<file>}

      Returns VIM quickfix friendly errors for last build/lint/test/coverage.
      Errors should be echoed in the form: <filename>:<line>:<col>:<message>.
      If called with ':build', ':lint', ':test', or ':coverage' then errors
      from the last build/lint/test/coverage should be returned. Otherwise
      the errors for the passed in target should be returned.

      Examples:
        projux errors :build    : Errors from last build
        projux errors :lint     : Errors from last lint
        projux errors :test     : Errors from last test
        projux errors :coverage : Errors from last coverage
        projux errors foo.py    : Errors for file foo

    $ projux url {:build|:lint|:test|:coverage|:bug|:review}

      Prints url summarizing last build, lint, etc (where applicable).

      Examples:
        projux url :build       : URL for last build
        projux url :lint        : URL for last lint
        projux url :test        : URL for last test
        projux url :coverage    : URL for last coverage
        projux url :bug         : URL for bug ID assigned to project
        projux url :review      : URL for current project code review

    $ projux goto {:build|:lint|:test|:coverage|:bug|:review}

      Opens url summarizing last build, lint, etc (where applicable).

      Examples:
        projux goto :build      : Open URL for last build
        projux goto :lint       : Open URL for last lint
        projux goto :test       : Open URL for last test
        projux goto :coverage   : Open URL for last coverage
        projux goto :bug        : Open URL for bug ID assigned to project
        projux goto :review     : Open URL for current project code review

    $ projux cat {:build|:lint|:test|:coverage}

      Print output of last build, lint, etc (where applicable).

      Examples:
        projux cat :build       : Print output of last build
        projux cat :lint        : Print output of last lint
        projux cat :test        : Print output of last test
        projux cat :coverage    : Print output of last coverage

    $ projux vi {:build|:lint|:test|:coverage}

      Vi output of last build, lint, etc (where applicable).

      Examples:
        projux vi :build        : Vi output of last build
        projux vi :lint         : Vi output of last lint
        projux vi :test         : Vi output of last test
        projux vi :coverage     : Vi output of last coverage


# Project Aliases

If PROJECT_ALIASES is set to to true, then an alias will be provided for all
projux commands by prefixing each subcommand with ':'. For example:

    :attach foo                 # projux attach foo
    :build :lint                # projux build :lint
    ...

In addition short cuts are provided for the most commonly used subcommands:

    :a                          # projux attach
    :b                          # projux build
    :d                          # projux deploy
    :f                          # projux format
    :l                          # projux lint
    :p                          # projux package
    :r                          # projux run
    :t                          # projux test
    :w                          # projux win


# Environment Variables

## Overview

Projux has special settings for some of the environment variables.

### Key/Value Maps

Some environment variables take a map of key/value settings. The settings are
specified using the following form:

        <key>::<value> <key>::<value> ...

The <key> and <value> settings depend on the type of environment variable.
There are three main types that use this format:

1 Project Windows

In this case the <key> is the window name and the <value> is the command to
use when launching the window. For example to launch bash in a 'vim' window
and scala in a 'repl' window:

        PROJECT_WINDOWS="\
            vim::bash; stty -ixon \   # tmux new-window -d vim 'bash; stty-ixon'
            repl::scala"              # tmux new-window -d vim 'scala'

2 Environment Variables with Commands Per File Type

In this case the <key> is a colon separated list of file extensions and the
<value> is the command(s) to use on files of those types. For example, for
the PROJECT_FORMAT_CMDS we could use the following to run clang-format on
files of type *.{cc,h} and goimports on files of type *.go, use:

        PROJECT_FORMAT_CMDS="\
            cc:h::clang-format -i <flags> <targets> \
            go::goimports -w <args>"

In this case the literal string <flags> and <targets> will be replaced by the
flags and expanded targets per file type. Any command line argument that starts
with a '-' is considered a flag, while all others are considered targets of the
command. If you just want the args as passed on the command line without
expanding the targets, the literal string <args> can be used.

Multiple commands can be run at once by separating them with ';'. For
example, to run javacheck on *.java and both golint and govet on *.go files:

        PROJECT_LINT_CMDS="\
            java::javacheck <flags> <targets> \
            go::golint <flags> <targets>; govet <flags> <targets>"

3 Environment Variables with Selectable Commands

In this case the <key> represents a name that can be used to select from
one or more commands. When the associated call is made the key is passed
and the commmand(s) associated with that key is run. If no key is provided
then the first command listed is used.

        PROJECT_RUN_CMDS="\
            client::client -server=localhost:1234 -data=a,b \
            server::server -port=1234"

        $ run client    # runs client -server=localhost:1234 -data=a,b
        $ run server    # runs server -port=1234
        $ run           # runs client -server=localhost:1234 -data=a,b

### Selectable Commands

As mentioned, selectable commands allow you to select from one or more
commands based on a key:: tag. However, you can also modify the flags that
are associated with the selected command.

        $run server -port=5678       # runs 'server -port=5678'
        $run client -data=+c         # runs 'server -port=1234 -data=a,b,c'
        $run client -data=-b,+e      # runs 'server -port=1234 -data=a,e'

### Defaults

An environment variable `XXX` can have a default called `DEFAULT_XXX`. The
`DEFAULT_XXX` value is used when a project does not set a specific `XXX` value.
`XXX` variables are set in `~/.projects` while `DEFAULT_XXX` are set in
`~/.bashrc`.

## General Project Settings

    PROJECT_NAME (required)
       Project name.

    PROJECT_HOST / DEFAULT_PROJECT_HOST
      Server project should be built/run/etc from (localhost, myserver, etc).

    PROJECT_WINDOWS / DEFAULT_PROJECT_WINDOWS
      Map of tmux windows. The keys are window names and the values are
      commands to use when the window is opened (see overview).
      (e.g. "bash::bash; stty -ixon repl::python").

    PROJECT_DIR (required)
      Main project directory

    PROJECT_SRC_DIR (required)
      Source directory for project (e.g. $PROJECT_DIR/src).

    PROJECT_TEST_DIR (required)
      Test directory for project (e.g. $PROJECT_DIR/test).

    PROJECT_TARGET_DIR (required)
      Directory for build/lint output (e.g. $PROJECT_DIR/bazel-out)

    PROJECT_BIN_DIR (required)
      Directory containing executable program(s) (e.g.
      $PROJECT_TARGET_DIR/bin). PROJECT_RUN_CMDS are relative to this
      directory.

    PROJECT_GEN_DIR (required if cdgen used)
      Directory generated code output to (e.g. $PROJECT_DIR/bazel-genfiles).

    PROJECT_PKGS (required)
      List of main packages used by project (e.g. "com/example1 com/example2").
      These are directories that should exist in the PROJECT_SRC_DIR and/or
      PROJECT_TEST_DIR. Default is all directories under source and test dirs.

    PROJECT_INCLUDE_FILTER / DEFAULT_PROJECT_INCLUDE_FILTER (required)
      Colon separated list of patterns for files that should be considered part
      of the project (for example, make:*.scala:*.java:*.go:*.{c,cc,h}). When
      format, lint, build, test, etc are run, these files are searched for in
      the PROJECT_SRC_DIR and/or PROJECT_TEST_DIR directories. Multiple file
      types may be specified even though only some operations are supported on
      some files (e.g. lint may only be supported for *.java and although
      *.scala may be included in the filter, lint will only be run for java).
      It is best to group files together that go together (e.g. *.{c,cc,h} vs
      *.c:*.cc:*.h). This setting is required.

    PROJECT_EXCLUDE_FILTER / DEFAULT_PROJECT_EXCLUDE_FILTER
      Colon separated list of patterns for files that should be NOT be
      considered part of the project (for example, Foo.scala:Bar.java). If these
      files exist in the PROJECT_SRC_DIR or PROJECT_TEST_DIR they will be
      ignored.

    PROJECT_BUG
      Current bug ID for work being done in project.

## Setttings for Selectable Commands

    PROJECT_RUN_CMDS / DEFAULT_PROJECT_RUN_CMDS
      Command(s) to use when run called. A single command can be specified or a
      map of commands can used by labelling them as <key>::<cmd>
      (e.g. clien::client -server=localhost:1234 server::server -port=1234).

    PROJECT_CLEAN_CMDS / DEFAULT_PROJECT_CLEAN_CMDS
      Command(s) to use when clean called. A single command can be specified or
      a map of commands can used by labelling them as <key>::<cmd>
      (e.g. sbt clean).

    PROJECT_PACKAGE_CMDS / DEFAULT_PROJECT_PACKAGE_CMDS
      Command(s) to use when package called. A single command can be specified
      or a map of commands can used by labelling them as <key>::<cmd>
      (e.g. sbt package).

    PROJECT_DEPLOY_CMDS / DEFAULT_PROJECT_DEPLOY_CMDS
      Command(s) to use when deploy called. A single command can be specified or
      a map of commands can used by labelling them as <key>::<cmd>
      (e.g. frontend::ant deploy-fe backend::ant deploy-be).

    PROJECT_GENDOCS_CMDS / DEFAULT_PROJECT_GENDOCS_CMDS
      Command(s) to use when gendocs called. A single command can be specified
      or a map of commands can used by labelling them as <key>::<cmd>
      (e.g. scala::sbt doc go::godocs -http=:6060).

    PROJECT_SEARCH_CMDS / DEFAUT_PROJECT_SEARCH_CMDS
      Command(s) to use when search called. A single command can be specified
      or a map of commands can used by labelling them as <key>::<cmd>
      (e.g. default::mysearch -src=${PROJECT_SRC_DIR}).

## Settings for Commands Based on File Types

    PROJECT_FORMAT_CMDS / DEFAULT_PROJECT_FORMAT_CMDS
      Map of commands to run when formatting project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      The literal strings <flags> and <targets> (or <args>) can be used as
      placeholders for the flags and targets passed to projux format.
      (e.g. "cc:h::clang-format -i <flags> <targets>
      go::goimports <args>")

    PROJECT_LINT_CMDS / DEFAULT_PROJECT_LINT_CMDS
      Map of commands to run when linting project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      The literal strings <flags> and <targets> (or <args>) can be used as
      placeholders for the flags and targets passed to projux format.
      (e.g. "go::golint <flags> <targets>; govet <flags> <targets>
      cc:h::clint <flags> <targets>")

    PROJECT_BUILD_CMDS / DEFAULT_PROJECT_BUILD_CMDS
      Map of commands to run when compling project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      The literal strings <flags> and <targets> (or <args>) can be used as
      placeholders for the flags and targets passed to projux format.
      (e.g. "cc:h::make <flags> <targets> scala::scalac <flags> <targets>
      java::javac <flags> <targets>")

    PROJECT_TEST_CMDS / DEFAULT_PROJECT_TEST_CMDS
      Map of commands to run when testing project files. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd> ...
      The literal strings <flags> and <targets> (or <args>) can be used as
      placeholders for the flags and targets passed to projux format.
      (e.g. "scala::sbt test <flags> <targets>
       go::bazel test <args>")

    PROJECT_COVERAGE_CMDS / DEFAULT_PROJECT_COVERAGE_CMDS
      Map of commands to show code coverage of project tests. The map should
      have the form: <file_ext>:...<file_ext>::<cmd>;...;<cmd>,...
      The literal strings <flags> and <targets> (or <args>) can be used as
      placeholders for the flags and targets passed to projux format.
      (e.g. "rb::coverage <flags> <targets>")

    PROJECT_TEST_SUFFIXES / DEFAULT_PROJECT_TEST_SUFFIXES
      Map of suffixes used on test related files. This is used to distinguish
      source and test files when they reside in the same directory. The map
      should have the form: <file_ext>:...<file_ext>::<suffix> ...
      (e.g. "cc::_test")

    PROJECT_TEST_TARGETS
      Override for test/coverate include filters. When set
      PROJECT_INCLUDE_FILTER is set to PROJECT_TEST_TARGETS before test or
      coverage is called. When not set, then PROJECT_INCLUDE_FILTER is used as
      set. This is used with the settest, etc functions to allow configuring
      a small subset of test to run multiple times during development.

## Custom Function Implementations

    PROJECT_FORMAT_FN / DEFAULT_PROJECT_FORMAT_FN
      Name of custom 'format' function implementation.

    PROJECT_LINT_FN / DEFAULT_PROJECT_LINT_FN
      Name of custom 'lint' function implementation.

    PROJECT_BUILD_FN / DEFAULT_PROJECT_BUILD_FN
      Name of custom 'build' function implementation.

    PROJECT_TEST_FN / DEFAULT_PROJECT_TEST_FN
      Name of custom 'test' function implementation.

    PROJECT_COVERAGE_FN / DEFAULT_PROJECT_COVERAGE_FN
      Name of custom 'coverage' function implementation.

    PROJECT_GETERRORS_FN / DEFAULT_PROJECT_GETERRORS_FN
      Name of custom 'geterrors' function implementation.
      Errors should be echoed as lines of: <file>:<line>:<col>:<message>.
      See geterrors function for more information.

    PROJECT_GETURL_FN / DEFAULT_PROJECT_GETURL_FN
      Name of custom 'geturl' function implementation.

    PROJECT_SANITY_FN / DEFAULT_PROJECT_SANITY_FN
      Name of custom 'santity' function implementation.


## Sharing/Syncing/Backup/...

    PROJECT_SHARE_DIR / DEFAULT_PROJECT_SHARE_DIR
      This variable is set to the directory to use for copying executables
      for sharing publically (e.g. ~/Public).

    PROJECT_BACKUP_DIR / DEFAULT_PROJECT_BACKUP_DIR
      This variable is set to the directory to use for project backups when
      the 'project backup' command is used.

    PROJECT_SYNC_LIST / DEFAULT_PROJECT_SYNC_LIST
      This variable is set to the name of a file containing a list of files
      that should be synchronized from the local host to the project host
      when using 'project sync' command. Listed files are specified relative
      to the local home directory. The DEFAULT_PROJECT_SYNC_LIST is by default
      set to ~/.projectsync which contains ~/.projects.

    PROJECT_SYNC_DESTS / DEFAULT_PROJECT_SYNC_DESTS
      Comma separated list of directories to sync files to on remote host.

## Misc

    PROJUX_ALIASES
      Set to true to add projux related aliases (:build, etc). Default is true.

    PROXY_CMDS
      Set to true to proxy commands to remote host when running locally. If not
      set, you must explicity attach to remote TMUX session to run commands.
      Default is false.

    PROXY_NOTIFY_FN
      Function to call whenever tmux new, attach, or switch is called. The first
      parameter will be true if 'new' was used.

    VERBOSE
      Some commands take a VERBOSE environment variable. This is a level setting
      not on/off. Level 0 effectively means off which is different than most
      flags in bash where 0 is success). Many commands do not support verbose
      output yet, so don't expect much. Default is true.

    HEADING
      Whether to print a heading when VERBOSE is used. This is a true/false
      settings (e.g. DRY_RUN=true).

    DRY_RUN
      The dry run flag will print the command that will execute, but not
      actualy execute it. This is a true/false settings (e.g. DRY_RUN=true)
      Default is false.

# Implementing Custom Selectable Commands

Some projects may have their own selectable commands that fall outside of
the built-in run, search, package, etc. Projux provides a helper function to
implement these using the `__projux_select_cmd` function. For example, given the
following:

      function update() {
        # first param is environment var name, second is heading
        __projux_select_cmd "PROJECT_UPDATE_CMDS" "UPDATING" $*
      }

A project can now create a `PROJECT_UPDATE_CMDS` variable similar to
`PROJECT_RUN_CMDS` that allows this new `update` command to be called while
passing labels and flags to select the bash command that is ultimately invoked.

# License

Copyright 2012 - 2018 Mike Dreves

All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v1.0
which accompanies this distribution, and is available at:

    http://opensource.org/licenses/eclipse-1.0.php

By using this software in any fashion, you are agreeing to be bound
by the terms of this license. You must not remove this notice, or any
other, from this software. Unless required by applicable law or agreed
to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
either express or implied.
